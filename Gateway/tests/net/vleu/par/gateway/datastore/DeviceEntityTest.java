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
import net.vleu.par.C2dmToken;
import net.vleu.par.DeviceName;
import net.vleu.par.gateway.models.UserIdTest;
import net.vleu.par.models.Device;
import net.vleu.par.models.DeviceId;

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

/**
 * Tests for {@link DeviceEntity}
 */
public class DeviceEntityTest {

    private static final C2dmToken C2DM_TOKEN = new C2dmToken("dummyC2DM");

    public static final DeviceId DUMMY_DEVICE_ID = DeviceId
            .fromBase64urlWithNoVerifications("CTJ5BgAAAAAA");
    
    public static DeviceName DUMMY_DEVICE_NAME = new DeviceName("dummyDevice");

    private static Device buildDummyDevice() {
        return new Device(DUMMY_DEVICE_ID, DUMMY_DEVICE_NAME, C2DM_TOKEN);
    }

    public static Entity buildDummyDeviceEntity() {
        return DeviceEntity.entityFromDevice(UserIdTest.DUMMY_USER_ID,
                buildDummyDevice());
    }

    private final LocalServiceTestHelper helper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setStoreDelayMs(0));

    @Before
    public void setUpLocalServiceTest() {
        this.helper.setUp();
    }

    @After
    public void tearDownLocalServiceTest() {
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
        final Entity entity =
                DeviceEntity
                        .entityFromDevice(UserIdTest.DUMMY_USER_ID, device1);
        final Device device2 = DeviceEntity.deviceFromEntity(entity);
        assertEquals(device1, device2);
    }

    /**
     * Test method for
     * {@link net.vleu.par.gateway.datastore.DeviceEntity#entityFromDevice(net.vleu.par.models.UserId, net.vleu.par.models.Device)}
     * .
     */
    @Test
    public void testEntityFromDevice() {
        final Device device = buildDummyDevice();
        final Entity entity =
                DeviceEntity.entityFromDevice(UserIdTest.DUMMY_USER_ID, device);
        assertEquals(C2DM_TOKEN.value,
                entity.getProperty(DeviceEntity.C2DM_REGISTRATION_ID_PROPERTY));
        assertEquals(DUMMY_DEVICE_ID.toBase64url(), entity.getKey().getName());
        assertEquals(DeviceEntity.KIND, entity.getKind());
    }

    @Test
    public void testFullStoreAndRetrieve() throws EntityNotFoundException {
        final Device device1 = buildDummyDevice();
        final Entity entity1 =
                DeviceEntity
                        .entityFromDevice(UserIdTest.DUMMY_USER_ID, device1);
        final DatastoreService ds =
                DatastoreServiceFactory.getDatastoreService();
        ds.put(null, entity1);
        final Entity entity2 =
                ds.get(DeviceEntity.keyForIds(UserIdTest.DUMMY_USER_ID,
                        DUMMY_DEVICE_ID));
        final Device device2 = DeviceEntity.deviceFromEntity(entity2);
        assertEquals(device1, device2);
    }

    /**
     * Test method for
     * {@link net.vleu.par.gateway.datastore.DeviceEntity#keyForIds(net.vleu.par.models.UserId, net.vleu.par.models.DeviceId)}
     * .
     */
    @Test
    public void testKeyForIds() {
        final Key key =
                DeviceEntity.keyForIds(UserIdTest.DUMMY_USER_ID,
                        DUMMY_DEVICE_ID);
        assertEquals(DUMMY_DEVICE_ID.toBase64url(), key.getName());
        assertEquals(UserIdTest.DUMMY_USER_ID.asString(), key.getParent()
                .getName());
    }
}
