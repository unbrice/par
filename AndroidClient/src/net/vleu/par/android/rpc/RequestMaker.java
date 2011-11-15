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

import net.vleu.par.C2dmToken;
import net.vleu.par.protocolbuffer.Devices.DeviceIdData;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayRequestData;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayRequestData.GetDeviceDirectivesData;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayRequestData.RegisterDeviceData;
import android.content.Context;

/**
 * This class is a helper for constructing the protocol buffers used to query
 * the GAE server
 */
public final class RequestMaker {
    private final DeviceIdData identifier;

    public RequestMaker(final Context context) {
        this.identifier = DeviceIdentifier.identifyCurrentDevice(context);
    }

    /**
     * Creates protocol buffers representing this request, as described in the
     * .proto file
     * 
     * @return A {@link GetDeviceDirectivesData} filled to be sent to the server
     */
    public GetDeviceDirectivesData makeGetDirectivesData() {
        final GatewayRequestData.GetDeviceDirectivesData.Builder builder =
                GatewayRequestData.GetDeviceDirectivesData.newBuilder();
        builder.setDeviceId(this.identifier);
        return builder.build();
    }

    /**
     * Creates protocol buffers representing this request, as described in the
     * .proto file
     * 
     * @param The
     *            latest available C2DM token
     * @return A {@link RegisterDeviceData} filled to be sent to the server
     */
    public RegisterDeviceData makeRegisterDeviceData(final C2dmToken c2dmToken) {
        final GatewayRequestData.RegisterDeviceData.Builder builder =
                GatewayRequestData.RegisterDeviceData.newBuilder();
        builder.setDeviceId(this.identifier);
        if (c2dmToken.value != null && c2dmToken.value.length() > 0)
            builder.setC2DmRegistrationId(c2dmToken.value);
        return builder.build();
    }
}
