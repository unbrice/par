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

import net.vleu.par.gwt.client.Screen;
import net.vleu.par.protocolbuffer.GatewayResponseData.DeviceDescriptionData;

import com.google.web.bindery.event.shared.Event;

/**
 * Represent the fact that a user moved from a Device+Screen pair to another
 */
public class NavigationEvent extends Event<NavigationHandler> {

    public static final Event.Type<NavigationHandler> TYPE =
            new Event.Type<NavigationHandler>();

    private final DeviceDescriptionData targetDevice;
    private final Screen targetScreen;

    public NavigationEvent(final DeviceDescriptionData targetDevice,
            final Screen targetScreen) {
        this.targetDevice = targetDevice;
        this.targetScreen = targetScreen;
    }

    @Override
    protected void dispatch(final NavigationHandler handler) {
        handler.onNavigation(this);
    }

    @Override
    public Event.Type<NavigationHandler> getAssociatedType() {
        return TYPE;
    }

    public DeviceDescriptionData getTargetDevice() {
        return this.targetDevice;
    }

    public Screen getTargetScreen() {
        return this.targetScreen;
    }

}
