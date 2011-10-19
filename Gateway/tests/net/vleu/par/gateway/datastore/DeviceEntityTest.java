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

import static org.junit.Assert.assertEquals;
import net.vleu.par.gateway.models.Device;
import net.vleu.par.gateway.models.DeviceId;
import net.vleu.par.gateway.models.UserId;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

public class DeviceEntityTest {

    private static final String C2DM_ID = "dummyC2DM";

    private static final DeviceId DEVICE_ID = DeviceId
            .fromBase64url("dGVzdHN0cmluZw");

    private static final UserId USER_ID = UserId.fromGoogleAuthId("dummyUser");

    private static Device buildDummyDevice() {
        return new Device(DEVICE_ID, C2DM_ID);
    }

    private final LocalServiceTestHelper helper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig());

    @Before
    public void setUp() {
        this.helper.setUp();
    }

    @After
    public void tearDown() {
        this.helper.tearDown();
    }

    /**
     * Test method for
     * {@link net.vleu.par.gateway.datastore.DeviceEntity#deviceFromEntity(com.google.appengine.api.datastore.Entity)}
     * .
     */
    @Test
    public void testDeviceFromEntity() {
        final Device device1 = buildDummyDevice();
        final Entity entity = DeviceEntity.entityFromDevice(USER_ID, device1);
        final Device device2 = DeviceEntity.deviceFromEntity(entity);
        assertEquals(device1, device2);
    }

    /**
     * Test method for
     * {@link net.vleu.par.gateway.datastore.DeviceEntity#entityFromDevice(net.vleu.par.gateway.models.UserId, net.vleu.par.gateway.models.Device)}
     * .
     */
    @Test
    public void testEntityFromDevice() {
        final Device device = buildDummyDevice();
        final Entity entity = DeviceEntity.entityFromDevice(USER_ID, device);
        assertEquals(C2DM_ID,
                entity.getProperty(DeviceEntity.C2DM_REGISTRATION_ID_PROPERTY));
        assertEquals(DEVICE_ID.toBase64url(), entity.getKey().getName());
        assertEquals(DeviceEntity.KIND, entity.getKind());
    }

    @Test
    public void testFullStoreAndRetrieve() throws EntityNotFoundException {
        final Device device1 = buildDummyDevice();
        final Entity entity1 = DeviceEntity.entityFromDevice(USER_ID, device1);
        final DatastoreService ds =
                DatastoreServiceFactory.getDatastoreService();
        ds.put(entity1);
        final Entity entity2 =
                ds.get(DeviceEntity.keyForIds(USER_ID, DEVICE_ID));
        final Device device2 = DeviceEntity.deviceFromEntity(entity2);
        assertEquals(device1, device2);

    }

    /**
     * Test method for
     * {@link net.vleu.par.gateway.datastore.DeviceEntity#keyForIds(net.vleu.par.gateway.models.UserId, net.vleu.par.gateway.models.DeviceId)}
     * .
     */
    @Test
    public void testKeyForIds() {
        final Key key = DeviceEntity.keyForIds(USER_ID, DEVICE_ID);
        assertEquals(DEVICE_ID.toBase64url(), key.getName());
        assertEquals(USER_ID.asString(), key.getParent().getName());
    }
}