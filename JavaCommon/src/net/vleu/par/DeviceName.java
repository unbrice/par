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
package net.vleu.par;

/**
 * Typesafe wrapper for the names that users choose for their device
 */
public final class DeviceName extends WrappedString {

    /**
     * @param value
     *            Will be trimed and become the wrapped string
     */
    public DeviceName(final String value) {
        super(value.trim());
    }

    /**
     * @return true if the wrapped value's length is greater than 0
     */
    public boolean isValid() {
        return this.value.length() > 0;
    }

}
