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

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

/**
 * This behaves just like a {@link ThreadLocal} excepts that it provides an
 * extra method: {@link #inject(Object)}. This method will set an object that
 * will be returned by all subsequents call to {@link #initialValue()}.
 * {@link #initialValue()} has been made final to avoid misuses. In order to
 * instantiate your thread-local object, override {@link #instantiateValue()}.
 */
@ThreadSafe
public abstract class InjectableThreadLocal<T> extends ThreadLocal<T> {

    /**
     * If not null, will be return by a call to {@link #initialValue()}
     */
    @GuardedBy("this")
    private T injected;

    @Override
    public synchronized final T initialValue() {
        if (this.injected != null)
            return this.injected;
        else
            return instantiateValue();
    }

    /**
     * Makes all subsequent calls to {@link #initialValue()} return the injected
     * object. This is exists to allow for dependency-injection.
     * 
     * @param injected
     *            The object to inject
     */
    public synchronized void inject(final T injected) {
        this.injected = injected;
    }

    /**
     * This is called when no values have been injected by
     * {@link #inject(Object)} to create new instances that will be return by
     * {@link #initialValue()}.
     * 
     * @return A value that will become the initial value for this thread-local
     *         if no other had been injected
     */
    abstract protected T instantiateValue();
}
