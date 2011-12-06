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
import net.vleu.par.gwt.client.events.DeviceListChangedEvent;
import net.vleu.par.gwt.client.events.DeviceListChangedHandler;
import net.vleu.par.gwt.client.storage.AppLocalCache;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

/**
 * A presenter for {@link SelectDeviceTinyView}. It enables the view when the
 * current place is an instance of {@link PlaceWithDeviceId}, and disables it
 * otherwise
 */
public class SelectDeviceTinyPresenter extends SelectDeviceAbstractPresenter {

    /**
     * It is a private class so as to not expose our event handling methods
     * 
     * @see #onPlaceChange(PlaceChangeEvent) and
     *      {@link #onDeviceListChanged(DeviceListChangedEvent)}
     */
    private class EventHandler implements PlaceChangeEvent.Handler,
            DeviceListChangedHandler {
        /**
         * Updates the view with the new device list
         * 
         * @see DeviceListChangedHandler#onDeviceListChanged(DeviceListChangedEvent)
         */
        @Override
        public void onDeviceListChanged(final DeviceListChangedEvent event) {
            SelectDeviceTinyPresenter.this.view.changeDeviceList(event
                    .getNewDevicesList());
        }

        /**
         * Enables the view when the current place is an instance of
         * {@link PlaceWithDeviceId}, and disables it otherwise.
         */
        @Override
        public void onPlaceChange(final PlaceChangeEvent event) {
            reflectCurrentPlaceOnTheView(event.getNewPlace());
        }

    }

    /**
     * Used to remove the handler for {@link DeviceListChangedEvent} when
     * {@link #onStop()} is called
     */
    private HandlerRegistration deviceListEventRegistration;
    /**
     * Used to remove the handler for {@link PlaceChangeEvent} when
     * {@link #onStop()} is called
     */
    private HandlerRegistration placeChangeRegistration;

    /**
     * The view, as per MVP
     */
    private SelectDeviceTinyView view;

    public SelectDeviceTinyPresenter(final AppLocalCache appLocalCache,
            final EventBus eventBus, final PlaceController placeController) {
        super(appLocalCache, eventBus, placeController);
    }

    @Override
    public void onStop() {
        super.onStop();
        this.deviceListEventRegistration.removeHandler();
        this.placeChangeRegistration.removeHandler();
    }

    private void reflectCurrentPlaceOnTheView(final Place place) {
        if (place instanceof PlaceWithDeviceId)
            SelectDeviceTinyPresenter.this.view.setEnabled(true);
        else
            SelectDeviceTinyPresenter.this.view.setEnabled(false);
    }

    @Override
    public void start(final AcceptsOneWidget panel, final EventBus eventBus) {
        final EventHandler eventHandler = new EventHandler();
        this.view = new SelectDeviceTinyView(this);
        this.view.changeDeviceList(buildInitialDevicesList());
        this.deviceListEventRegistration =
                eventBus.addHandler(DeviceListChangedEvent.TYPE, eventHandler);
        this.placeChangeRegistration =
                eventBus.addHandler(PlaceChangeEvent.TYPE, eventHandler);
        panel.setWidget(this.view);
    }
}
