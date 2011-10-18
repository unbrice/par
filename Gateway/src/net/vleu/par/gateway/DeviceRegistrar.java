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

import net.vleu.par.gateway.datastore.DeviceEntity;
import net.vleu.par.gateway.models.Device;
import net.vleu.par.gateway.models.DeviceId;
import net.vleu.par.gateway.models.UserId;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;

/**
 *
 */
public final class DeviceRegistrar {
    /** The GAE datastore where to store the {@link DeviceEntity} */
    private final DatastoreService datastore;

    public DeviceRegistrar() {
        this(DatastoreServiceFactory.getDatastoreService());
    }

    /** Allows for injecting the private fields, for testing purposes */
    DeviceRegistrar(final DatastoreService datastore) {
        this.datastore = datastore;
    }

    /**
     * @param ownerId
     *            The user who registered the device
     * @param deviceId
     *            The device to wake up.
     * @param c2dmRegistrationId
     */
    public void registerDevice(final UserId ownerId, final DeviceId deviceId,
            final String c2dmRegistrationId) {
        final Device device = new Device(deviceId, c2dmRegistrationId);
        final Entity deviceEntity =
                DeviceEntity.entityFromDevice(ownerId, device);
        this.datastore.put(deviceEntity);
    }

}
