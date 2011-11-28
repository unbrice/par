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

import com.google.web.bindery.event.shared.Event;

/**
 * Represents the fact that the list of known devices changed, possibly because
 * of a {@link DeviceListReceivedEvent}
 */
public class DeviceListChangedEvent extends Event<DeviceListChangedHandler> {
    public static final Event.Type<DeviceListChangedHandler> TYPE =
            new Event.Type<DeviceListChangedHandler>();

    @Override
    protected void dispatch(final DeviceListChangedHandler handler) {
        handler.onDeviceListChange(this);
    }

    @Override
    public Type<DeviceListChangedHandler> getAssociatedType() {
        return TYPE;
    }

}
