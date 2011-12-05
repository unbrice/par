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

import java.util.logging.Logger;

import net.jcip.annotations.ThreadSafe;
import net.vleu.par.models.UserId;

import com.google.appengine.api.oauth.OAuthRequestException;
import com.google.appengine.api.oauth.OAuthService;
import com.google.appengine.api.oauth.OAuthServiceFactory;
import com.google.appengine.api.oauth.OAuthServiceFailureException;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

/**
 * A collection of utility functions shared among Servlets
 */
@ThreadSafe
class ServletHelper {
    /** We only accept authentication through Google right now */
    private static final Object ALLOWED_AUTH_DOMAIN = "gmail.com";
    private static final Logger LOG = Logger.getLogger(ServletHelper.class
            .getName());
    private final ThreadLocal<OAuthService> oauthServices;

    private final ThreadLocal<UserService> userServices;

    public ServletHelper() {
        this(new ThreadLocal<UserService>() {
            @Override
            protected UserService initialValue() {
                return UserServiceFactory.getUserService();
            }
        }, new ThreadLocal<OAuthService>() {
            @Override
            protected OAuthService initialValue() {
                return OAuthServiceFactory.getOAuthService();
            }
        });
    }

    public ServletHelper(final ThreadLocal<UserService> userServices,
            final ThreadLocal<OAuthService> oauthServices) {
        this.userServices = userServices;
        this.oauthServices = oauthServices;
    }

    public UserId getCurrentUser() {
        User googleUser = null;
        try {
            /* OAuth has the priority */
            googleUser = this.oauthServices.get().getCurrentUser();
        }
        catch (final OAuthRequestException e) {
            /* Fallbacks to old Google Auth, because the Android AccountManager has no proper support for OAuth */
            googleUser = this.userServices.get().getCurrentUser();
        }
        catch (final OAuthServiceFailureException e) {
            LOG.warning("Failed contacting the OAuth service: " + e);
        }
        if (googleUser == null)
            return null;
        else if (!ALLOWED_AUTH_DOMAIN.equals(googleUser.getAuthDomain())) {
            LOG.warning("Got an user from an unknown domain: "
                + googleUser.getAuthDomain());
            return null;
        }
        else {
            final String googleAuthId = googleUser.getUserId();
            final UserId userId = UserId.fromGoogleAuthId(googleAuthId);
            return userId;
        }

    }

}
