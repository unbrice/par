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

public final class DeviceId implements Comparable<DeviceId> {
    /**
     * Does the reverse of {@link #toBase64url()}
     * 
     * @param base64Str
     *            the ID in Base64, URL variant, as per RFC4648.
     * @return An opaque object representing the DeviceId
     */
    public static DeviceId fromBase64url(final String base64Str) {
        return new DeviceId(base64Str);
    }

    /**
     * This String has been interned as per {@link String#intern()}. It is
     * encoded in base64, URL variant, as per RFC4648
     */
    private final String asBase64url;

    private final Pattern BASE64URL_WHITELIST = Pattern
            .compile("[a-zA-Z0-9\\-_]*");

    private DeviceId(final String idAsLong) {
        throwIfInvalidBase64url(idAsLong);
        this.asBase64url = idAsLong.intern();
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
            /* This is OK because both strings are interned */
            return this.asBase64url == otherAsDevice.asBase64url;
        }
    }

    @Override
    public int hashCode() {
        /*
         * In theory, this could be optimized by taking the pointer's address,
         * since the string is interned. However, String.hashcode() cache the
         * hash anyway
         */
        return this.asBase64url.hashCode();
    }

    private void throwIfInvalidBase64url(final String str)
            throws IllegalArgumentException {
        // TODO: Validate str by cheking it is a valid PB representation
        final Matcher matcher = this.BASE64URL_WHITELIST.matcher(str);
        if (matcher.matches())
            throw new IllegalArgumentException("Invalid base64url:" + str);
    }

    /**
     * Does the reverse of {@link #fromBase64url(String)}
     * 
     * @return the ID in Base64, URL variant, as per RFC4648
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
