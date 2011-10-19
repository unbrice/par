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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import biz.source_code.base64Coder.Base64UrlCoder;

import com.google.protobuf.InvalidProtocolBufferException;

public final class DeviceId implements Comparable<DeviceId> {
    @SuppressWarnings("serial")
    public static class InvalidDeviceIdSerialisation extends Exception {
        private final net.vleu.par.protocolbuffer.Devices.DeviceId invalidProto;

        private InvalidDeviceIdSerialisation(final String message) {
            super(message);
            this.invalidProto = null;
        }

        private InvalidDeviceIdSerialisation(final String message,
                final Exception cause) {
            super(message, cause);
            this.invalidProto = null;
        }

        private InvalidDeviceIdSerialisation(final String message,
                final net.vleu.par.protocolbuffer.Devices.DeviceId invalidProto) {
            super(message);
            this.invalidProto = invalidProto;
        }

        /** @return The protocol buffer that triggered the exception, or null */
        public net.vleu.par.protocolbuffer.Devices.DeviceId
                getInvalidProtocolBuffer() {
            return this.invalidProto;
        }
    }

    private final static Pattern BASE64URL_WHITELIST = Pattern
            .compile("[a-zA-Z0-9\\-_]*");

    /**
     * Does the reverse of {@link #toBase64url()}
     * 
     * @param base64Str
     *            the ID in Base64, URL variant with no padding, as per RFC4648.
     * @return An opaque object representing the DeviceId
     * @throws InvalidDeviceIdSerialisation If the PB is not valid, as described in the .proto file.
     */
    public static DeviceId fromBase64url(final String base64Str)
            throws InvalidDeviceIdSerialisation {
        final Matcher matcher = BASE64URL_WHITELIST.matcher(base64Str);
        if (!matcher.matches())
            throw new InvalidDeviceIdSerialisation("Invalid base64url:" + base64Str);
        final byte[] bytes = Base64UrlCoder.decode(base64Str);
        net.vleu.par.protocolbuffer.Devices.DeviceId proto;
        try {
            proto =
                    net.vleu.par.protocolbuffer.Devices.DeviceId
                            .parseFrom(bytes);
        }
        catch (final InvalidProtocolBufferException e) {
            throw new InvalidDeviceIdSerialisation(base64Str, e);
        }
        return fromProtocolBuffer(proto);
    }

    /**
     * Builds a DeviceId after checking that the protocol buffer is indeed valid, as described
     * in the .proto file.
     * @param proto The PB that will be checked and used
     * @return An opaque object representing the DeviceId
     * @throws InvalidDeviceIdSerialisation If the PB is not valid, as described in the .proto file.
     */
    public static DeviceId fromProtocolBuffer(
            final net.vleu.par.protocolbuffer.Devices.DeviceId proto) throws InvalidDeviceIdSerialisation {
        if (!isValidProtocolBuffer(proto))
            throw new InvalidDeviceIdSerialisation("Rejected by DeviceId.isValidProtocolBuffer", proto);
        else
            return new DeviceId(proto);
    }
    
    public static DeviceId fromBase64urlWithNoVerifications(final String base64str) {
        return new DeviceId(base64str);
    }

    /**
     * Used by the constructor
     * 
     * @param proto
     *            The protocol buffer, must be valid, it won't be verified.
     * @return A string that represent this protocol buffer, with no padding
     */
    private static String fromProtocolBufferToBase64url(
            final net.vleu.par.protocolbuffer.Devices.DeviceId proto) {
        final byte[] bytes = proto.toByteArray();
        final char[] base64urlChars = Base64UrlCoder.encode(bytes);
        return new String(base64urlChars);
    }

    /**
     * Checks that one and only one of the possible IDs is set.
     * 
     * @param proto
     *            The suspicious protocolbuffer.
     * @return True if the protocol buffer has one and only one set field.
     */
    public static boolean isValidProtocolBuffer(
            final net.vleu.par.protocolbuffer.Devices.DeviceId proto) {
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

    /**
     * This String has been interned as per {@link String#intern()}. It is
     * encoded in base64, URL variant with no padding, as per RFC4648
     */
    private final String asBase64url;

    private DeviceId(final net.vleu.par.protocolbuffer.Devices.DeviceId proto) {
        this(fromProtocolBufferToBase64url(proto));
    }

    private DeviceId(final String asBase64url) {
        this.asBase64url = asBase64url.intern();
    }

    @Override
    public int compareTo(final DeviceId other) {
        return this.asBase64url.compareTo(other.asBase64url);
    }

    @Override
    public boolean equals(final Object other) {
        if ((other == null) || !(other instanceof DeviceId))
            return false;
        else {
            final DeviceId otherAsDevice = (DeviceId) other;
            /* This works because both strings have been interned */
            return this.asBase64url == otherAsDevice.asBase64url;
        }
    }

    @Override
    public int hashCode() {
        /*
         * In theory, this could be optimized by taking the pointer's address,
         * since the string is interned. However, String.hashcode() caches the
         * hash anyway
         */
        return this.asBase64url.hashCode();
    }

    /**
     * Does the reverse of {@link #fromBase64url(String)}
     * 
     * @return the ID in Base64, URL variant with no padding, as per RFC4648
     */
    public String toBase64url() {
        return this.asBase64url;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "DeviceId:" + toBase64url();
    }
}
