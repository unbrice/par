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
    public static final String C2DM_DELAY_PROPERTY = "c2dmDelay";
    public static final String C2DM_DELAY_RESET_PROPERTY = "c2dmDelay";
    public static final String C2DM_REGISTRATION_ID_PROPERTY =
            "c2dmRegistrationId";
    public static final String KIND = "Device";
    public static final String SHARED_SECRET_PROPERTY = "sharedSecret";

    private static Device deviceFromEntity(final Entity entity) {
        final DeviceId id = new DeviceId(entity.getKey().getId());
        final long c2dmDelay = (Long) entity.getProperty(C2DM_DELAY_PROPERTY);
        final long c2dmDelayReset =
                (Long) entity.getProperty(C2DM_DELAY_RESET_PROPERTY);
        final String c2dmRegistrationId =
                (String) entity.getProperty(C2DM_REGISTRATION_ID_PROPERTY);
        final String sharedSecret =
                (String) entity.getProperty(SHARED_SECRET_PROPERTY);
        return new Device(id, c2dmDelay, c2dmDelayReset, c2dmRegistrationId,
                sharedSecret);
    }

    public static Entity entityFromDevice(final Device device) {
        final Entity res = new Entity(KIND, device.getId().asLong);
        res.setUnindexedProperty(C2DM_DELAY_PROPERTY, device.getC2dmDelay());
        res.setUnindexedProperty(C2DM_DELAY_RESET_PROPERTY,
                device.getC2dmDelayReset());
        res.setUnindexedProperty(C2DM_REGISTRATION_ID_PROPERTY,
                device.getC2dmRegistrationId());
        res.setUnindexedProperty(SHARED_SECRET_PROPERTY,
                device.getSharedSecret());
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
