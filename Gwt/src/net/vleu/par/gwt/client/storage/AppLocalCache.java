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
package net.vleu.par.gwt.client.storage;

import java.util.ArrayList;

import net.vleu.par.gwt.client.events.DeviceListChangedEvent;
import net.vleu.par.gwt.client.events.DeviceListChangedHandler;
import net.vleu.par.gwt.shared.Device;
import net.vleu.par.gwt.shared.DeviceId;
import net.vleu.par.gwt.shared.DeviceName;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

/**
 * Monitors exchanges with the gateway and keeps some of the latest responses
 * for the presenters which might need them
 */
public class AppLocalCache {
    /**
     * This class exists so as to keep private the event handlers
     */
    private class MyHandler implements DeviceListChangedHandler {
        @Override
        public void onDeviceListChanged(final DeviceListChangedEvent event) {
            final ArrayList<Device> newDeviceList = event.getNewDevicesList();
            serializeDevicesList(newDeviceList);
        }
    }

    private static final String DEVICE_IDS_LIST_KEY = "deviceIds";

    private static final String DEVICE_NAMES_LIST_KEY = "deviceNames";

    private static String stringFromJsonValue(final JSONValue json) {
        if (json == null)
            return null;
        final JSONString jsonStr = json.isString();
        if (jsonStr == null)
            return null;
        return jsonStr.stringValue();
    }

    private final LocalStorage localStorage;

    public AppLocalCache() {
        this(new LocalStorage(PersistentStorage.getSingleton()));
    }

    /**
     * This constructor only exists so as to ease unit testing. Do not use it.
     */
    AppLocalCache(final LocalStorage localStorage) {
        this.localStorage = localStorage;
    }

    public ArrayList<Device> getCachedDevicesList() {
        /* Reads value from the local storage */
        final String idsJsonStr =
                this.localStorage.getFromAnyStore(DEVICE_IDS_LIST_KEY);
        final String namesJsonStr =
                this.localStorage.getFromAnyStore(DEVICE_NAMES_LIST_KEY);
        if (idsJsonStr == null || namesJsonStr == null)
            return null;

        /* Parses and validates value from the persistent store */
        final JSONArray idsJsonArray, namesJsonArray;
        try {
            idsJsonArray = JSONParser.parseLenient(idsJsonStr).isArray();
            namesJsonArray = JSONParser.parseLenient(namesJsonStr).isArray();
        }
        catch (final Exception _) {
            return null;
        }
        if (idsJsonArray.size() != namesJsonArray.size())
            return null;

        /* Builds the result by going through the array */
        final ArrayList<Device> res = new ArrayList<Device>();
        for (int i = 0; i < idsJsonArray.size(); i++) {
            final String id = stringFromJsonValue(idsJsonArray.get(i));
            final String name = stringFromJsonValue(namesJsonArray.get(i));
            if (id == null || name == null)
                continue;
            final Device device =
                    new Device(new DeviceId(id), new DeviceName(name));
            res.add(device);
        }

        return res;
    }

    public HandlerRegistration registerHandlers(final EventBus eventBus) {
        return eventBus
                .addHandler(DeviceListChangedEvent.TYPE, new MyHandler());
    }

    private void serializeDevicesList(final ArrayList<Device> newDeviceList) {
        final JSONArray idsJsonArray = new JSONArray();
        final JSONArray namesJsonArray = new JSONArray();

        int index = 0;
        for (final Device device : newDeviceList) {
            idsJsonArray.set(index, new JSONString(device.deviceId.value));
            namesJsonArray.set(index, new JSONString(device.deviceName.value));
            index++;
        }

        this.localStorage.putPersistingIfPossible(DEVICE_NAMES_LIST_KEY,
                namesJsonArray.toString());
        this.localStorage.putPersistingIfPossible(DEVICE_IDS_LIST_KEY,
                idsJsonArray.toString());
    }
}
