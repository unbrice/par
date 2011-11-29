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

import net.vleu.par.gwt.client.AppPlaceHistoryMapper;
import net.vleu.par.gwt.client.PlaceWithDeviceId;
import net.vleu.par.gwt.shared.DeviceId;

import com.google.gwt.place.shared.PlaceTokenizer;

public class SelectDirectivePlace extends PlaceWithDeviceId {
    /**
     * Used by the {@link AppPlaceHistoryMapper}
     */
    public static class Tokenizer implements
            PlaceTokenizer<SelectDirectivePlace> {
        private static final String NO_DEVICE_ID_TOKEN = "";

        @Override
        public SelectDirectivePlace getPlace(final String token) {
            final DeviceId deviceId;
            if (token.equals(NO_DEVICE_ID_TOKEN))
                deviceId = null;
            else
                deviceId = new DeviceId(token);
            return new SelectDirectivePlace(deviceId);
        }

        @Override
        public String getToken(final SelectDirectivePlace place) {
            if (place.hasDeviceId())
                return place.getDeviceId().value;
            else
                return NO_DEVICE_ID_TOKEN;
        }

    }

    protected SelectDirectivePlace(final DeviceId device) {
        super(device);
    }

    @Override
    public boolean equals(final Object otherObj) {
        if (otherObj instanceof SelectDirectivePlace)
            return super.equals((SelectDirectivePlace) otherObj);
        else
            return false;
    }

    @Override
    public SelectDirectivePlace withOtherDeviceId(final DeviceId deviceId) {
        return new SelectDirectivePlace(deviceId);
    }
}
