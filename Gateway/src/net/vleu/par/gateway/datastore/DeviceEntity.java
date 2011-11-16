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

import net.vleu.par.C2dmToken;
import net.vleu.par.DeviceName;
import net.vleu.par.models.Device;
import net.vleu.par.models.DeviceId;
import net.vleu.par.models.UserId;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;

public final class DeviceEntity {
    /** Undefined if the device is not registered */
    static final String C2DM_REGISTRATION_ID_PROPERTY = "c2dmRegistrationId";
    static final String FRIENDLY_NAME_PROPERTY = "friendlyName";
    static final String KIND = "Device";

    static Query buildQueryForOwnedDevices(final UserId ownerId) {
        final Key parentKey = UserEntity.keyForId(ownerId);
        return new Query(KIND, parentKey);
    }

    public static Device deviceFromEntity(final Entity entity) {
        assert (entity.getKind() == KIND);
        final String c2dmRegistrationIdStr =
                (String) entity.getProperty(C2DM_REGISTRATION_ID_PROPERTY);
        final C2dmToken c2dmRegistrationId;
        final DeviceName friendlyName =
                new DeviceName(
                        (String) entity.getProperty(FRIENDLY_NAME_PROPERTY));
        final DeviceId id =
                DeviceId.fromBase64urlWithNoVerifications(entity.getKey()
                        .getName());
        if (c2dmRegistrationIdStr != null)
            c2dmRegistrationId = new C2dmToken(c2dmRegistrationIdStr);
        else
            c2dmRegistrationId = null;
        return new Device(id, friendlyName, c2dmRegistrationId);
    }

    public static Entity entityFromDevice(final UserId ownerId,
            final Device device) {
        final Key deviceKey = keyForIds(ownerId, device.getId());
        final Entity res =
                new Entity(KIND, deviceKey.getName(), deviceKey.getParent());
        if (device.hasC2dmRegistrationId())
            res.setUnindexedProperty(C2DM_REGISTRATION_ID_PROPERTY,
                    device.getC2dmRegistrationId().value);
        return res;
    }

    public static Key keyForIds(final UserId ownerId, final DeviceId deviceId) {
        final Key parentKey = UserEntity.keyForId(ownerId);
        return parentKey.getChild(KIND, deviceId.toBase64url());
    }

    private DeviceEntity() {
    }
}
