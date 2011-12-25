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

import static com.google.appengine.api.datastore.FetchOptions.Builder.withChunkSize;

import java.util.ArrayList;
import java.util.List;

import sun.util.LocaleServiceProviderPool.LocalizedObjectGetter;

import net.jcip.annotations.ThreadSafe;
import net.vleu.par.C2dmToken;
import net.vleu.par.DeviceName;
import net.vleu.par.gateway.datastore.DeviceEntity;
import net.vleu.par.gateway.datastore.ThreadLocalDatastoreService;
import net.vleu.par.models.Device;
import net.vleu.par.models.DeviceId;
import net.vleu.par.models.UserId;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;

@ThreadSafe
public class DeviceRegistrar {

    private static final FetchOptions FETCH_ALL_OPTIONS = withChunkSize(
            Integer.MAX_VALUE).prefetchSize(Integer.MAX_VALUE);

    /**
     * The GAE datastore where to store the {@link DeviceEntity}. They have to
     * be local because the {@link DatastoreService} are not thread-safe.
     */
    private final ThreadLocal<DatastoreService> datastores;

    public DeviceRegistrar() {
        this(ThreadLocalDatastoreService.getSingleton());
    }

    /**
     * Allows dependency injection, for testing purposes.
     * 
     * @param datastores
     *            ThreadGlobal datastores.
     */
    DeviceRegistrar(final ThreadLocal<DatastoreService> datastores) {
        this.datastores = datastores;
    }

    /**
     * @param ownerId
     *            The user who registered the device
     */
    public ArrayList<Device> enumerateOwnedDevices(final UserId ownerId) {
        final ArrayList<Device> result = new ArrayList<Device>();
        final DatastoreService datastore = this.datastores.get();
        final Query query = DeviceEntity.buildQueryForOwnedDevices(ownerId);
        final List<Entity> queryResult =
                datastore.prepare(null, query).asList(FETCH_ALL_OPTIONS);
        for (final Entity entity : queryResult)
            result.add(DeviceEntity.deviceFromEntity(entity));
        return result;
    }

    /**
     * @param ownerId
     *            The user who registered the device
     * @param deviceId
     *            The device to wake up.
     * @param friendlyName
     *            A user friendly name
     * @param c2dmRegistrationId
     *            Can be null
     */
    public void registerDevice(final UserId ownerId, final DeviceId deviceId,
            final DeviceName friendlyName, final C2dmToken c2dmRegistrationId) {
        final Device device =
                new Device(deviceId, friendlyName, c2dmRegistrationId);
        final Entity deviceEntity =
                DeviceEntity.entityFromDevice(ownerId, device);
        this.datastores.get().put(null, deviceEntity);
    }
}
