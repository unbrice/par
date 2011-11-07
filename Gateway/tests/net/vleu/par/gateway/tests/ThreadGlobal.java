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
package net.vleu.par.gateway.tests;

/**
 * An {@link ThreadLocal} that always give the same value to all threads.
 * This class is used for tests.
 */
public final class ThreadGlobal<T> extends ThreadLocal<T> {
    T globalValue;

    public ThreadGlobal(final T injected) {
        this.globalValue = injected;
    }

    /**
     * @return The object passed to the constructor
     */
    @Override
    @Deprecated
    protected T initialValue() {
        return this.globalValue;
    }
}