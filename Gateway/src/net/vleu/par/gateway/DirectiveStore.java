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

import java.util.ConcurrentModificationException;
import java.util.logging.Logger;

import net.jcip.annotations.ThreadSafe;
import net.vleu.par.gateway.datastore.DeviceEntity;
import net.vleu.par.gateway.datastore.DirectiveEntity;
import net.vleu.par.gateway.datastore.TooManyConcurrentAccesses;
import net.vleu.par.gateway.models.DeviceId;
import net.vleu.par.gateway.models.Directive;
import net.vleu.par.gateway.models.UserId;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Transaction;

@ThreadSafe
public final class DirectiveStore {
    /**
     * The GAE datastore where to get the {@link DeviceEntity}. They have to be
     * local because the {@link DatastoreService} are not thread-safe.
     */
    static final InjectableThreadLocal<DatastoreService> DATASTORES =
            new InjectableThreadLocal<DatastoreService>() {
                @Override
                protected DatastoreService instantiateValue() {
                    return DatastoreServiceFactory.getDatastoreService();
                }
            };
    private static final Logger LOG = Logger.getLogger(DirectiveStore.class
            .getName());

    /**
     * After failing to commit a single transaction this number of time, we'll
     * abort. The current value is quite random.
     */
    private static final int MAX_COMMIT_RETRIES = 128;

    public void store(final UserId ownerId, final DeviceId deviceId,
            final Directive directive) throws TooManyConcurrentAccesses {
        final Entity asEntity =
                DirectiveEntity.entityFromDirective(ownerId, directive);
        boolean commited = false;
        int commitRetries = 0;
        do {
            final Transaction txn = DATASTORES.get().beginTransaction();
            try {
                DATASTORES.get().put(txn, asEntity);
                txn.commit();
                commited = true;
            }
            catch (final ConcurrentModificationException e) {
                final String message =
                        getClass().getCanonicalName()
                            + ".store(): Commit failed. Retried "
                            + commitRetries + " times.";
                LOG.fine(message + ":" + e.toString());
                commitRetries++;
                if (commitRetries > MAX_COMMIT_RETRIES)
                    throw new TooManyConcurrentAccesses(message, e);
            }
            finally {
                if (txn.isActive())
                    txn.rollback();
            }
        } while (!commited);
    }

}
