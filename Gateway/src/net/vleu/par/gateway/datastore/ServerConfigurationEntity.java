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
package net.vleu.par.gateway.datastore;

import net.vleu.par.ClientLoginToken;
import net.vleu.par.gateway.ServerConfiguration;
import net.vleu.par.models.User;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

/**
 * This entity helps to serializing the {@link ServerConfiguration}
 */
public final class ServerConfigurationEntity {
    private static final String C2DM_CLIENT_LOGIN_TOKEN_KEY =
            "c2dmClientLoginToken";
    
    private static final String KEY_NAME = "v0";

    private static final String KIND = "ServerConfiguration";


    public static final Key KEY = KeyFactory.createKey(KIND, KEY_NAME);
    
    /**
     * @return An {@link Entity} representing the user
     */
    public static Entity entityServerConfig(final ServerConfiguration config) {
        final Entity res = new Entity(KIND, KEY_NAME);
        res.setUnindexedProperty(C2DM_CLIENT_LOGIN_TOKEN_KEY,
                config.getC2dmAuthToken().value);
        return res;
    }

    /**
     * @return A {@link User}
     */
    public static ServerConfiguration
            serverConfigFromEntity(final Entity entity) {
        assert (entity.getKind() == KIND);
        final String c2dmClientLoginTokenStr =
                (String) entity.getProperty(C2DM_CLIENT_LOGIN_TOKEN_KEY);
        final ClientLoginToken c2dmClientLoginToken =
                new ClientLoginToken(c2dmClientLoginTokenStr);
        return new ServerConfiguration(c2dmClientLoginToken);
    }

    private ServerConfigurationEntity() {
    }
}
