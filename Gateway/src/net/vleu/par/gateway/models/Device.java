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
    /** As per {@link com.google.appengine.api.users.User#getUserId()} */
    public final String owner;

    public Device(final DeviceId id, final String c2dmRegistrationId,
            final String owner) {
        this.id = id;
        this.c2dmRegistrationId = c2dmRegistrationId;
        this.owner = owner;
    }

    public String getC2dmRegistrationId() {
        return this.c2dmRegistrationId;
    }

    public DeviceId getId() {
        return this.id;
    }

    /**
     * @return the User Id as per
     *         {@link com.google.appengine.api.users.User#getUserId()}
     */
    public String getOwner() {
        return this.owner;
    }

    public void setC2dmRegistrationId(final String c2dmRegistrationId) {
        this.c2dmRegistrationId = c2dmRegistrationId;
    }
}
