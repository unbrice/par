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
 * This POJO represents a {@link DeviceId} along with a friendly user name
 */
public class Device implements Comparable<Device> {
    public final DeviceId deviceId;
    public final DeviceName deviceName;

    public Device(final DeviceId deviceId, final DeviceName deviceName) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
    }

    @Override
    public int compareTo(final Device other) {
        final int res = this.deviceName.compareTo(other.deviceName);
        if (res != 0)
            return res;
        return this.deviceId.compareTo(other.deviceId);
    }

    @Override
    public boolean equals(final Object otherObj) {
        if (otherObj instanceof Device) {
            final Device other = (Device) otherObj;
            return this.deviceId.equals(other.deviceId)
                && this.deviceName.equals(other.deviceName);
        }
        else
            return false;
    }

    @Override
    public int hashCode() {
        return (this.deviceId.hashCode() * 33 + this.deviceName.hashCode())
            % Integer.MAX_VALUE;
    }
}
