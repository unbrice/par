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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ConcurrentModificationException;
import java.util.logging.Logger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Transaction;

@RunWith(MockitoJUnitRunner.class)
public class TransactionHelperTest {

    /**
     * A {@link TransactionHelper} with a counter to keep track of calls to
     * {@link #doInsideTransaction(DatastoreService, Transaction)}.
     */
    private final class CountingTransactionHelper extends TransactionHelper {

        public int timesCalled = 0;

        /**
         * If timesCalled is less than this,
         * {@link #doInsideTransaction(DatastoreService, Transaction)} will
         * throw a {@link ConcurrentModificationException}
         */
        public final int timesCalledBeforeSuccess;

        public CountingTransactionHelper() {
            this(0);
        }

        public CountingTransactionHelper(final int timesCalledBeforeSuccess) {
            super(TransactionHelperTest.this.datastore,
                    TransactionHelperTest.this.logger, "test");
            this.timesCalledBeforeSuccess = timesCalledBeforeSuccess;
        }

        /**
         * @param datastore
         *            Ignored
         * @param transaction
         *            Ignored
         * @throws ConcurrentModificationException
         *             If {@link #timesCalled} < {@link #timesCalledBeforeSuccess}
         */
        @Override
        protected void doInsideTransaction(final DatastoreService datastore,
                final Transaction transaction)
                throws ConcurrentModificationException {
            this.timesCalled++;
            if (this.timesCalled < this.timesCalledBeforeSuccess)
                throw new ConcurrentModificationException(
                        "Failing for test purposes");
        }

    }

    @Mock
    DatastoreService datastore;
    @Mock
    Transaction defaultTransaction;
    @Mock
    Logger logger;

    @Test
    public void testFailingCallsAreRetried() throws TooManyConcurrentAccesses {
        final int failuresNumber = 42;
        when(this.datastore.beginTransaction()).thenReturn(
                this.defaultTransaction);
        when(this.defaultTransaction.isActive()).thenReturn(true);

        final CountingTransactionHelper tested =
                new CountingTransactionHelper(failuresNumber);
        tested.call();
        assertEquals(failuresNumber, tested.timesCalled);
        verify(this.defaultTransaction, times(failuresNumber)).rollback();
        verify(this.defaultTransaction).commit();
    }

    @Test
    public void testOrdinaryCallsAreNotRetried()
            throws TooManyConcurrentAccesses {
        when(this.datastore.beginTransaction()).thenReturn(
                this.defaultTransaction);
        final CountingTransactionHelper tested =
                new CountingTransactionHelper();
        tested.call();
        assertEquals(1, tested.timesCalled);
        verify(this.defaultTransaction).commit();
    }

    @Test(expected = TooManyConcurrentAccesses.class)
    public void throwsAfterTooManyFailures() throws TooManyConcurrentAccesses {
        when(this.datastore.beginTransaction()).thenReturn(
                this.defaultTransaction);
        final CountingTransactionHelper tested =
                new CountingTransactionHelper(Integer.MAX_VALUE);
        tested.call();
    }
}
