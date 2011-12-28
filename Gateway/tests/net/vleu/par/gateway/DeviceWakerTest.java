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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import net.vleu.par.ClientLoginToken;
import net.vleu.par.gateway.datastore.DeviceEntity;
import net.vleu.par.gateway.datastore.DeviceEntityTest;
import net.vleu.par.gateway.tests.ThreadGlobal;
import net.vleu.par.models.DeviceId;
import net.vleu.par.models.UserId;
import net.vleu.par.utils.C2dmRequestFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

@RunWith(MockitoJUnitRunner.class)
public class DeviceWakerTest {

    private static ClientLoginToken DUMMY_C2DM_AUTH_TOKEN =
            new ClientLoginToken("DUMMY_C2DM_AUTH_TOKEN");
    private static final UserId USER_ID = UserId.fromGoogleAuthId("dummyUser");
    @Mock
    private DatastoreService datastoreService;

    private final LocalServiceTestHelper helper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setStoreDelayMs(0));
    @Mock
    private Queue taskQueue;

    @Mock
    private URLFetchService urlFetchService;

    private DeviceWaker newDeviceWakerUsingMocks() {
        return new DeviceWaker(new ThreadGlobal<DatastoreService>(
                this.datastoreService), new C2dmRequestFactory(),
                this.taskQueue, new ThreadGlobal<URLFetchService>(
                        this.urlFetchService));
    }

    @Before
    public void setUpLocalServiceTest() {
        this.helper.setUp();
    }

    @After
    public void tearDownLocalServiceTest() {
        this.helper.tearDown();
    }

    /**
     * Tests that {@link DeviceWaker#queueWake(UserId, DeviceId)} queues a task.
     */
    @Test
    public void testQueueWake() throws IOException, EntityNotFoundException {
        final DeviceWaker deviceWaker = newDeviceWakerUsingMocks();
        deviceWaker.queueWake(USER_ID, DeviceEntityTest.DUMMY_DEVICE_ID);
        verify(this.taskQueue).add(any(TaskOptions.class));
    }

    /**
     * Test that {@link DeviceWaker#reallyWake(UserId, DeviceId)} fails when the
     * URLFetchService fails.
     * 
     * @throws IOException
     *             Test failed
     * @throws EntityNotFoundException
     *             Test failed
     */
    @Test(expected = IOException.class)
    public void testReallyWakeFailure() throws EntityNotFoundException,
            IOException {
        final Key dummyDeviceKey =
                DeviceEntity.keyForIds(USER_ID,
                        DeviceEntityTest.DUMMY_DEVICE_ID);
        final HTTPResponse mockedHttpResponse = mock(HTTPResponse.class);
        stub(
                this.datastoreService.get(any(Transaction.class),
                        eq(dummyDeviceKey))).toReturn(
                DeviceEntityTest.buildDummyDeviceEntity());
        stub(this.urlFetchService.fetch(any(HTTPRequest.class))).toReturn(
                mockedHttpResponse);
        stub(mockedHttpResponse.getResponseCode()).toReturn(404);
        final DeviceWaker tested = newDeviceWakerUsingMocks();
        tested.reallyWake(DUMMY_C2DM_AUTH_TOKEN, USER_ID,
                DeviceEntityTest.DUMMY_DEVICE_ID);
    }

    /**
     * Tests that {@link DeviceWaker#reallyWake(UserId, DeviceId)} can succeed.
     * 
     * @throws IOException
     *             Test failed
     * @throws EntityNotFoundException
     *             Test failed
     */
    @Test
    public void testReallyWakeSuccess() throws EntityNotFoundException,
            IOException {
        final Key dummyDeviceKey =
                DeviceEntity.keyForIds(USER_ID,
                        DeviceEntityTest.DUMMY_DEVICE_ID);
        final HTTPResponse mockedHttpResponse = mock(HTTPResponse.class);
        stub(
                this.datastoreService.get(any(Transaction.class),
                        eq(dummyDeviceKey))).toReturn(
                DeviceEntityTest.buildDummyDeviceEntity());
        stub(this.urlFetchService.fetch(any(HTTPRequest.class))).toReturn(
                mockedHttpResponse);
        stub(mockedHttpResponse.getResponseCode()).toReturn(200);
        final DeviceWaker tested = newDeviceWakerUsingMocks();
        tested.reallyWake(DUMMY_C2DM_AUTH_TOKEN, USER_ID,
                DeviceEntityTest.DUMMY_DEVICE_ID);
    }

}
