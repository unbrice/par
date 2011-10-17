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

    private final UserService userService;

    public DeviceRegistrarServlet() {
        this(UserServiceFactory.getUserService());
    }

    /** For dependency-injection during tests */
    DeviceRegistrarServlet(final UserService userService) {
        this.userService = userService;
    }

    /** @inherit */
    @Override
    public void doGet(final HttpServletRequest req,
            final HttpServletResponse resp) throws IOException {
        final com.google.appengine.api.users.User googleUser =
                this.userService.getCurrentUser();
        if (googleUser == null) {
            resp.sendError(HTTPCodes.HTTP_FORBIDDEN_STATUS,
                    "Requests must be authenticated");
            return;
        }
        final String googleAuthId =
                this.userService.getCurrentUser().getUserId();
        final UserId userId = UserId.fromGoogleAuthId(googleAuthId);

    }
}
