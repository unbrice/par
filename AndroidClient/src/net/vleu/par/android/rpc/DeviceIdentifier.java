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
package net.vleu.par.android.rpc;

import net.vleu.par.protocolbuffer.Devices.DeviceIdData;
import android.content.ContentResolver;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;

public final class DeviceIdentifier {
    private static String[] BLACKLISTED_ANDROID_IDS = {

    };

    private static boolean isValidAndroidId(final String androidId) {
        /* null is not a valid ID */
        if (androidId == null)
            return false;

        /* Check that it is an hexadecimal number. Archos, I am looking at you. */
        try {
            Long.parseLong(androidId, 16);
        }
        catch (final NumberFormatException e) {
            return false;
        }

        /* Almost done, just check the blacklist */
        for (final String blackListed : BLACKLISTED_ANDROID_IDS)
            if (blackListed.equals(androidId))
                return false;

        return true;
    }

    private final ContentResolver contentResolver;

    private final TelephonyManager telephonyManager;

    private final WifiManager wifiManager;

    public DeviceIdentifier(final ContentResolver contentResolver,
            final TelephonyManager telephonyManager,
            final WifiManager wifiManager) {
        this.contentResolver = contentResolver;
        this.telephonyManager = telephonyManager;
        this.wifiManager = wifiManager;
    }

    public DeviceIdData identifyCurrentDevice() {
        final DeviceIdData.Builder res = DeviceIdData.newBuilder();
        final long value;

        /* 1: Android ID */
        final String androidId =
                Secure.getString(this.contentResolver,
                        android.provider.Settings.Secure.ANDROID_ID);
        if (isValidAndroidId(androidId)) {
            value = Long.parseLong(androidId, 16);
            res.setAndroidId(value);
            return res.build();
        }

        /* 2&3: IMEI or MEID */
        if (this.telephonyManager != null) {

            final String deviceId = this.telephonyManager.getDeviceId();
            final int phoneType = this.telephonyManager.getPhoneType();
            switch (phoneType) {
            case TelephonyManager.PHONE_TYPE_GSM:
                value = Long.parseLong(deviceId, 10);
                res.setTelephonyEmei(value);
                return res.build();
            case TelephonyManager.PHONE_TYPE_CDMA:
                value = Long.parseLong(deviceId, 16);
                res.setTelephonyMeid(value);
                return res.build();
            }
        }

        /* 4: WiFi Mac address */
        if (this.wifiManager != null) {
            final WifiInfo wifiInfo = this.wifiManager.getConnectionInfo();
            final String macAddr = wifiInfo.getMacAddress();
            value = Long.parseLong(macAddr, 16);
            res.setWifiMac(value);
            return res.build();
        }
        throw new InternalError("TODO: Add other ids");

    }
}
