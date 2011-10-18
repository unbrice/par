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

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.vleu.par.gateway.models.DeviceId;
import net.vleu.par.gateway.models.UserId;

import com.google.appengine.api.datastore.EntityNotFoundException;

@SuppressWarnings("serial")
public class DeviceWakerServlet extends HttpServlet {
    public static final String APPENGINE_QUEUE_NAME = "deviceWakerQueue";
    /** Name of the HTTP parameter containing the Device ID */
    public static final String DEVICE_ID_HTTP_PARAM = "deviceId";
    /**
     * Name of the HTTP parameter containing the User ID as a Base64 URL string
     */
    public static final String USER_ID_HTTP_PARAM = "userId";
    private final DeviceWaker deviceWaker;

    public DeviceWakerServlet() {
        this(new DeviceWaker());
    }

    /** Allows for injecting the private fields, for testing purposes */
    DeviceWakerServlet(final DeviceWaker deviceWaker) {
        this.deviceWaker = deviceWaker;
    }

    /** @inherit */
    @Override
    public void doGet(final HttpServletRequest req,
            final HttpServletResponse resp) throws IOException {
        final DeviceId deviceId;
        final UserId userId;
        final String base64urlDeviceId = req.getParameter(DEVICE_ID_HTTP_PARAM);
        final String stringUserId = req.getParameter(USER_ID_HTTP_PARAM);

        /* Validates the request */
        if (base64urlDeviceId == null) {
            resp.sendError(HttpCodes.HTTP_BAD_REQUEST_STATUS, "No "
                + DEVICE_ID_HTTP_PARAM + " parameter");
            return;
        }
        else if (stringUserId == null) {
            resp.sendError(HttpCodes.HTTP_BAD_REQUEST_STATUS, "No "
                + USER_ID_HTTP_PARAM + " parameter");
            return;
        }
        else if (req.getHeader("X-AppEngine-QueueName") == null) {
            resp.sendError(HttpCodes.HTTP_FORBIDDEN_STATUS,
                    "Requests must be made through a Queue");
            return;
        }

        /* Wakeups the device */
        deviceId = DeviceId.fromBase64url(base64urlDeviceId);
        userId = UserId.fromGoogleAuthId(stringUserId);
        try {
            this.deviceWaker.wake(userId, deviceId);
        }
        catch (final EntityNotFoundException e) {
            resp.sendError(HttpCodes.HTTP_GONE_STATUS, "Unknown device: "
                + base64urlDeviceId);
            return;
        }
        resp.setContentType("text/plain");
        resp.getWriter().println("Done.");
    }
}
