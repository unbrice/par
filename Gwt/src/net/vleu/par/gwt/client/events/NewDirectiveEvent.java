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
package net.vleu.par.gwt.client.events;

import net.vleu.par.gwt.shared.DeviceId;
import net.vleu.par.protocolbuffer.DirectiveData;

import com.google.web.bindery.event.shared.Event;

/**
 * Represents the fact that the user created a new directive, probably by
 * hitting the "send" button of a screen
 */
public class NewDirectiveEvent extends Event<NewDirectiveHandler> {

    public static final Event.Type<NewDirectiveHandler> TYPE =
            new Event.Type<NewDirectiveHandler>();

    private final DeviceId deviceId;
    private final DirectiveData directive;

    public NewDirectiveEvent(final DeviceId deviceId,
            final DirectiveData directive) {
        this.deviceId = deviceId;
        this.directive = directive;
    }

    @Override
    protected void dispatch(final NewDirectiveHandler handler) {
        handler.onNewGatwayRequest(this);
    }

    @Override
    public Event.Type<NewDirectiveHandler> getAssociatedType() {
        return TYPE;
    }

    public DeviceId getDeviceId() {
        return this.deviceId;
    }

    public DirectiveData getDirective() {
        return this.directive;
    }

}
