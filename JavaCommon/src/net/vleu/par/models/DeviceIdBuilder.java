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
package net.vleu.par.models;

import net.vleu.par.protocolbuffer.Devices.DeviceIdBuilderData;
import biz.source_code.base64Coder.Base64UrlCoder;

import com.google.protobuf.InvalidProtocolBufferException;

public final class DeviceIdBuilder {
    @SuppressWarnings("serial")
    public static class InvalidDeviceIdSerialisation extends Exception {
        private InvalidDeviceIdSerialisation(final String message,
                final Exception cause) {
            super(message, cause);
        }
    }

    /**
     * Decodes a DeviceId into a protocol buffer
     * 
     * @param deviceId The DeviceId to decode
     * @return A protocol buffer representing the DeviceI
     * @throws InvalidDeviceIdSerialisation
     *             If the PB is not as described in the .proto file.
     */
    public static DeviceIdBuilderData deviceIdToProto(final DeviceId deviceId)
            throws InvalidDeviceIdSerialisation {
        String deviceIdStr = deviceId.value;
        final byte[] bytes = Base64UrlCoder.decode(deviceIdStr);
        DeviceIdBuilderData proto;
        try {
            proto = DeviceIdBuilderData.parseFrom(bytes);
        }
        catch (final InvalidProtocolBufferException e) {
            throw new InvalidDeviceIdSerialisation(deviceIdStr, e);
        }
        return proto;
    }

    /**
     * Encodes a protocol buffer into a DeviceId 
     * 
     * @param proto
     *            The protocol buffer, must be valid, it won't be verified.
     * @return A string that represent this protocol buffer, with no padding
     */
    public static DeviceId fromProtocolBufferToDeviceId(
            final DeviceIdBuilderData proto) {
        final byte[] bytes = proto.toByteArray();
        final char[] base64urlChars = Base64UrlCoder.encode(bytes);
        return new DeviceId(new String(base64urlChars));
    }

    /**
     * Checks that one and only one of the possible IDs is set.
     * 
     * @param proto
     *            The suspicious protocolbuffer.
     * @return True if the protocol buffer has one and only one set field.
     */
    public static boolean
            isValidProtocolBuffer(final DeviceIdBuilderData proto) {
        int count = 0;
        if (proto.hasAndroidId())
            count++;
        if (proto.hasTelephonyEmei())
            count++;
        if (proto.hasTelephonyMeid())
            count++;
        if (proto.hasWifiMac())
            count++;
        return count == 1;
    }

    private DeviceIdBuilder() {
    }
}
