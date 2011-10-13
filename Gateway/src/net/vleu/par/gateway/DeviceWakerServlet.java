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

import com.google.appengine.api.datastore.EntityNotFoundException;

@SuppressWarnings("serial")
public class DeviceWakerServlet extends HttpServlet {
    public static final String APPENGINE_QUEUE_NAME = "deviceWakerQueue";
    /** Name of the HTTP parameter containing the Device ID */
    public static final String DEVICE_ID_HTTP_PARAM = "deviceId";
    /** HTTP error code 400 */
    private static final int HTTP_BAD_REQUEST_STATUS = 400;
    /** HTTP error code 403 */
    private static final int HTTP_FORBIDDEN_STATUS = 403;
    /** HTTP error code 410 */
    private static final int HTTP_GONE_STATUS = 410;
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
        final String base64DeviceId = req.getParameter(DEVICE_ID_HTTP_PARAM);

        /* Validate the request */
        if (base64DeviceId == null) {
            resp.sendError(HTTP_BAD_REQUEST_STATUS, "No device parameter");
            return;
        }
        else if (req.getHeader("X-AppEngine-QueueName") == null) {
            resp.sendError(HTTP_FORBIDDEN_STATUS,
                    "Requests must be made through a Queue");
            return;
        }
        deviceId = DeviceId.fromBase64(base64DeviceId);
        try {
            this.deviceWaker.wake(deviceId);
        }
        catch (final EntityNotFoundException e) {
            resp.sendError(HTTP_GONE_STATUS, "Unknown device: "
                + base64DeviceId);
            return;
        }
        resp.setContentType("text/plain");
        resp.getWriter().println("Done.");
    }
}
