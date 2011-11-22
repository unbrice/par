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
import java.util.Arrays;
import java.util.logging.Logger;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.jcip.annotations.ThreadSafe;
import net.vleu.par.C2dmToken;
import net.vleu.par.Config;
import net.vleu.par.DeviceName;
import net.vleu.par.gateway.datastore.TooManyConcurrentAccesses;
import net.vleu.par.models.DeviceId;
import net.vleu.par.models.DeviceId.InvalidDeviceIdSerialisation;
import net.vleu.par.models.Directive;
import net.vleu.par.models.GatewayRequest;
import net.vleu.par.models.UserId;
import net.vleu.par.protocolbuffer.Commands.DirectiveData;
import net.vleu.par.protocolbuffer.Devices.DeviceIdData;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayRequestData;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayRequestData.GetDeviceDirectivesData;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayRequestData.QueueDirectiveData;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayRequestData.RegisterDeviceData;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayResponseData;
import net.vleu.par.protocolbuffer.SchemaGatewayCommands;

import com.dyuproject.protostuff.JsonIOUtil;
import com.google.protobuf.InvalidProtocolBufferException;

@ThreadSafe
@SuppressWarnings("serial")
public final class ApiServlet extends HttpServlet {
    private static enum Encapsulation {
        JSON, PROTOBUFF
    }

    /**
     * Thrown when a request is discovered to be invalid after the point where
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

    private class RequestHandler implements GatewayRequest.ThrowingVisitor {
        final GatewayResponseData.Builder resp;
        final UserId userId;

        public RequestHandler(final GatewayResponseData.Builder resp,
                final UserId userId) {
            this.userId = userId;
            this.resp = resp;
        }

        @Override
        public void visit(final GetDeviceDirectivesData req)
                throws InvalidRequestPassedVerification,
                TooManyConcurrentAccesses {
            final DeviceId deviceId =
                    parseOrThrowInvalidRequestPassedVerification(req
                            .getDeviceId());
            final ArrayList<Directive> directives =
                    ApiServlet.this.directiveStore.fetchAndDelete(this.userId,
                            deviceId);
            for (final Directive directive : directives)
                this.resp.addDirective(directive.asProtocolBuffer());

        }

        @Override
        public void visit(final QueueDirectiveData req)
                throws InvalidRequestPassedVerification,
                TooManyConcurrentAccesses {
            final DirectiveData directiveData = req.getDirective();
            final Directive directive = new Directive(directiveData);
            final DeviceId deviceId =
                    parseOrThrowInvalidRequestPassedVerification(req
                            .getDeviceId());
            ApiServlet.this.directiveStore.store(this.userId, deviceId,
                    directive);
            ApiServlet.this.deviceWaker.queueWake(this.userId, deviceId);

        }

        @Override
        public void visit(final RegisterDeviceData req)
                throws InvalidRequestPassedVerification {
            final DeviceId deviceId =
                    parseOrThrowInvalidRequestPassedVerification(req
                            .getDeviceId());
            final C2dmToken c2dmRegistrationId;
            final DeviceName friendlyName =
                    new DeviceName(req.getFriendlyName());
            if (req.hasC2DMRegistrationId())
                c2dmRegistrationId = new C2dmToken(req.getC2DMRegistrationId());
            else
                c2dmRegistrationId = null;
            ApiServlet.this.deviceRegistrar.registerDevice(this.userId,
                    deviceId, friendlyName, c2dmRegistrationId);
        }
    }

    private static final Logger LOG = Logger.getLogger(ApiServlet.class
            .getName());

    /** Maximal size in bytes for the serialized protocol buffers we accept */
    public static final int MAX_COMMAND_SIZE = 1024;

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
        final byte[] buffer = new byte[MAX_COMMAND_SIZE];
        int position = 0;
        int lastRet;
        do {
            lastRet =
                    stream.read(buffer, position, MAX_COMMAND_SIZE - position);
            if (lastRet > 0)
                position += lastRet;
        } while (lastRet > 0);
        if (lastRet == 0) {
            LOG.warning("Truncating a too large input");
            return null;
        }
        return Arrays.copyOf(buffer, position);
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
    };
    
    /** @inherit */
    @Override
    public void doPost(final HttpServletRequest httpReq,
            final HttpServletResponse httpResp) throws IOException {
        final UserId userId = this.servletHelper.getCurrentUser();
        final GatewayRequest request;
        final Encapsulation encapsulationType;

        /* Checks that the user is authenticated */
        if (userId == null) {
            httpResp.sendError(HttpCodes.HTTP_FORBIDDEN_STATUS,
                    "Requests must be authenticated");
            LOG.finer("Unauthenticated request");
            return;
        }

        /* Validates request type */
        if (httpReq.getServletPath().endsWith(Config.SERVER_RPC_JSON_SUFFIX))
            encapsulationType = Encapsulation.JSON;
        else if (httpReq.getServletPath().endsWith(
                Config.SERVER_RPC_PROTOBUFF_SUFFIX))
            encapsulationType = Encapsulation.PROTOBUFF;
        else {
            final String msg = "Invalid URL suffix: '" + httpReq.getServletPath() + "'";
            LOG.fine(msg);
            httpResp.sendError(HttpCodes.HTTP_BAD_REQUEST_STATUS, msg);
            return;
        }

        /* Checks that the request is well-formed */
        {
            final byte[] requestBytes;
            final ArrayList<String> errors = new ArrayList<String>(0);
            requestBytes = readAllBytes(httpReq.getInputStream());
            if (requestBytes == null || requestBytes.length == 0) {
                final String msg =
                        "Protocol buffer rejected for its size (empty or too big)";
                LOG.fine(msg);
                httpResp.sendError(HttpCodes.HTTP_BAD_REQUEST_STATUS, msg);
                return;
            }

            final GatewayRequestData requestPB =
                    parseRequest(requestBytes, errors, encapsulationType);
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
            writeResponse(httpResp, encapsulationType, responseProto);
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
        catch (final Exception e) {
            httpResp.sendError(HttpCodes.HTTP_INTERNAL_ERROR_STATUS,
                    e.toString());
            LOG.severe(e.toString());
            return;
        }
    }

    private void handleRequest(final UserId userId, final GatewayRequest req,
            final GatewayResponseData.Builder resp) throws Exception {
        final RequestHandler handler = new RequestHandler(resp, userId);
        req.accept(handler);
    }

    /**
     * Parses the request from bytes to a protocol buffer. Must be thread-safe.
     * 
     * @param requestBytes
     *            The byte array, never null or empty.
     * @param errors
     *            Strings describing the errors will be added to it
     * @param encapsulation
     * @see Encapsulation
     * @return null if parsing failed, the parsed Protocol Buffer else
     */
    private GatewayRequestData parseRequest(final byte[] requestBytes,
            final ArrayList<String> errors, final Encapsulation encapsulation) {
        switch (encapsulation) {
        case PROTOBUFF:
            try {
                return GatewayRequestData.parseFrom(requestBytes);
            }
            catch (final InvalidProtocolBufferException e) {
                errors.add(e.getMessage());
                return null;
            }
        case JSON:
            final GatewayRequestData.Builder res =
                    GatewayRequestData.newBuilder();
            try {
                JsonIOUtil.mergeFrom(requestBytes, 0, requestBytes.length, res,
                        SchemaGatewayCommands.GatewayRequestData.MERGE,
                        Config.SERVER_RPC_JSON_NUMERIC);
            }
            catch (final IOException e) {
                errors.add("Invalid JSON: " + e.getMessage());
                return null;
            }
            return res.build();
        default:
            throw new InternalError("Unknow encapsulation: " + encapsulation);
        }
    }

    /**
     * Serializes the {@link GatewayResponseData} onto the HttpServletResponse.
     * Must be thread-safe.
     * 
     * This function is responsible for calling
     * {@link HttpServletResponse#setContentType(String)}
     * {@link HttpServletResponse#setContentLength(int)} and writing on
     * {@link HttpServletResponse#getOutputStream()}
     * 
     * @param httpResp
     *            Must not be null
     * @param encapsulation
     * @see Encapsulation
     * @param responseProto
     *            Must not be null
     * @throws IOException
     *             Failed in writing or serializing the response
     */
    private void writeResponse(final HttpServletResponse httpResp,
            final Encapsulation encapsulation,
            final GatewayResponseData responseProto) throws IOException {
        final ServletOutputStream outputStream = httpResp.getOutputStream();
        final byte[] respBytes;
        switch (encapsulation) {
        case PROTOBUFF:
            httpResp.setContentType("application/octet-stream");
            respBytes = responseProto.toByteArray();
            break;
        case JSON:
            httpResp.setContentType("application/json");
            respBytes =
                    JsonIOUtil.toByteArray(responseProto,
                            SchemaGatewayCommands.GatewayResponseData.WRITE,
                            Config.SERVER_RPC_JSON_NUMERIC);
            break;
        default:
            throw new InternalError("Unknow encapsulation: " + encapsulation);
        }
        httpResp.setContentLength(respBytes.length);
        outputStream.write(respBytes);
    }

}
