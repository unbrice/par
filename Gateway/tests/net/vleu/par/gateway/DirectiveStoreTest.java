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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import net.vleu.par.gateway.datastore.DeviceEntityTest;
import net.vleu.par.gateway.datastore.DirectiveEntityTest;
import net.vleu.par.gateway.datastore.TooManyConcurrentAccesses;
import net.vleu.par.gateway.models.Directive;
import net.vleu.par.gateway.models.UserId;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

/**
 * This is more an integration test than a unit test but who cares ?
 */
public class DirectiveStoreTest {
    private static final UserId USER_ID = UserId.fromGoogleAuthId("dummyUser");
    private final LocalServiceTestHelper helper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setStoreDelayMs(0));

    @Before
    public void setUp() {
        this.helper.setUp();
    }

    @After
    public void tearDown() {
        this.helper.tearDown();
    }

    @Test
    public void testEmptyDatastore() throws TooManyConcurrentAccesses {
        final DirectiveStore test = new DirectiveStore();
        final ArrayList<Directive> result =
                test.fetchAndDelete(USER_ID, DeviceEntityTest.DUMMY_DEVICE_ID);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testStoreTwiceThenFetchTwice() throws TooManyConcurrentAccesses {
        final DirectiveStore test = new DirectiveStore();
        test.store(USER_ID, DeviceEntityTest.DUMMY_DEVICE_ID,
                DirectiveEntityTest.DUMMY_DIRECTIVE);
        test.store(USER_ID, DeviceEntityTest.DUMMY_DEVICE_ID,
                DirectiveEntityTest.DUMMY_DIRECTIVE);
        final ArrayList<Directive> result1 =
                test.fetchAndDelete(USER_ID, DeviceEntityTest.DUMMY_DEVICE_ID);
        final ArrayList<Directive> result2 =
                test.fetchAndDelete(USER_ID, DeviceEntityTest.DUMMY_DEVICE_ID);
        assertEquals(2, result1.size());
        assertEquals(result1.get(0), DirectiveEntityTest.DUMMY_DIRECTIVE);
        assertEquals(result1.get(1), DirectiveEntityTest.DUMMY_DIRECTIVE);
        assertTrue(result2.isEmpty());
    }

}
