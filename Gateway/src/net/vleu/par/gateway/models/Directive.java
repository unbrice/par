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

import net.vleu.par.gateway.models.DeviceId.InvalidDeviceIdSerialisation;

public final class Directive {
    private final net.vleu.par.protocolbuffer.Commands.Directive.Builder proto;

    public Directive(final net.vleu.par.protocolbuffer.Commands.Directive proto) {
        this.proto = proto.toBuilder();
    }
    
    public Directive() {
        this.proto = net.vleu.par.protocolbuffer.Commands.Directive.newBuilder();
    }

    /** @return The DeviceId, null if missing or unparseable */
    public DeviceId getDeviceId() {
        if (!this.proto.hasDeviceId())
            return null;
        try {
            return DeviceId.fromProtocolBuffer(this.proto.getDeviceId());
        }
        catch (final InvalidDeviceIdSerialisation _) {
            return null;
        }
    }
}