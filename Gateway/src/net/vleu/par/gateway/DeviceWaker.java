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

import net.vleu.par.gateway.datastore.DeviceEntity;
import net.vleu.par.gateway.models.Device;
import net.vleu.par.gateway.models.DeviceId;
import net.vleu.par.gateway.models.UserId;
import net.vleu.par.utils.C2dmRequestFactory;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;

public class DeviceWaker {

    /** Gets the password from net/vleu/par/gateway/secrets.properties */
    private static String readC2dmAuthToken() throws MissingResourceException {
        return ResourceBundle.getBundle("net.vleu.par.gateway.secrets")
                .getString("C2DM_AUTH_TOKEN");
    }

    /** The datastore from which to get {@link DeviceEntity} */
    private final DatastoreService datastore;

    /** Used to form the requests to Google C2DM */
    private final C2dmRequestFactory requestFactory;

    /** Used to perform the requests to Google C2DM */
    private final URLFetchService urlFetchService;

    public DeviceWaker() {
        this(DatastoreServiceFactory.getDatastoreService(),
                new C2dmRequestFactory(readC2dmAuthToken()),
                URLFetchServiceFactory.getURLFetchService());
    }

    /** Allows for injecting the private fields, for testing purposes */
    DeviceWaker(final DatastoreService datastore,
            final C2dmRequestFactory requestFactory,
            final URLFetchService urlFetchService) {
        this.datastore = datastore;
        this.requestFactory = requestFactory;
        this.urlFetchService = urlFetchService;
    }

    /**
     * Calls Google's C2DM platform for the device whose ID is specified.
     * 
     * @param deviceId
     *            The device to wake up.
     * @throws EntityNotFoundException
     */
    public void wake(final UserId ownerId, final DeviceId deviceId)
            throws IOException, EntityNotFoundException {
        final Key deviceKey = DeviceEntity.keyForIds(ownerId, deviceId);
        final Entity deviceEntity = this.datastore.get(deviceKey);
        final Device device = DeviceEntity.deviceFromEntity(deviceEntity);
        final String c2dmRegistrationId = device.getC2dmRegistrationId();
        final HTTPRequest request =
                this.requestFactory.buildRequest(c2dmRegistrationId);
        final HTTPResponse response = this.urlFetchService.fetch(request);
        if (response.getResponseCode() != 200)
            throw new IOException("The C2DM server anwsered a "
                + response.getResponseCode() + " error.");
    }
}
