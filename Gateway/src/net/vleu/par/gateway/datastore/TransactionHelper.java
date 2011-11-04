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

import java.util.ConcurrentModificationException;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Transaction;

/**
 * This class helps one to use GAE Transactions by implementing the "retry upon
 * concurrent modifications" pattern described in <a href=
 * "http://code.google.com/appengine/docs/java/datastore/transactions.html#Uses_for_Transactions"
 * > Google AppEngine's documentation </a>.
 */
public abstract class TransactionHelper implements Callable<Void> {
    /**
     * After failing to commit a single transaction this number of time, we'll
     * abort. The current value is quite random.
     */
    private static final int MAX_COMMIT_RETRIES = 128;
    private final DatastoreService datastore;
    /** A string used as a prefix when logging */
    private final String description;
    private final Logger logger;

    /**
     * Builds a new TransactionHelper.
     * 
     * @param datastore
     *            The datastore on which the changes will be performed
     * @param logger
     *            Used to report failures
     * @param description
     *            A string used as a prefix when logging
     */
    public TransactionHelper(final DatastoreService datastore,
            final Logger logger, final String description) {
        this.datastore = datastore;
        this.description = description;
        this.logger = logger;
    }

    /**
     * Calls {@link #doInsideTransaction(DatastoreService, Transaction)},
     * retrying in case of failure, thus implementing the "retry upon concurrent
     * modifications" pattern described in <a href=
     * "http://code.google.com/appengine/docs/java/datastore/transactions.html#Uses_for_Transactions"
     * > Google AppEngine's documentation </a>
     */
    @Override
    public final Void call() throws TooManyConcurrentAccesses {
        boolean commited = false;
        int commitRetries = 0;
        do {
            final Transaction txn = this.datastore.beginTransaction();
            try {
                doInsideTransaction(this.datastore, txn);
                txn.commit();
                commited = true;
            }
            catch (final ConcurrentModificationException e) {
                final String message =
                        this.description + ": Commit failed. Retried "
                            + commitRetries + " times.";
                this.logger.fine(message + ":" + e.toString());
                commitRetries++;
                if (commitRetries > MAX_COMMIT_RETRIES)
                    throw new TooManyConcurrentAccesses(message, e);
            }
            finally {
                if (txn.isActive())
                    txn.rollback();
            }
        } while (!commited);
        return null;
    }

    /**
     * Override this to modify data stored in the datastore. If it throws a
     * {@link ConcurrentModificationException}, the transaction will be rolled
     * back and this function will be called again.
     * 
     * <b>Bear in mind that this method will be called at least once, but
     * possibly many times.</b>
     * 
     * @param datastore
     *            The datastore provided to the constructor
     * @param transaction
     *            This transaction will be rolled back in case of retries
     * @throws ConcurrentModificationException
     *             If thrown, we'll retry
     */
    protected abstract void doInsideTransaction(DatastoreService datastore,
            Transaction transaction) throws ConcurrentModificationException;
}
