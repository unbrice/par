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

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

public abstract class DeviceEntity {
    public static final String C2DM_REGISTRATION_ID_PROPERTY =
            "c2dmRegistrationId";
    public static final String KIND = "Device";
    public static final String OWNER_PROPERTY = "owner";

    private static Device deviceFromEntity(final Entity entity) {
        final DeviceId id = new DeviceId(entity.getKey().getId());
        final String c2dmRegistrationId =
                (String) entity.getProperty(C2DM_REGISTRATION_ID_PROPERTY);
        final String owner =
                (String) entity.getProperty(OWNER_PROPERTY);
        return new Device(id, c2dmRegistrationId, owner);
    }

    public static Entity entityFromDevice(final Device device) {
        final Entity res = new Entity(KIND, device.getId().asLong);
        res.setUnindexedProperty(C2DM_REGISTRATION_ID_PROPERTY,
                device.getC2dmRegistrationId());
        res.setUnindexedProperty(OWNER_PROPERTY,
                device.getOwner());
        return res;
    }

    public static Device getDevice(final DatastoreService datastore,
            final DeviceId deviceId) throws EntityNotFoundException {
        final Entity entity;
        entity = datastore.get(keyForDeviceId(deviceId));
        return deviceFromEntity(entity);
    }

    private static Key keyForDeviceId(final DeviceId deviceId) {
        return KeyFactory.createKey(KIND, deviceId.asLong);
    }
}
