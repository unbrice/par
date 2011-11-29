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
package net.vleu.par.gwt.client.activities;

import net.vleu.par.gwt.client.PlaceWithDeviceId;
import net.vleu.par.gwt.client.events.DeviceListRequestedEvent;
import net.vleu.par.gwt.shared.DeviceId;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;

public abstract class SelectDeviceAbstractPresenter extends
        Bug6653AbstractActivity implements SelectDeviceTinyView.Presenter {

    private final EventBus eventBus;

    private final PlaceController placeController;

    public SelectDeviceAbstractPresenter(final EventBus eventBus,
            final PlaceController placeController) {
        this.eventBus = eventBus;
        this.placeController = placeController;
    }

    @Override
    public void goToSamePlaceOtherDevice(final DeviceId newDeviceId) {
        final Place where = this.placeController.getWhere();
        if (where instanceof PlaceWithDeviceId) {
            final Place newPlace =
                    ((PlaceWithDeviceId) where).withOtherDeviceId(newDeviceId);
            this.placeController.goTo(newPlace);
        }
    }

    @Override
    public void refreshDevices() {
        final DeviceListRequestedEvent event = new DeviceListRequestedEvent();
        this.eventBus.fireEventFromSource(event, this);
    }
}
