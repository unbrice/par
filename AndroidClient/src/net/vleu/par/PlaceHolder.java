package net.vleu.par;

import net.vleu.par.protocolbuffer.GatewayCommands.GatewayRequestData.Builder;
import android.util.Log;

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

/**
 * Bunch of static methods that acts as placeholder for the real code
 */
public final class PlaceHolder {

    private static final String TAG = "PlaceHolder";

    public static void addDeviceRegistrationToRequest(
            final Builder requestBuilder, final String c2dmToken) {
        Log.w(TAG, "registerDevice");
    }

    public static void addGetDirectiveToRequest(final Builder requestBuilder) {
        Log.w(TAG, "addGetDirectiveToRequest");

    }

    private PlaceHolder() {
    }
}
