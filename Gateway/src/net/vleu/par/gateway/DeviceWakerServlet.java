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
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.jcip.annotations.ThreadSafe;
import net.vleu.par.ClientLoginToken;
import net.vleu.par.gateway.DeviceWaker.InvalidC2dmClientLoginToken;
import net.vleu.par.models.DeviceId;
import net.vleu.par.models.UserId;

import com.google.appengine.api.datastore.EntityNotFoundException;

/**
 * This class is a callback used by AppEngine's Tasks API for the tasks that are
 * queued by {@link DeviceWaker#queueWake(UserId, DeviceId)}
 */
@ThreadSafe
@SuppressWarnings("serial")
public class DeviceWakerServlet extends HttpServlet {
    public static final String APPENGINE_QUEUE_NAME = "deviceWakerQueue";
    /** Name of the HTTP parameter containing the Device ID */
    public static final String DEVICE_ID_HTTP_PARAM = "deviceId";
    private static final Logger LOG = Logger.getLogger(DeviceWakerServlet.class
            .getName());
    /**
     * Name of the HTTP parameter containing the User ID as a Base64 URL string
     */
    public static final String USER_ID_HTTP_PARAM = "userId";

    /**
     * A cached C2DM authentication token. <br>
     * It will be refreshed from the {@link #servletHelper} (
     * {@link ServletHelper#readServerConfiguration()}) when rejected by C2DM
     * server, thus handling the case where another node gets the updated value. <br>
     * May be null if there is no cached value. All accesses should be done
     * through {@link #getC2dmAuthToken()},
     * {@link #setC2dmAuthToken(ClientLoginToken)} and
     * {@link #resetCachedC2dmAuthToken()}
     * 
     * @see DeviceWakerServlet#getC2dmAuthToken()
     * @see DeviceWakerServlet#setC2dmAuthToken(ClientLoginToken)
     * @see DeviceWakerServlet#resetCachedC2dmAuthToken()
     */
    private volatile ClientLoginToken cachedC2dmAuthToken;

    private final DeviceWaker deviceWaker;

    private final ServletHelper servletHelper;

    public DeviceWakerServlet() {
        this(new DeviceWaker(), new ServletHelper());
    }

    /** Allows for injecting the private fields, for testing purposes */
    DeviceWakerServlet(final DeviceWaker deviceWaker,
            final ServletHelper servletHelper) {
        this.deviceWaker = deviceWaker;
        this.servletHelper = servletHelper;
    }

    /** @inherit */
    // TODO: Switch to ProtoRPC
    @Override
    public void doPost(final HttpServletRequest req,
            final HttpServletResponse resp) throws IOException {
        final DeviceId deviceId;
        final UserId userId;
        final String deviceIdStr = req.getParameter(DEVICE_ID_HTTP_PARAM);
        final String stringUserId = req.getParameter(USER_ID_HTTP_PARAM);

        /* Validates the request */
        if (deviceIdStr == null) {
            resp.sendError(HttpCodes.HTTP_BAD_REQUEST_STATUS, "No "
                + DEVICE_ID_HTTP_PARAM + " parameter");
            LOG.severe("No " + DEVICE_ID_HTTP_PARAM + " parameter");
            return;
        }
        else if (stringUserId == null) {
            resp.sendError(HttpCodes.HTTP_BAD_REQUEST_STATUS, "No "
                + USER_ID_HTTP_PARAM + " parameter");
            LOG.severe("No " + USER_ID_HTTP_PARAM + " parameter");
            return;
        }
        else if (req.getHeader("X-AppEngine-QueueName") == null) {
            resp.sendError(HttpCodes.HTTP_FORBIDDEN_STATUS,
                    "Requests must be made through a Queue");
            LOG.severe("No X-AppEngine-QueueName parameter");
            return;
        }

        /* Wakeups the device */
        if (DeviceId.isValidDeviceIdString(deviceIdStr))
            deviceId = new DeviceId(deviceIdStr);
        else {
            resp.sendError(HttpCodes.HTTP_BAD_REQUEST_STATUS, "Invalid "
                + DEVICE_ID_HTTP_PARAM);

            LOG.severe("Invalid " + DEVICE_ID_HTTP_PARAM);
            return;
        }
        userId = UserId.fromGoogleAuthId(stringUserId);
        try {
            final ClientLoginToken updatedC2dmAuthToken =
                    this.deviceWaker.reallyWake(getC2dmAuthToken(), userId,
                            deviceId);
            if (updatedC2dmAuthToken != null
                && !getC2dmAuthToken().equals(updatedC2dmAuthToken)) {
                LOG.info("Got updated auth token from C2DM servers: "
                    + updatedC2dmAuthToken);
                setC2dmAuthToken(updatedC2dmAuthToken);
            }
        }
        catch (final EntityNotFoundException e) {
            LOG.severe("Unknown device: " + deviceIdStr);
            resp.sendError(HttpCodes.HTTP_GONE_STATUS, "Unknown device: "
                + deviceIdStr);
            return;
        }
        catch (final InvalidC2dmClientLoginToken e) {
            // This handles the case where another server refreshes the token
            LOG.warning("Cached value of the C2DM auth token is invalid. Forcing a refresh.");
            resetCachedC2dmAuthToken();
            // GAE task queue will retry later
            resp.sendError(HttpCodes.HTTP_INTERNAL_ERROR_STATUS);
            return;
        }
        catch (final RuntimeException e) {
            LOG.severe("Failed waking the device: " + e.toString());
            throw e;
        }
        resp.setContentType("text/plain");
        resp.getWriter().println("Done.");
        // TODO: There is a race where the device can be awoken while
        // data are added, in which case it may not receive the data.
        // This could be fixed in a few way, the least problematic seems
        // to add two tasks, the second being anonymous and in charge for
        // checking that either the first is planned or that there are no
        // longer any items to deliver
    }

    /**
     * @return {@link #cachedC2dmAuthToken} if not null, else reads it from
     *         {@link ServletHelper#readServerConfiguration() it} and refreshes
     *         the cached copy
     */
    private ClientLoginToken getC2dmAuthToken() {
        ClientLoginToken res = this.cachedC2dmAuthToken;
        if (res == null) {
            final ServerConfiguration config =
                    this.servletHelper.readServerConfiguration();
            res = config.getC2dmAuthToken();
            this.cachedC2dmAuthToken = res;
        }
        return res;
    }

    /**
     * Sets {@link #cachedC2dmAuthToken} to null, thus discarding the cached
     * value
     */
    private void resetCachedC2dmAuthToken() {
        this.cachedC2dmAuthToken = null;
    }

    /**
     * Sets {@link #cachedC2dmAuthToken} and stores an updated configuration
     * using
     * {@link ServletHelper#persistServerConfiguration(ServerConfiguration)

     */
    private void setC2dmAuthToken(final ClientLoginToken newC2dmAuthToken) {
        assert (newC2dmAuthToken != null);
        this.cachedC2dmAuthToken = newC2dmAuthToken;
        final ServerConfiguration config =
                this.servletHelper.readServerConfiguration();
        config.setC2dmCAuthToken(newC2dmAuthToken);
        this.servletHelper.persistServerConfiguration(config);
    }
}
