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
package net.vleu.par.gwt.client;

import net.vleu.par.gwt.shared.DeviceId;

import com.google.gwt.place.shared.Place;

/**
 * Base abstract class for places which includes a {@link DeviceIdData} as part
 * of their state. It has helpers allowing to (un)serialize this state
 */
public abstract class PlaceWithDevice extends Place {
    /**
     * Can be null
     */
    private final DeviceId deviceId;

    protected PlaceWithDevice(final DeviceId device) {
        this.deviceId = device;
    }

    /**
     * Instances of {@link Place} are expected to implement this
     */
    @Override
    abstract public boolean equals(Object other);

    /**
     * @return True if the {@link PlaceWithDevice}'s fields are equal in both
     *         objects
     */
    protected boolean equals(final PlaceWithDevice other) {
        return other.deviceId.equals(this.deviceId);
    }

    public DeviceId getDeviceId() {
        return this.deviceId;
    }

    /**
     * @return False if {@link #getDeviceId()} would have returned null, else
     *         true
     */
    public boolean hasDeviceId() {
        return this.deviceId != null;
    }

    /**
     * @return A hashcode for {@link PlaceWithDevice}'s fields
     */
    @Override
    public int hashCode() {
        return this.deviceId.hashCode();
    }
}
