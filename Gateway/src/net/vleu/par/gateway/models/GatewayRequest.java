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

import java.util.ArrayList;
import java.util.List;

import net.vleu.par.protocolbuffer.GatewayCommands.GatewayRequestData;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayRequestData.GetDeviceDirectivesData;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayRequestData.QueueDirectiveData;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayRequestData.RegisterDeviceData;

/**
 * Encapsulates a {@link GatewayRequestData}.
 */
public final class GatewayRequest {
    /**
     * Checks that the data stored in the request are as described in the .proto
     * file
     * 
     * @param reqData
     *            The request to check
     * @param errors
     *            Strings describing the errors will be added to it
     * @return
     */
    public static boolean isValid(final GatewayRequestData reqData,
            final ArrayList<String> errors) {
        boolean valid = true;
        for (final QueueDirectiveData d : reqData.getQueueDirectiveList())
            if (!isValid(d, errors))
                valid = false;
        for (final RegisterDeviceData d : reqData.getRegisterDeviceList())
            if (!isValid(d, errors))
                valid = false;
        for (final GetDeviceDirectivesData d : reqData
                .getGetDeviceDirectivesList())
            if (!isValid(d, errors))
                valid = false;
        return valid;
    }

    /**
     * Checks that the data are as described in the .proto file
     * 
     * @param data
     *            Data to check
     * @param errors
     *            Strings describing the errors will be added to it
     * @return true is they are as described, false else
     */
    private static boolean isValid(final GetDeviceDirectivesData data,
            final ArrayList<String> errors) {
        if (!DeviceId.isValidProtocolBuffer(data.getDeviceId())) {
            errors.add("Invalid DeviceId");
            return false;
        }
        return true;
    }

    /**
     * Checks that the data are as described in the .proto file
     * 
     * @param data
     *            Data to check
     * @param errors
     *            Strings describing the errors will be added to it
     * @return true is they are as described, false else
     */
    private static boolean isValid(final QueueDirectiveData data,
            final ArrayList<String> errors) {
        if (!DeviceId.isValidProtocolBuffer(data.getDeviceId())) {
            errors.add("Invalid DeviceId");
            return false;
        }
        return true;
    }

    /**
     * Checks that the data are as described in the .proto file
     * 
     * @param data
     *            Data to check
     * @param errors
     *            Strings describing the errors will be added to it
     * @return true is they are as described, false else
     */
    private static boolean isValid(final RegisterDeviceData data,
            final ArrayList<String> errors) {
        if (!DeviceId.isValidProtocolBuffer(data.getDeviceId())) {
            errors.add("Invalid DeviceId");
            return false;
        }
        return false;
    }

    private final GatewayRequestData proto;

    public GatewayRequest() {
        this.proto = GatewayRequestData.getDefaultInstance();
    }

    public GatewayRequest(final GatewayRequestData proto) {
        this.proto = proto;
    }

    public List<GetDeviceDirectivesData> getGetDeviceDirectivesData() {
        return this.proto.getGetDeviceDirectivesList();
    }

    public List<QueueDirectiveData> getQueueDirectivesData() {
        return this.proto.getQueueDirectiveList();
    }

    public List<RegisterDeviceData> getRegisterDeviceData() {
        return this.proto.getRegisterDeviceList();
    }

    /**
     * Checks that the data stored in the request are as described in the .proto
     * file. It calls {@link GatewayRequest#isValid(GatewayRequest, ArrayList)}
     * with {@code this.proto.build()} as the first argument.
     * 
     * @param errors
     *            Strings describing the errors will be added to it
     * @return
     */
    @Deprecated
    public boolean isValid(final ArrayList<String> errors) {
        return isValid(this.proto, errors);
    }
}
