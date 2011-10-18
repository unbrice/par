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

import net.vleu.par.gateway.models.UserId;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

@SuppressWarnings("serial")
public class DeviceRegistrarServlet extends HttpServlet {
    /** Name of the HTTP parameter containing the Device ID */
    public static final String DEVICE_ID_HTTP_PARAM = "deviceId";

    private final ServletHelper servletHelper;

    public DeviceRegistrarServlet() {
        this(new ServletHelper());
    }

    /** For dependency-injection during tests */
    DeviceRegistrarServlet(final ServletHelper servletHelper) {
        this.servletHelper = servletHelper;
    }

    /** @inherit */
    @Override
    public void doGet(final HttpServletRequest req,
            final HttpServletResponse resp) throws IOException {
        final UserId userId =
                this.servletHelper.getCurrentUser();
        if (userId == null) {
            resp.sendError(HttpCodes.HTTP_FORBIDDEN_STATUS,
                    "Requests must be authenticated");
            return;
        }
        final String base64urlDeviceId = req.getParameter(DEVICE_ID_HTTP_PARAM);
        
    }
}
