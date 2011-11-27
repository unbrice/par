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
import java.util.logging.Level;
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
import net.vleu.par.models.Directive;
import net.vleu.par.models.GatewayRequest;
import net.vleu.par.models.UserId;
import net.vleu.par.protocolbuffer.Commands.DirectiveData;
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
    /**
     * This POJO describes the unfortunate outcome of a failed call to
     * {@link #doPostOrReturnError()}
     */
    private static class DoPostError {
        /** The HTTP code to send back to the user */
        public final int httpCode;
        public final Level logLevel;
        /** Will be logged with the severity associated to logLevel */
        public final String logMessage;
        /** The HTTP message to send back to the user */
        public final String userMessage;

        public DoPostError(final Level logLevel, final int httpCode,
                final Exception exception) {
            this(logLevel, exception.toString(), httpCode, exception.toString());
        }

        public DoPostError(final Level logLevel, final int httpCode,
                final String message) {
            this(logLevel, message, httpCode, message);
        }

        public DoPostError(final Level logLevel, final String logMessage,
                final int httpCode, final String userMessage) {
            this.logLevel = logLevel;
            this.logMessage = logMessage;
            this.httpCode = httpCode;
            this.userMessage = userMessage;
        }
    }

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
         * @see Exception#Exception(String message)
         */
        public InvalidRequestPassedVerification(final String string) {
            super(string);
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
                    checkOrThrowInvalidRequestPassedVerification(req
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
                    checkOrThrowInvalidRequestPassedVerification(req
                            .getDeviceId());
            ApiServlet.this.directiveStore.store(this.userId, deviceId,
                    directive);
            ApiServlet.this.deviceWaker.queueWake(this.userId, deviceId);

        }

        @Override
        public void visit(final RegisterDeviceData req)
                throws InvalidRequestPassedVerification {
            final DeviceId deviceId =
                    checkOrThrowInvalidRequestPassedVerification(req
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

    private static DeviceId checkOrThrowInvalidRequestPassedVerification(
            final String deviceIdStr) throws InvalidRequestPassedVerification {
        if (DeviceId.isValidDeviceIdString(deviceIdStr))
            return new DeviceId(deviceIdStr);
        else
            throw new InvalidRequestPassedVerification("Invalid DeviceId: "
                + deviceIdStr);
    }

    private static String joinStrings(final ArrayList<String> strings,
            final String separator) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (final String s : strings)
            stringBuilder.append(s);
        return stringBuilder.toString();
    }

    /**
     * Decides which encapsulation to use, according the URL
     * 
     * @param httpReq
     *            The request
     * @return One of {@link Encapsulation}
     */
    private static Encapsulation parseEncapsulation(
            final HttpServletRequest httpReq) {

        /* Validates request type */
        if (httpReq.getServletPath().endsWith(Config.SERVER_RPC_JSON_SUFFIX))
            return Encapsulation.JSON;
        else if (httpReq.getServletPath().endsWith(
                Config.SERVER_RPC_PROTOBUFF_SUFFIX))
            return Encapsulation.PROTOBUFF;
        else
            return null;

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
    private static GatewayRequestData parseRequest(final byte[] requestBytes,
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
     * Reads up to {@value #MAX_COMMAND_SIZE} bytes from the stream and returns
     * them
     * 
     * @param stream
     *            The stream to read from
     * @return A byte array of up to {@value #MAX_COMMAND_SIZE} bytes, null if
     *         it failed reading
     * @throws IOException
     *             If some I/O error occurs while reading the stream
     * 
     */
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
    private static void writeResponse(final HttpServletResponse httpResp,
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

    /**
     * Reads the request, handles it and writes down the response or the error
     * message. Depending on the URL suffix, the queries and responses are
     * expected to be encapsulated in JSON or ProtolBuffers.
     * 
     * The implementation just calls
     * {@link #doPostReturningErrors(HttpServletRequest, HttpServletResponse)}
     * to accomplish its task then logs and sends back the possible errors.
     * 
     * @param httpReq
     *            The request, must not be null
     * @param httpResp
     *            The response will be written on it, must not be null
     * @throws IOException
     *             Unrecoverable I/O error while sending the response
     */
    @Override
    public void doPost(final HttpServletRequest httpReq,
            final HttpServletResponse httpResp) throws IOException {
        final DoPostError error = doPostReturningErrors(httpReq, httpResp);
        if (error != null) {
            LOG.log(error.logLevel, error.logMessage);
            httpResp.sendError(error.httpCode, error.userMessage);
        }
    }

    /**
     * Parses the request, handles it and adds the response to the provided
     * {@link GatewayResponseData.Builder}, or returns a {@link DoPostError}.
     * 
     * This method does no I/Os.
     * 
     * @param encapsulation
     *            The {@link Encapsulation} to use for parsing the requestBytes
     * @param userId
     *            The {@link UserId} of the authenticated user who sent the
     *            request
     * @param requestBytes
     *            The unparsed request as a byte array
     * @param responseBuilder
     *            The response will be merged to it
     * @return null if everything went fine, else a {@link DoPostError}
     */
    private DoPostError doPostExceptIOs(final UserId userId,
            final Encapsulation encapsulation, final byte[] requestBytes,
            final GatewayResponseData.Builder responseBuilder) {

        final GatewayRequest request;
        final ArrayList<String> errors = new ArrayList<String>(0);

        /* Checks that the request is well-formed */
        {
            final GatewayRequestData requestPB =
                    parseRequest(requestBytes, errors, encapsulation);
            if (!GatewayRequest.isValid(requestPB, errors)) {
                final String logMsg =
                        "Requests rejected: " + joinStrings(errors, " --- \n");
                final String userMsg = joinStrings(errors, "<br />\n");
                return new DoPostError(Level.FINE, logMsg,
                        HttpCodes.HTTP_BAD_REQUEST_STATUS, userMsg);
            }
            request = new GatewayRequest(requestPB);
        }

        /* Handles the request */
        try {
            final RequestHandler handler =
                    new RequestHandler(responseBuilder, userId);
            request.accept(handler);
        }
        catch (final InvalidRequestPassedVerification e) {
            return new DoPostError(Level.SEVERE,
                    HttpCodes.HTTP_BAD_REQUEST_STATUS, e);
        }
        catch (final TooManyConcurrentAccesses e) {
            return new DoPostError(Level.SEVERE,
                    HttpCodes.HTTP_SERVICE_UNAVAILABLE_STATUS, e);

        }
        catch (final Exception e) {
            return new DoPostError(Level.SEVERE,
                    HttpCodes.HTTP_INTERNAL_ERROR_STATUS, e);
        }

        /* No errors ! */
        return null;
    }

    /**
     * Reads the request, handles it and writes down the response, or returns a
     * {@link DoPostError}. Depending on the URL suffix, the queries and
     * responses are expected to be encapsulated in JSON or ProtolBuffers.
     * 
     * The implementation just calls
     * {@link #doPostReturningErrors(HttpServletRequest, HttpServletResponse)}
     * to accomplish its task then logs and sends back the possible errors.
     * 
     * @param httpReq
     *            The request, must not be null
     * @param httpResp
     *            The response will be written on it, must not be null
     * @return null if everything went fine, else a {@link DoPostError}
     * @throws IOException
     *             Unrecoverable I/O error while sending the response This
     *             functions does the same as
     *             {@link #doPost(HttpServletRequest, HttpServletResponse)}
     *             excepts that it returns an {@link DoPostError} when it fails
     *             instead of handling the error itself.
     */
    private DoPostError doPostReturningErrors(final HttpServletRequest httpReq,
            final HttpServletResponse httpResp) throws IOException {
        /* Checks that the user is authenticated */
        final UserId userId = this.servletHelper.getCurrentUser();
        if (userId == null)
            return new DoPostError(Level.FINER, "Unauthenticated request",
                    HttpCodes.HTTP_FORBIDDEN_STATUS,
                    "Requests must be authenticated");

        /* Decide on encapsulation */
        final Encapsulation encapsulation = parseEncapsulation(httpReq);
        if (encapsulation == null)
            return new DoPostError(Level.FINE,
                    HttpCodes.HTTP_BAD_REQUEST_STATUS,
                    "Cannnot guess the required encapsulation");

        /* Reads the request */
        final byte[] requestBytes;
        try {
            requestBytes = readAllBytes(httpReq.getInputStream());
        }
        catch (final IOException e) {
            return new DoPostError(Level.FINE,
                    HttpCodes.HTTP_BAD_REQUEST_STATUS, e);
        }
        if (requestBytes == null || requestBytes.length == 0)
            return new DoPostError(Level.FINE,
                    HttpCodes.HTTP_BAD_REQUEST_STATUS,
                    "Protocol buffer rejected for its size (empty or too big)");

        /* Builds the response and call #doPostExceptIOs */
        final GatewayResponseData.Builder responseBuilder =
                GatewayResponseData.newBuilder();
        try {
            final DoPostError error =
                    doPostExceptIOs(userId, encapsulation, requestBytes,
                            responseBuilder);
            if (error != null)
                return error;

        }
        catch (final Exception e) {
            return new DoPostError(Level.SEVERE,
                    HttpCodes.HTTP_INTERNAL_ERROR_STATUS, e);
        }

        writeResponse(httpResp, encapsulation, responseBuilder.build());
        httpResp.flushBuffer();
        httpResp.getOutputStream().close();
        return null;
    }

}
