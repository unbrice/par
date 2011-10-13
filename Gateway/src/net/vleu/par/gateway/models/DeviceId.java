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

import java.math.BigInteger;

import biz.source_code.base64Coder.Base64Coder;

public final class DeviceId implements Comparable<DeviceId> {
    public static final int BASE64_RESERVED_FIRST_BITS = 8;
    private static final int BASE64_TOTAL_BYTES = 9;

    private static final BigInteger MIN_LONG_AS_BIGINT = BigInteger
            .valueOf(Long.MIN_VALUE);

    /** Does the reverse of toBase64 */
    public static DeviceId fromBase64(final String str) {
        final byte[] array = Base64Coder.decode(str);
        BigInteger bigInt = new BigInteger(1, array);
        bigInt = bigInt.add(MIN_LONG_AS_BIGINT);
        return new DeviceId(bigInt.longValue());
    }

    public final long asLong;

    public DeviceId(final long idAsLong) {
        this.asLong = idAsLong;
    }

    @Override
    public int compareTo(final DeviceId other) {
        return Long.signum(this.asLong - other.asLong);
    }

    @Override
    public boolean equals(final Object other) {
        if ((other == null) || !(other instanceof DeviceId))
            return false;
        else {
            final DeviceId otherAsDevice = (DeviceId) other;
            return this.asLong == otherAsDevice.asLong;
        }
    }

    @Override
    public int hashCode() {
        return ((Long) this.asLong).hashCode();
    }

    /**
     * Encodes the ID in Base64, padded to 12 characters. The first character is
     * always 'A'. This serves as as kind of 'version number'.
     */
    public String toBase64() {
        final byte[] bytes = value().toByteArray();
        // Prepend 0 bytes so that there are BASE64_TOTAL_BYTES bytes
        final byte[] paddedBytes = new byte[BASE64_TOTAL_BYTES];
        final int missingBytes = BASE64_TOTAL_BYTES - bytes.length;
        assert (missingBytes >= 0);
        for (int n = 0; n < bytes.length; ++n)
            paddedBytes[missingBytes + n] = bytes[n];
        return String.valueOf(Base64Coder.encode(paddedBytes));
    }

    @Override
    public String toString() {
        return "DeviceId:" + value().toString();
    }

    /**
     * @return The Device ID as a positive 64-bit number.
     */
    public final BigInteger value() {
        final BigInteger asBigInt = BigInteger.valueOf(this.asLong);
        return asBigInt.subtract(MIN_LONG_AS_BIGINT);
    }
}
