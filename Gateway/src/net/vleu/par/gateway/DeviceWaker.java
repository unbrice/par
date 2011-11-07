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

import java.io.IOException;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import net.jcip.annotations.ThreadSafe;
import net.vleu.par.gateway.datastore.DeviceEntity;
import net.vleu.par.gateway.datastore.ThreadLocalDatastoreService;
import net.vleu.par.gateway.models.Device;
import net.vleu.par.gateway.models.DeviceId;
import net.vleu.par.gateway.models.UserId;
import net.vleu.par.utils.C2dmRequestFactory;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskAlreadyExistsException;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TransientFailureException;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;

@ThreadSafe
public class DeviceWaker {

    private static final Logger LOG = Logger.getLogger(DeviceWaker.class
            .getName());

    /**
     * We will wait at least this much time before to wakeing a device, so as to
     * prevent flooding C2DM if flooding the
     */
    private static final long WAKE_DELAY_MILLIS = 2 * 1000;

    /** Gets the password from net/vleu/par/gateway/secrets.properties */
    private static String readC2dmAuthToken() throws MissingResourceException {
        return ResourceBundle.getBundle("net.vleu.par.gateway.secrets")
                .getString("C2DM_AUTH_TOKEN");
    }

    /**
     * The GAE datastores where to get the {@link DeviceEntity}. They have to be
     * thread-local because the {@link DatastoreService} are not thread-safe.
     */
    private final ThreadLocal<DatastoreService> datastores;

    /** Used to form the requests to Google C2DM */
    private final C2dmRequestFactory requestFactory;

    private final Queue taskQueue;

    /** Used to perform the requests to Google C2DM */
    final ThreadLocal<URLFetchService> urlFetchService;

    public DeviceWaker() {
        this(ThreadLocalDatastoreService.getSingleton(),
                new C2dmRequestFactory(readC2dmAuthToken()), QueueFactory
                        .getQueue(DeviceWakerServlet.APPENGINE_QUEUE_NAME),
                new ThreadLocal<URLFetchService>() {
                    @Override
                    protected URLFetchService initialValue() {
                        return URLFetchServiceFactory.getURLFetchService();
                    }
                });
    }

    /** Allows for injecting the private fields, for testing purposes */
    DeviceWaker(final ThreadLocal<DatastoreService> datastoreService,
            final C2dmRequestFactory requestFactory, final Queue taskQueue,
            final ThreadLocal<URLFetchService> urlFetchService) {
        this.datastores = datastoreService;
        this.requestFactory = requestFactory;
        this.taskQueue = taskQueue;
        this.urlFetchService = urlFetchService;
    }

    public boolean queueWake(final UserId ownerId, final DeviceId deviceId) {
        final int MAX_RETRIES = 10;
        final TaskOptions options = TaskOptions.Builder.withDefaults();
        int retried = 0;
        boolean done = false;
        options.header(DeviceWakerServlet.DEVICE_ID_HTTP_PARAM,
                deviceId.toBase64url());
        options.header(DeviceWakerServlet.USER_ID_HTTP_PARAM,
                ownerId.asString());
        options.countdownMillis(WAKE_DELAY_MILLIS);
        options.taskName(deviceId.toBase64url());

        do
            try {
                this.taskQueue.add(options);
                done = true;
            }
            catch (final TaskAlreadyExistsException e) {
                /* Ignored, the device will be awoken */
                done = true;
            }
            catch (final TransientFailureException e) {
                retried++;
                LOG.warning("TransientFailureException while enqueueing a task: "
                    + e.toString() + " Retried " + retried + " times");
            }
        while (!done && retried < MAX_RETRIES);
        if (!done)
            LOG.severe("Failed enqueueing a task. Retried " + retried
                + " times");
        return done;
    }

    /**
     * Calls Google's C2DM platform for the device whose ID is specified. This
     * method is normally called from the {@link DeviceWakerServlet} during the
     * execution of a task queued by {@link #queueWake(UserId, DeviceId)}
     * 
     * @param ownerId
     *            The user who registered the device
     * @param deviceId
     *            The device to wake up.
     * @throws EntityNotFoundException
     */
    void reallyWake(final UserId ownerId, final DeviceId deviceId)
            throws IOException, EntityNotFoundException {
        final Key deviceKey = DeviceEntity.keyForIds(ownerId, deviceId);
        final Entity deviceEntity = this.datastores.get().get(null, deviceKey);
        final Device device = DeviceEntity.deviceFromEntity(deviceEntity);
        final String c2dmRegistrationId = device.getC2dmRegistrationId();
        final HTTPRequest request =
                this.requestFactory.buildRequest(c2dmRegistrationId);
        final HTTPResponse response = this.urlFetchService.get().fetch(request);
        if (response.getResponseCode() != 200)
            throw new IOException("The C2DM server anwsered a "
                + response.getResponseCode() + " error.");
        System.out.println("Response code: " + response.getResponseCode());
    }
}
