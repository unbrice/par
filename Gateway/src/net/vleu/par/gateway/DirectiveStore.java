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
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.logging.Logger;

import net.jcip.annotations.ThreadSafe;
import net.vleu.par.gateway.datastore.DeviceEntity;
import net.vleu.par.gateway.datastore.DirectiveEntity;
import net.vleu.par.gateway.datastore.ThreadLocalDatastoreService;
import net.vleu.par.gateway.datastore.TooManyConcurrentAccesses;
import net.vleu.par.gateway.datastore.TransactionHelper;
import net.vleu.par.gateway.models.DeviceId;
import net.vleu.par.gateway.models.Directive;
import net.vleu.par.gateway.models.UserId;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;

@ThreadSafe
public class DirectiveStore {
    private static final FetchOptions FETCH_ALL_OPTIONS = withChunkSize(
            Integer.MAX_VALUE).prefetchSize(Integer.MAX_VALUE);

    private static final Logger LOG = Logger.getLogger(DirectiveStore.class
            .getName());

    /**
     * The GAE datastore where to get the {@link DeviceEntity}. They have to be
     * thread-local because the {@link DatastoreService} are not thread-safe.
     */
    private final ThreadLocal<DatastoreService> datastores;

    public DirectiveStore() {
        this(ThreadLocalDatastoreService.getSingleton());
    }

    /**
     * Allows dependency injection, for testing purposes.
     * 
     * @param datastores
     *            ThreadGlobal datastores.
     */
    DirectiveStore(final ThreadLocal<DatastoreService> datastores) {
        this.datastores = datastores;
    }

    public ArrayList<Directive> fetchAndDelete(final UserId ownerId,
            final DeviceId deviceId) throws TooManyConcurrentAccesses {
        final String methodName =
                getClass().getCanonicalName() + ".fetchAndDelete()";
        final ArrayList<Directive> result = new ArrayList<Directive>();
        final Query query =
                DirectiveEntity
                        .buildQueryForQueuedDirectives(ownerId, deviceId);
        new TransactionHelper(this.datastores.get(), LOG, methodName) {
            @Override
            protected void doInsideTransaction(
                    final DatastoreService datastore, final Transaction txn)
                    throws ConcurrentModificationException {
                final List<Entity> queryResult;
                result.clear();
                /* Lists the directives */
                queryResult =
                        datastore.prepare(txn, query).asList(FETCH_ALL_OPTIONS);
                for (final Entity entity : queryResult)
                    try {
                        result.add(DirectiveEntity.directiveFromEntity(entity));
                    }
                    catch (final Exception e) {
                        LOG.severe("An invalid Directive has been found in the datastore ! "
                            + e);
                        /*
                         * The only thing we can do with an invalid directive is
                         * to ignore it. It will be deleted with the valid
                         * directives
                         */
                    }
                /* Deletes the fetched entities */
                for (final Entity entity : queryResult)
                    datastore.delete(entity.getKey());
            }
        }.call();
        return result;
    }

    public void store(final UserId ownerId, final DeviceId deviceId,
            final Directive directive) throws TooManyConcurrentAccesses {
        final String methodName = getClass().getCanonicalName() + ".store()";
        final Entity asEntity =
                DirectiveEntity.entityFromDirective(ownerId, deviceId,
                        directive);
        new TransactionHelper(this.datastores.get(), LOG, methodName) {
            @Override
            protected void doInsideTransaction(
                    final DatastoreService datastore, final Transaction txn)
                    throws ConcurrentModificationException {
                datastore.put(txn, asEntity);
            }
        }.call();
    }
}
