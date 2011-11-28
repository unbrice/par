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
package net.vleu.par.gwt.shared;

/**
 * This class can inherited by classes that wraps a {@link String} so as to
 * prevent taking a string for another.
 */
public abstract class WrappedString {
    /** The value wrapped by this class */
    public final String value;

    protected WrappedString(final String value) {
        if (value == null)
            throw new NullPointerException(
                    "Trying to initialize a Wrapped String with a NULL pointer");
        this.value = value;
    }

    @Override
    public boolean equals(final Object other) {
        if (this.getClass().isInstance(other))
            return ((WrappedString) other).value.equals(this.value);
        else
            return false;
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    @Override
    public String toString() {
        return this.value;
    }

}
