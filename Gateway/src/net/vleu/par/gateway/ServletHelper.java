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

import java.util.ResourceBundle;
import java.util.logging.Logger;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import net.vleu.par.ClientLoginToken;
import net.vleu.par.gateway.datastore.ServerConfigurationEntity;
import net.vleu.par.models.UserId;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.memcache.InvalidValueException;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
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

    /**
     * {@link #memcache} must have this as namespace
     */
    public static final String MEMCACHE_NAMESPACE = ServletHelper.class
            .getSimpleName();

    private static final String MEMCACHE_SERVER_CONFIG_KEY =
            "ServerConfiguration-v0";

    /**
     * The GAE datastore from which to get the {@link ServerConfigurationEntity}
     * .
     */
    @GuardedBy("itself")
    private final DatastoreService datastore;

    /**
     * Its namespace must be {@link #MEMCACHE_NAMESPACE} Thread safe, according
     * to {@linkplain http ://groups.google.com/group/google-appengine-java
     * /browse_thread/thread/d3f1536084f59c22/6a877706e3f3a4ec }
     */
    private final MemcacheService memcache;

    /**
     * They have to be thread-local because the {@link OAuthService} are not
     * thread-safe.
     */
    private final ThreadLocal<OAuthService> oauthServices;

    /**
     * They have to be thread-local because the {@link UserService} are not
     * thread-safe.
     */
    private final ThreadLocal<UserService> userServices;

    public ServletHelper() {
        this(DatastoreServiceFactory.getDatastoreService(),
                MemcacheServiceFactory.getMemcacheService(MEMCACHE_NAMESPACE),
                new ThreadLocal<UserService>() {
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

    public ServletHelper(final DatastoreService datastore,
            final MemcacheService memcache,
            final ThreadLocal<UserService> userServices,
            final ThreadLocal<OAuthService> oauthServices) {
        assert (MEMCACHE_NAMESPACE.equals(memcache.getNamespace()));
        this.datastore = datastore;
        this.memcache = memcache;
        this.oauthServices = oauthServices;
        this.userServices = userServices;
    }

    public UserId getCurrentUser() {
        User googleUser = null;
        try {
            /* OAuth has the priority */
            googleUser = this.oauthServices.get().getCurrentUser();
        }
        catch (final OAuthRequestException e) {
            /*
             * Fallbacks to old Google Auth, because the Android AccountManager
             * has no proper support for OAuth
             */
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

    /**
     * Writes the {@link ServerConfiguration} in the datastore and memcache
     * services.
     * 
     * @param config
     *            The value that will be returned by subsequent calls to
     *            {@link #readServerConfiguration()}
     */
    public void persistServerConfiguration(final ServerConfiguration config) {
        final Entity entity =
                ServerConfigurationEntity.entityServerConfig(config);
        /* Saves in the datastore */
        synchronized (this.datastore) {
            this.datastore.put(null, entity);
        }
        /* Saves in the memcache */
        this.memcache.put(MEMCACHE_SERVER_CONFIG_KEY, entity);
    }

    /**
     * @return The {@link ServerConfiguration} value given to
     *         {@link #persistServerConfiguration(ServerConfiguration)}
     */
    public ServerConfiguration readServerConfiguration() {

        /* Searches in the memcache */
        try {
            final Entity entity =
                    (Entity) this.memcache.get(MEMCACHE_SERVER_CONFIG_KEY);
            if (entity != null)
                return ServerConfigurationEntity.serverConfigFromEntity(entity);
            else
                LOG.finest(MEMCACHE_SERVER_CONFIG_KEY
                    + " not found in memcache. Trying the datastore.");
        }
        catch (final InvalidValueException _) {
            LOG.finest("Error while accessing memcache. Trying the datastore.");
        }

        /* Searches in the datastore */
        try {

            final Entity entity;
            synchronized (this.datastore) {
                entity =
                        this.datastore.get(null, ServerConfigurationEntity.KEY);
            }
            return ServerConfigurationEntity.serverConfigFromEntity(entity);
        }
        catch (final EntityNotFoundException _) {
            LOG.warning(ServerConfigurationEntity.KEY.toString()
                + " not found in the datastore. Using the default values from the config file.");
        }

        /* Searches in the config file */
        {
            final String c2dmTokenStr =
                    ResourceBundle.getBundle("net.vleu.par.gateway.secrets")
                            .getString("C2DM_AUTH_TOKEN");
            final ServerConfiguration res =
                    new ServerConfiguration(new ClientLoginToken(c2dmTokenStr));
            persistServerConfiguration(res);
            return res;
        }
    }

}
