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

/**
 * Thrown when too many transaction conflicted and the auto-retry aborted.
 * 
 * @see <a href=
 *      "http://code.google.com/appengine/docs/java/datastore/transactions.html#Uses_for_Transactions"
 *      > the "Retry upon concurrentmodifications" pattern </a>
 * @see {@link TransactionHelper}
 */
@SuppressWarnings("serial")
public final class TooManyConcurrentAccesses extends Exception {
    /**
     * @see Exception#Exception(String)
     */
    public TooManyConcurrentAccesses(final String message) {
        super(message);
    }

    /**
     * @see Exception#Exception(String, Throwable)
     */
    public TooManyConcurrentAccesses(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * @see Exception#Exception(String, Throwable, boolean, boolean)
     */
    public TooManyConcurrentAccesses(final String message,
            final Throwable cause, final boolean enableSuppression,
            final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * @see Exception#Exception(Throwable)
     */
    public TooManyConcurrentAccesses(final Throwable cause) {
        super(cause);
    }
}
