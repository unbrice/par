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

import net.vleu.par.gateway.models.Device;
import net.vleu.par.gateway.models.DeviceId;
import net.vleu.par.gateway.models.UserId;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;

public abstract class DeviceEntity {
    public static final String C2DM_REGISTRATION_ID_PROPERTY =
            "c2dmRegistrationId";
    public static final String KIND = "Device";

    static Query buildQueryForOwnedDevices(final UserId ownerId) {
        final Key parentKey = UserEntity.keyForId(ownerId);
        return new Query(KIND, parentKey);
    }

    public static Device deviceFromEntity(final Entity entity) {
        final DeviceId id = new DeviceId(entity.getKey().getId());
        final String c2dmRegistrationId =
                (String) entity.getProperty(C2DM_REGISTRATION_ID_PROPERTY);
        return new Device(id, c2dmRegistrationId);
    }

    public static Entity entityFromDevice(final UserId ownerId,
            final Device device) {
        final DeviceId deviceId = device.getId();
        final Key ownerKey = UserEntity.keyForId(ownerId);
        final Entity res = new Entity(KIND, deviceId.asLong, ownerKey);
        res.setUnindexedProperty(C2DM_REGISTRATION_ID_PROPERTY,
                device.getC2dmRegistrationId());
        return res;
    }

    public static Key keyForIds(final UserId ownerId, final DeviceId deviceId) {
        final Key parentKey = UserEntity.keyForId(ownerId);
        return parentKey.getChild(KIND, deviceId.asLong);
    }
}