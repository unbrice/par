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
package net.vleu.par.gateway.models;

public final class Device {
    /** Access token for using C2DM with this device */
    private String c2dmRegistrationId;
    private final DeviceId id;

    /**
     * @param id
     *            Cannot be null
     */
    public Device(final DeviceId id) {
        this(id, null);
    }

    /**
     * @param id
     *            Cannot be null
     * @param c2dmRegistrationId
     *            Null if the device is not registered
     */
    public Device(final DeviceId id, final String c2dmRegistrationId) {
        this.id = id;
        this.c2dmRegistrationId = c2dmRegistrationId;
    }

    @Override
    public boolean equals(final Object other) {
        if ((other == null) || !(other instanceof Device))
            return false;
        else {
            final Device otherAsDevice = (Device) other;
            return this.id.equals(otherAsDevice.id)
                && this.c2dmRegistrationId
                        .equals(otherAsDevice.c2dmRegistrationId);
        }
    }

    /** @return access token for using C2DM with this device */
    public String getC2dmRegistrationId() {
        if (!hasC2dmRegistrationId())
            throw new IllegalStateException("No C2DM Registration ID");
        else
            return this.c2dmRegistrationId;
    }

    public DeviceId getId() {
        return this.id;
    }

    public boolean hasC2dmRegistrationId() {
        return (this.c2dmRegistrationId != null)
            && (this.c2dmRegistrationId.length() > 0);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    /**
     * Sets the access token for using C2DM with this device
     * 
     * @param c2dmRegistrationId
     *            Access token for using C2DM with this device, or null
     */
    protected void setC2dmRegistrationId(final String c2dmRegistrationId) {
        this.c2dmRegistrationId = c2dmRegistrationId;
    }
}
