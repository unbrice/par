/*
 * Copyright ©2011 Brice Arnould
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.vleu.par.gateway;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.jcip.annotations.ThreadSafe;
import net.vleu.par.gateway.datastore.TooManyConcurrentAccesses;
import net.vleu.par.gateway.models.DeviceId;
import net.vleu.par.gateway.models.DeviceId.InvalidDeviceIdSerialisation;
import net.vleu.par.gateway.models.Directive;
import net.vleu.par.gateway.models.GatewayRequest;
import net.vleu.par.gateway.models.UserId;
import net.vleu.par.protocolbuffer.Commands.DirectiveData;
import net.vleu.par.protocolbuffer.Devices.DeviceIdData;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayRequestData;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayRequestData.GetDeviceDirectivesData;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayRequestData.QueueDirectiveData;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayRequestData.RegisterDeviceData;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayResponseData;

@ThreadSafe
@SuppressWarnings("serial")
public final class ApiServlet extends HttpServlet {
    /**
     * Thrown when a request is descovedred to be invalid after the point where
     * it should have been checked by
     * {@link GatewayRequest#isValid(GatewayRequestData, ArrayList)}
     */
    public static class InvalidRequestPassedVerification extends Exception {

        /**
         * @see RuntimeException#RuntimeException(String message, Throwable
         *      cause)
         */
        private InvalidRequestPassedVerification(final String message,
                final Throwable cause) {
            super(message, cause);
        }

        /** @see RuntimeException#RuntimeException(Throwable) */
        private InvalidRequestPassedVerification(final Throwable cause) {
            super(cause);
        }
    }

    private static final Logger LOG = Logger.getLogger(ApiServlet.class
            .getName());

    /** Mximal size in bytes for the serialized protocol buffers we accept */
    private static final int MAX_COMMAND_SIZE = 1024;

    private static String joinStrings(final ArrayList<String> strings,
            final String separator) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (final String s : strings)
            stringBuilder.append(s);
        return stringBuilder.toString();
    }

    private static DeviceId parseOrThrowInvalidRequestPassedVerification(
            final DeviceIdData proto) throws InvalidRequestPassedVerification {
        try {
            return DeviceId.fromProtocolBuffer(proto);
        }
        catch (final InvalidDeviceIdSerialisation e) {
            throw new InvalidRequestPassedVerification(e);
        }
    }

    private static byte[] readAllBytes(final ServletInputStream stream)
            throws IOException {
        final byte[] res = new byte[MAX_COMMAND_SIZE];
        int position = 0;
        int lastRet;
        do {
            lastRet = stream.read(res, position, MAX_COMMAND_SIZE - position);
            position += lastRet;
        } while (lastRet > 0);
        if (lastRet == 0) {
            LOG.warning("Truncating a too large input");
            return null;
        }
        return res;
    }

    private final DeviceRegistrar deviceRegistrar;

    private final DeviceWaker deviceWaker;

    private final DirectiveStore directiveStore;

    private final ServletHelper servletHelper;

    public ApiServlet() {
        this(new DirectiveStore(), new DeviceRegistrar(), new DeviceWaker(),
                new ServletHelper());
    }

    /** For dependency-injection during tests */
    ApiServlet(final DirectiveStore directiveStore,
            final DeviceRegistrar deviceRegistrar,
            final DeviceWaker deviceWaker, final ServletHelper servletHelper) {
        this.directiveStore = directiveStore;
        this.deviceRegistrar = deviceRegistrar;
        this.deviceWaker = deviceWaker;
        this.servletHelper = servletHelper;
    }

    /** @inherit */
    @Override
    public void doPost(final HttpServletRequest req,
            final HttpServletResponse httpResp) throws IOException {
        final UserId userId = this.servletHelper.getCurrentUser();
        final GatewayRequest request;

        /* Checks that the user is authenticated */
        if (userId == null) {
            httpResp.sendError(HttpCodes.HTTP_FORBIDDEN_STATUS,
                    "Requests must be authenticated");
            LOG.finer("Unauthenticated request");
            return;
        }

        /* Checks that the request is well-formed */
        {
            final byte[] requestBytes;
            final GatewayRequestData requestPB;
            final ArrayList<String> errors = new ArrayList<String>(0);
            requestBytes = readAllBytes(req.getInputStream());
            if (requestBytes == null || requestBytes.length == 0) {
                final String msg =
                        "Protocol buffer rejected for its size (empty or too big)";
                LOG.fine(msg);
                httpResp.sendError(HttpCodes.HTTP_BAD_REQUEST_STATUS, msg);
                return;
            }
            requestPB = GatewayRequestData.parseFrom(requestBytes);
            if (!GatewayRequest.isValid(requestPB, errors)) {
                LOG.fine("Requests rejected: " + joinStrings(errors, " --- \n"));
                httpResp.sendError(HttpCodes.HTTP_BAD_REQUEST_STATUS,
                        joinStrings(errors, "<br />\n"));
                return;
            }
            request = new GatewayRequest(requestPB);
        }

        try {
            final GatewayResponseData.Builder responseBuilder =
                    GatewayResponseData.newBuilder();
            final GatewayResponseData responseProto;
            handleRequest(userId, request, responseBuilder);
            responseProto = responseBuilder.build();
            httpResp.setContentType("application/octet-stream");
            httpResp.setContentLength(responseProto.getSerializedSize());
            responseProto.writeTo(httpResp.getOutputStream());
            httpResp.flushBuffer();
            httpResp.getOutputStream().close();
        }
        catch (final InvalidRequestPassedVerification e) {
            httpResp.sendError(HttpCodes.HTTP_BAD_REQUEST_STATUS, e.toString());
            LOG.severe(e.toString());
            return;
        }
        catch (final TooManyConcurrentAccesses e) {
            httpResp.sendError(HttpCodes.HTTP_SERVICE_UNAVAILABLE_STATUS,
                    e.toString());
            LOG.severe(e.toString());
            return;
        }
    }

    private void handleRequest(final UserId userId, final GatewayRequest req,
            final GatewayResponseData.Builder resp)
            throws InvalidRequestPassedVerification, TooManyConcurrentAccesses {
        for (final QueueDirectiveData qd : req.getQueueDirectivesData())
            handleRequestPart(userId, qd, resp);
        for (final RegisterDeviceData rd : req.getRegisterDeviceData())
            handleRequestPart(userId, rd, resp);
        for (final GetDeviceDirectivesData rd : req
                .getGetDeviceDirectivesData())
            handleRequestPart(userId, rd, resp);
    }

    private void handleRequestPart(final UserId userId,
            final GetDeviceDirectivesData req,
            final GatewayResponseData.Builder resp)
            throws InvalidRequestPassedVerification, TooManyConcurrentAccesses {
        final DeviceId deviceId =
                parseOrThrowInvalidRequestPassedVerification(req.getDeviceId());
        final ArrayList<Directive> directives =
                this.directiveStore.fetchAndDelete(userId, deviceId);
        for (final Directive directive : directives)
            resp.addDirective(directive.asProtocolBuffer());
    }

    private void
            handleRequestPart(final UserId userId,
                    final QueueDirectiveData req,
                    final GatewayResponseData.Builder resp)
                    throws InvalidRequestPassedVerification,
                    TooManyConcurrentAccesses {
        final DirectiveData directiveData = req.getDirective();
        final Directive directive = new Directive(directiveData);
        final DeviceId deviceId =
                parseOrThrowInvalidRequestPassedVerification(req.getDeviceId());
        this.directiveStore.store(userId, deviceId, directive);
        this.deviceWaker.queueWake(userId, deviceId);
    }

    private void
            handleRequestPart(final UserId userId,
                    final RegisterDeviceData req,
                    final GatewayResponseData.Builder resp)
                    throws InvalidRequestPassedVerification {
        final DeviceId deviceId =
                parseOrThrowInvalidRequestPassedVerification(req.getDeviceId());
        final String c2dmRegistrationId = req.getC2DmRegistrationId();
        this.deviceRegistrar.registerDevice(userId, deviceId,
                c2dmRegistrationId);
    }
}