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

import net.vleu.par.protocolbuffer.Devices.DeviceIdData;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayRequestData;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayRequestData.GetDeviceDirectivesData;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayRequestData.QueueDirectiveData;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayRequestData.RegisterDeviceData;

/**
 * Encapsulates a {@link GatewayRequestData}.
 */
public final class GatewayRequest {

    /**
     * Just like {@link Visitor} except that it can throw an
     * {@linkplain Exception}.
     */
    public static interface ThrowingVisitor {
        public void visit(GetDeviceDirectivesData data) throws Exception;

        public void visit(QueueDirectiveData data) throws Exception;

        public void visit(RegisterDeviceData data) throws Exception;
    }

    /**
     * This visitor validates the request, keeping tracks of errors.
     */
    private static class Validator implements Visitor {
        /** True if all visited objects so far are valid, false else */
        private boolean allValid = true;
        /** Strings describing the errors will be added to it */
        private final ArrayList<String> errors;

        /**
         * @param errors
         *            Strings describing the errors will be added to this
         */
        public Validator(final ArrayList<String> errors) {
            this.errors = errors;
        }

        private void checkDeviceId(final DeviceIdData data) {
            if (!DeviceId.isValidProtocolBuffer(data)) {
                this.errors.add("Invalid DeviceId");
                this.allValid = false;
            }
        }

        @Override
        public void visit(final GetDeviceDirectivesData data) {
            checkDeviceId(data.getDeviceId());
        }

        @Override
        public void visit(final QueueDirectiveData data) {
            checkDeviceId(data.getDeviceId());
        }

        @Override
        public void visit(final RegisterDeviceData data) {
            checkDeviceId(data.getDeviceId());
        }
    }

    public static interface Visitor extends ThrowingVisitor {
        @Override
        public void visit(GetDeviceDirectivesData data);

        @Override
        public void visit(QueueDirectiveData data);

        @Override
        public void visit(RegisterDeviceData data);
    }

    public static void accept(final GatewayRequestData reqData,
            final ThrowingVisitor visitor) throws Exception {
        for (final QueueDirectiveData proto : reqData.getQueueDirectiveList())
            visitor.visit(proto);
        for (final RegisterDeviceData proto : reqData.getRegisterDeviceList())
            visitor.visit(proto);
        for (final GetDeviceDirectivesData proto : reqData
                .getGetDeviceDirectivesList())
            visitor.visit(proto);
    }

    public static void accept(final GatewayRequestData reqData,
            final Visitor visitor) {
        try {
            accept(reqData, (ThrowingVisitor) visitor);
        }
        catch (final RuntimeException e) {
            throw e;
        }
        catch (final Exception e) {
            throw new InternalError(
                    "A non-throwing visitor throwed an exception !");
        }
    }

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
        final Validator validator = new Validator(errors);
        accept(reqData, validator);
        return validator.allValid;
    }

    private final GatewayRequestData proto;

    public GatewayRequest() {
        this.proto = GatewayRequestData.getDefaultInstance();
    }

    public GatewayRequest(final GatewayRequestData proto) {
        this.proto = proto;
    }

    public void accept(final ThrowingVisitor visitor) throws Exception {
        accept(this.proto, visitor);
    }

    public void accept(final Visitor visitor) {
        accept(this.proto, visitor);
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
     * with {@code this.proto} as the first argument.
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
