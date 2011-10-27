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

import net.jcip.annotations.ThreadSafe;
import net.vleu.par.gateway.models.UserId;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

/**
 * A collection of utility functions shared among Servlets
 */
@ThreadSafe
final class ServletHelper {
    static final InjectableThreadLocal<UserService> USER_SERVICES = new InjectableThreadLocal<UserService>() {
        protected UserService instantiateValue() {
            return UserServiceFactory.getUserService(); 
        };   
    };

    public synchronized UserId getCurrentUser() {
        final com.google.appengine.api.users.User googleUser =
                USER_SERVICES.get().getCurrentUser();
        if (googleUser == null)
            return null;
        final String googleAuthId = googleUser.getUserId();
        final UserId userId = UserId.fromGoogleAuthId(googleAuthId);
        return userId;
    }

}
