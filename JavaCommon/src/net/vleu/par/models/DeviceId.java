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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.vleu.par.WrappedString;
import net.vleu.par.protocolbuffer.Devices.DeviceIdBuilderData;
import biz.source_code.base64Coder.Base64UrlCoder;

public final class DeviceId extends WrappedString implements
        Comparable<DeviceId> {
    private final static Pattern BASE64URL_WHITELIST = Pattern
            .compile("[a-zA-Z0-9\\-_]*");
    private static final int MAX_DEVICE_ID_LEN = 48;

    /**
     * Tells if the String is suitable for being a DeviceId. It must be: - not
     * too short/too long - a Base64 string
     * 
     * @param deviceId
     *            Will be checked
     * @return true if valid, else false
     */
    public static boolean isValidDeviceIdString(final String deviceId) {
        /** Protects the decoding from messages that might make it crash */
        if (deviceId.length() < 1)
            return false;
        else if (deviceId.length() > MAX_DEVICE_ID_LEN)
            return false;
        else {
            final Matcher matcher = BASE64URL_WHITELIST.matcher(deviceId);
            if (!matcher.matches())
                return false;
        }
        /** Tries to decode the DeviceId, as a last check */
        try {
            Base64UrlCoder.decode(deviceId);
            return true;
        }
        catch (final Exception _) {
            return false;
        }
    }

    /**
     * @param value
     *            A {@link DeviceIdBuilderData} encoded as Base64 URL variant
     *            with no padding, as per RFC4648
     */
    public DeviceId(final String value) {
        super(value);
    }

    @Override
    public int compareTo(final DeviceId other) {
        return this.value.compareTo(other.value);
    }
}
