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

import java.util.ArrayList;

import net.vleu.par.gwt.shared.Device;

import com.google.web.bindery.event.shared.Event;

/**
 * Represents the fact that the Gateway answered with a list of the devices it
 * knows
 */
public class DeviceListReceivedEvent extends Event<DeviceListReceivedHandler> {
    public static final Event.Type<DeviceListReceivedHandler> TYPE =
            new Event.Type<DeviceListReceivedHandler>();
    private final ArrayList<Device> newDeviceList;

    public DeviceListReceivedEvent(final ArrayList<Device> newDeviceList) {
        this.newDeviceList = newDeviceList;
    }

    @Override
    protected void dispatch(final DeviceListReceivedHandler handler) {
        handler.onDeviceListReception(this);
    }

    @Override
    public Type<DeviceListReceivedHandler> getAssociatedType() {
        return TYPE;
    }

    public ArrayList<Device> getNewDeviceList() {
        return this.newDeviceList;
    }
}
