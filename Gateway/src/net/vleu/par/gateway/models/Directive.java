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

import net.vleu.par.protocolbuffer.Commands.DirectiveData;
import net.vleu.par.protocolbuffer.Devices.DeviceIdData;

import com.google.protobuf.InvalidProtocolBufferException;

public final class Directive {
    @SuppressWarnings("serial")
    public static class InvalidDirectiveSerialisation extends Exception {
        private final DeviceIdData invalidProto;

        private InvalidDirectiveSerialisation(final String message) {
            super(message);
            this.invalidProto = null;
        }

        private InvalidDirectiveSerialisation(final String message,
                final DeviceIdData invalidProto) {
            super(message);
            this.invalidProto = invalidProto;
        }

        private InvalidDirectiveSerialisation(final String message,
                final Exception cause) {
            super(message, cause);
            this.invalidProto = null;
        }

        /** @return The protocol buffer that triggered the exception, or null */
        public DeviceIdData getInvalidProtocolBuffer() {
            return this.invalidProto;
        }
    }

    public static Directive fromProtocolBuffer(final byte[] data)
            throws InvalidDirectiveSerialisation {
        Directive res;
        try {
            res = new Directive(DirectiveData.parseFrom(data));
        }
        catch (final InvalidProtocolBufferException e) {
            throw new InvalidDirectiveSerialisation(
                    "Protocol Buffer does not parse", e);
        }
        // TODO: Check it is valid
        return res;
    }

    private final DirectiveData proto;

    public Directive(final DirectiveData proto) {
        this.proto = proto;
    }

    public byte[] asProtocolBufferBytes() {
        return this.proto.toByteArray();
    }
}
