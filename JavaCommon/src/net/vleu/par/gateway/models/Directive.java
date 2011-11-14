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
import java.util.Arrays;

import net.vleu.par.protocolbuffer.Commands.DirectiveData;
import net.vleu.par.protocolbuffer.Commands.HapticNotificationData;
import net.vleu.par.protocolbuffer.Commands.StatusBarNotificationData;
import net.vleu.par.protocolbuffer.Devices.DeviceIdData;

import com.google.protobuf.InvalidProtocolBufferException;

public final class Directive {
    @SuppressWarnings("serial")
    public static class InvalidDirectiveSerialisation extends Exception {
        private final DeviceIdData invalidProto;

        private InvalidDirectiveSerialisation(final String message) {
            super(message);
            this.invalidProto = null;
        }

        private InvalidDirectiveSerialisation(final String message,
                final DeviceIdData invalidProto) {
            super(message);
            this.invalidProto = invalidProto;
        }

        private InvalidDirectiveSerialisation(final String message,
                final Exception cause) {
            super(message, cause);
            this.invalidProto = null;
        }

        /** @return The protocol buffer that triggered the exception, or null */
        public DeviceIdData getInvalidProtocolBuffer() {
            return this.invalidProto;
        }
    }

    public static interface ThrowingVisitor {
        public void visit(HapticNotificationData data) throws Exception;

        public void visit(StatusBarNotificationData data) throws Exception;
    }

    /**
     * This visitor validates the directive, keeping tracks of errors.
     */
    private static class Validator implements Visitor {
        /** True if all visited objects so far are valid, false else */
        private final boolean allValid = true;
        /** Strings describing the errors will be added to it */
        private final ArrayList<String> errors;

        /**
         * @param errors
         *            Strings describing the errors will be added to this
         */
        public Validator(final ArrayList<String> errors) {
            this.errors = errors;
        }

        @Override
        public void visit(final HapticNotificationData data) {
            /* All well-formed protocol buffers are valid */
        }

        @Override
        public void visit(final StatusBarNotificationData data) {
            /* All well-formed protocol buffers are valid */
        }
    }

    public static interface Visitor extends ThrowingVisitor {
        @Override
        public void visit(HapticNotificationData data);

        @Override
        public void visit(StatusBarNotificationData data);
    }

    public static final long[] DEFAULT_VIBRATION_SEQUENCE = { 0L, 100L, 250L,
            1000L, 250L, 500L };

    public static void accept(final DirectiveData dirData,
            final ThrowingVisitor visitor) throws Exception {
        for (final HapticNotificationData proto : dirData
                .getHapticNotificationList())
            visitor.visit(proto);
        for (final StatusBarNotificationData proto : dirData
                .getStatusbarNotificationList())
            visitor.visit(proto);
    }

    public static void
            accept(final DirectiveData dirData, final Visitor visitor) {
        try {
            accept(dirData, (ThrowingVisitor) visitor);
        }
        catch (final RuntimeException e) {
            throw e;
        }
        catch (final Exception e) {
            throw new InternalError(
                    "A non-throwing visitor throwed an exception !");
        }
    }

    public static Directive fromProtocolBuffer(final byte[] data)
            throws InvalidDirectiveSerialisation {
        Directive res;
        try {
            res = new Directive(DirectiveData.parseFrom(data));
        }
        catch (final InvalidProtocolBufferException e) {
            throw new InvalidDirectiveSerialisation(
                    "Protocol Buffer does not parse", e);
        }
        // TODO: Check it is valid
        return res;
    }

    /**
     * Checks that the data stored in the directives are as described in the
     * .proto file
     * 
     * @param dirData
     *            The directive to check. Can be null (in which case it's
     *            invalid).
     * @param errors
     *            Strings describing the errors will be added to it
     * @return False if reqData is null or if the data are valid, else true
     */
    public static boolean isValid(final DirectiveData dirData,
            final ArrayList<String> errors) {
        if (dirData != null) {
            final Validator validator = new Validator(errors);
            accept(dirData, validator);
            return validator.allValid;
        }
        else {
            errors.add("Not a ProtocolBuffer !");
            return false;
        }
    }

    private final DirectiveData proto;

    public Directive(final DirectiveData proto) {
        this.proto = proto;
    }

    public DirectiveData asProtocolBuffer() {
        return this.proto;
    }

    public byte[] asProtocolBufferBytes() {
        return this.proto.toByteArray();
    }

    /**
     * <b>This method is slow and meant to be used for tests</b> {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other) {
        if (other != null && other instanceof Directive) {
            final Directive otherDirective = (Directive) other;
            return Arrays.equals(asProtocolBufferBytes(),
                    otherDirective.asProtocolBufferBytes());
        }
        return false;
    };

    /**
     * <b>This method is slow</b> {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(asProtocolBufferBytes());
    }

    @Override
    public String toString() {
        return Arrays.toString(asProtocolBufferBytes());
    }
}
