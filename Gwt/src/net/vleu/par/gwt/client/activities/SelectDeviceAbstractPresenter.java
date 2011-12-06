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

import java.util.ArrayList;

import net.vleu.par.gwt.client.DesktopActivityMapper;
import net.vleu.par.gwt.client.PlaceWithDeviceId;
import net.vleu.par.gwt.client.events.DeviceListRequestedEvent;
import net.vleu.par.gwt.client.storage.AppLocalCache;
import net.vleu.par.gwt.shared.Device;
import net.vleu.par.gwt.shared.DeviceId;
import net.vleu.par.gwt.shared.DeviceName;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;

public abstract class SelectDeviceAbstractPresenter extends
        Bug6653AbstractActivity implements SelectDeviceTinyView.Presenter {

    /**
     * Search for a device with needle's ID in the haystack, returns true if it
     * finds one, false otherwise
     * 
     * @param haystack
     *            searched for the needle
     * @param needle
     *            the searched needle
     * @return true if it found the needle, else false
     */
    private static boolean containsDeviceWithId(
            final ArrayList<Device> haystack, final DeviceId needle) {
        for (final Device device : haystack)
            if (needle.equals(device.deviceId))
                return true;
        return false;
    }

    private final AppLocalCache appLocalCache;

    private final EventBus eventBus;

    private final PlaceController placeController;

    public SelectDeviceAbstractPresenter(final AppLocalCache appLocalCache,
            final EventBus eventBus, final PlaceController placeController) {
        this.appLocalCache = appLocalCache;
        this.eventBus = eventBus;
        this.placeController = placeController;
    }

    /**
     * @return a non-null list with known devices from the cache. The current
     *         device will be added to that list if absent
     */
    protected ArrayList<Device> buildDevicesList() {
        return cloneWithCurrentDevice(this.appLocalCache.getCachedDevicesList());
    }

    /**
     * Makes a swallow copy of its argument, adding the current device if no
     * device with its ID is present
     * 
     * @param original
     *            The list to copy
     * @return A swallow copy with the current device added
     */
    protected ArrayList<Device> cloneWithCurrentDevice(
            final ArrayList<Device> original) {
        final DeviceId currentId = getCurrentDeviceId();
        boolean resultContainsCurrentId = false;
        final ArrayList<Device> res;
        if (original == null)
            res = new ArrayList<Device>(1);
        else
            res = new ArrayList<Device>(original);
        /* Adds the current device id if there is one */
        if (currentId != null) {
            for (final Device device : res)
                if (currentId.equals(device.deviceId))
                    resultContainsCurrentId = true;
            if (!resultContainsCurrentId) {
                final Device currentDevice =
                        new Device(currentId, new DeviceName(currentId.value));
                res.add(currentDevice);
            }
        }
        return res;
    }

    /**
     * @return the {@link DeviceId} associated with the current {@link Place},
     *         null if there is none
     */
    protected DeviceId getCurrentDeviceId() {
        final Place where = this.placeController.getWhere();
        if (where instanceof PlaceWithDeviceId)
            return ((PlaceWithDeviceId) where).getDeviceId();
        else
            return null;
    }

    /**
     * Calls {@link #goToSamePlaceOtherDevice(DeviceId)} with null as argument
     * if the current device, as returned by {@link #getCurrentDeviceId()} is
     * not present in the list
     * 
     * Deprecated because it seems cleaner to have the {@link DesktopActivityMapper}
     * to handle this by hiding the activity if the device is unknown
     */
    @Deprecated
    protected void
            gotoForgetCurrentDeviceIfAbsent(final ArrayList<Device> list) {
        final DeviceId currentDeviceId = getCurrentDeviceId();
        if (currentDeviceId != null
            && !containsDeviceWithId(list, currentDeviceId))
            goToSamePlaceOtherDevice(null);
    }
    
    /**
     * @see PlaceController#getWhere()
     * @return this.placeController.getWhere()
     */
    protected Place getWhere() {
        return this.placeController.getWhere();
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
