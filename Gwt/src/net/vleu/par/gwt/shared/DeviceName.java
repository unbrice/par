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
 * Type-safe wrapper for a device's friendly name
 */
public final class DeviceName extends WrappedString implements
        Comparable<DeviceName> {

    /**
     * @param value A user-friendly name
     */
    public DeviceName(final String value) {
        super(value);
    }

    @Override
    public int compareTo(final DeviceName other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public boolean equals(final Object other) {
        if (other instanceof DeviceName)
            return super.equals((DeviceName) other);
        else
            return false;
    }
}
