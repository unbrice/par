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
    private long c2dmDelay;
    private long c2dmDelayReset;
    private String c2dmRegistrationId;
    private final DeviceId id;
    private String sharedSecret;

    public Device(final DeviceId id, final long c2dmDelay,
            final long c2dmDelayReset, final String c2dmRegistrationId,
            final String sharedSecret) {
        this.id = id;
        this.c2dmDelay = c2dmDelay;
        this.c2dmDelayReset = c2dmDelayReset;
        this.c2dmRegistrationId = c2dmRegistrationId;
        this.sharedSecret = sharedSecret;
    }

    public long getC2dmDelay() {
        return this.c2dmDelay;
    }

    public long getC2dmDelayReset() {
        return this.c2dmDelayReset;
    }

    public String getC2dmRegistrationId() {
        return this.c2dmRegistrationId;
    }

    public DeviceId getId() {
        return this.id;
    }

    public String getSharedSecret() {
        return this.sharedSecret;
    }

    public void setC2dmDelay(final long c2dmDelay) {
        this.c2dmDelay = c2dmDelay;
    }

    public void setC2dmDelayReset(final long c2dmDelayReset) {
        this.c2dmDelayReset = c2dmDelayReset;
    }

    public void setC2dmRegistrationId(final String c2dmRegistrationId) {
        this.c2dmRegistrationId = c2dmRegistrationId;
    }

    public void setSharedSecret(final String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }
}
