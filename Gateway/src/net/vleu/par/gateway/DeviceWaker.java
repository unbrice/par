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
import java.util.logging.Logger;

import net.jcip.annotations.ThreadSafe;
import net.vleu.par.C2dmToken;
import net.vleu.par.ClientLoginToken;
import net.vleu.par.gateway.datastore.DeviceEntity;
import net.vleu.par.gateway.datastore.ThreadLocalDatastoreService;
import net.vleu.par.models.Device;
import net.vleu.par.models.DeviceId;
import net.vleu.par.models.UserId;
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
import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;

@ThreadSafe
public class DeviceWaker {

    /**
     * Thrown by
     * {@link DeviceWaker#reallyWake(ClientLoginToken, UserId, DeviceId)} when
     * the authentication token is refused by the C2DM servers.
     * 
     * This might be the sign that a local copy of this token is no longer fresh
     * enough
     */
    @SuppressWarnings("serial")
    @ThreadSafe
    public static class InvalidC2dmClientLoginToken extends Exception {
    }

    /**
     * If this header is set in an answer to a request to C2DM's server, the
     * associated value will become the new {@link ClientLoginToken}
     * 
     * @see DeviceWaker#readNewAuthTokenFromC2dmResponse(HTTPResponse)
     */
    private static final String C2DM_UPDATE_AUTH_HEADER = "Update-Client-Auth";

    private static final Logger LOG = Logger.getLogger(DeviceWaker.class
            .getName());

    /**
     * We will aggregate all wake requests during this delay, so as to help
     * preventing C2DM flood if the user is flooding the gateway
     */
    private static final long WAKE_DELAY_MILLIS = 2 * 1000;

    /**
     * TODO: The current task name are created using the time as a simple way to
     * prevent redundant wake ups, using the fact that
     * {@link #WAKE_DELAY_MILLIS} ms are required before the task to fire. A
     * better way of doing this might be to use memcache
     * 
     * @param deviceId
     *            The device that will be woken up
     * @return A task name
     */
    private static String buildTaskName(final DeviceId deviceId) {
        return Long.toHexString(System.currentTimeMillis() / WAKE_DELAY_MILLIS)
            + '_' + deviceId.value;
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
                new C2dmRequestFactory(), QueueFactory
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
        options.param(DeviceWakerServlet.DEVICE_ID_HTTP_PARAM, deviceId.value);
        options.param(DeviceWakerServlet.USER_ID_HTTP_PARAM, ownerId.asString());
        options.countdownMillis(WAKE_DELAY_MILLIS);
        options.taskName(buildTaskName(deviceId));

        do
            try {
                this.taskQueue.add(options);
                done = true;
            }
            catch (final TaskAlreadyExistsException e) {
                /* Ignored, the device will be awoken */
                done = true;
                LOG.warning("Queueing done (existed)");
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
     * Helper function for
     * {@link #reallyWake(ClientLoginToken, UserId, DeviceId)}. It searches for
     * a header called {@value #C2DM_UPDATE_AUTH_HEADER} in the response and
     * returns its value as a {@link ClientLoginToken}.
     * 
     * @param response
     *            C2DM's server response
     * @return The new {@link ClientLoginToken}, null if unchanged
     */
    private ClientLoginToken readNewAuthTokenFromC2dmResponse(
            final HTTPResponse response) {
        for (final HTTPHeader header : response.getHeaders())
            if (C2DM_UPDATE_AUTH_HEADER.equals(header.getName())) {
                final ClientLoginToken res =
                        new ClientLoginToken(header.getValue());
                LOG.fine("Got updated auth token from datamessaging servers: "
                    + res);
                return res;
            }
        return null;
    }

    /**
     * Calls Google's C2DM platform for the device whose ID is specified. This
     * method is normally called from the {@link DeviceWakerServlet} during the
     * execution of a task queued by {@link #queueWake(UserId, DeviceId)}
     * 
     * @param c2dmAuthToken
     *            Where to find the client login token for authenticating with
     *            Google C2DM servers
     * @param ownerId
     *            The user who registered the device
     * @param deviceId
     *            The device to wake up.
     * @return A new {@link ClientLoginToken} to use in subsequent requests, or
     *         null
     * @throws EntityNotFoundException
     *             When the deviceId or userId are unknown
     * @throws InvalidC2dmClientLoginToken
     *             When the C2DM {@link ClientLoginToken} (c2dmAuthToken) is
     *             invalid and needs to be refreshed
     */
    ClientLoginToken reallyWake(final ClientLoginToken c2dmAuthToken,
            final UserId ownerId, final DeviceId deviceId) throws IOException,
            EntityNotFoundException, InvalidC2dmClientLoginToken {
        final Key deviceKey = DeviceEntity.keyForIds(ownerId, deviceId);
        final Entity deviceEntity = this.datastores.get().get(null, deviceKey);
        final Device device = DeviceEntity.deviceFromEntity(deviceEntity);
        if (device.hasC2dmRegistrationId()) {
            LOG.severe("Waking up the device using id: "
                + device.getC2dmRegistrationId());
            final C2dmToken c2dmRegistrationId = device.getC2dmRegistrationId();
            final HTTPRequest request =
                    this.requestFactory.buildRequest(c2dmAuthToken,
                            c2dmRegistrationId);
            final HTTPResponse response =
                    this.urlFetchService.get().fetch(request);
            if (response.getResponseCode() == 200)
                return readNewAuthTokenFromC2dmResponse(response);
            else if (response.getResponseCode() == 401)
                throw new InvalidC2dmClientLoginToken();
            else
                throw new IOException("The C2DM server anwsered a "
                    + response.getResponseCode() + " error.");
        }
        else {
            LOG.finest("Won't wake the device because it is not registered with C2DM");
            return null;
        }
    }
}
