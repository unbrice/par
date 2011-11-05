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

import net.vleu.par.gateway.models.User;
import net.vleu.par.gateway.models.UserId;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

public final class UserEntity {
    public static final String KIND = "User";

    /**
     * @return An {@link Entity} representing the user
     */
    public static Entity entityFromUser(final User user) {
        return new Entity(KIND, user.getId().asString());
    }

    /**
     * @param id
     * @return A suitable Key for use with GAE's datastore.
     */
    public static Key keyForId(final UserId id) {
        return KeyFactory.createKey(KIND, id.asString());
    }

    /**
     * @return A {@link User}
     */
    public static User userFromEntity(final Entity entity) {
        assert (entity.getKind() == KIND);
        final UserId userId =
                UserId.fromGoogleAuthId(entity.getKey().getName());
        return new User(userId);
    }
    
    private UserEntity() {
    }
}
