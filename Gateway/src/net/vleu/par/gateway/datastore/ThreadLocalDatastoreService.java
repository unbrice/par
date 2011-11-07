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

import net.jcip.annotations.ThreadSafe;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;

/**
 * An {@link ThreadLocal} that always contains a {@link DatastoreService}. We
 * have to have a {@link DatastoreService} per thread because they are not
 * thread-safe. Use {@link #getSingleton()} to get an instance.
 */
@ThreadSafe
public final class ThreadLocalDatastoreService extends
        ThreadLocal<DatastoreService> {

    private static final ThreadLocalDatastoreService SINGLETON =
            new ThreadLocalDatastoreService();

    public static ThreadLocalDatastoreService getSingleton() {
        return SINGLETON;
    }

    private ThreadLocalDatastoreService() {
    }

    @Override
    protected synchronized DatastoreService initialValue() {
        return DatastoreServiceFactory.getDatastoreService();
    }
}
