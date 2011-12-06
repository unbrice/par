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
package net.vleu.par.gwt.client;

import net.vleu.par.gwt.client.activities.CreateStatusBarNotificationActivity;
import net.vleu.par.gwt.client.activities.CreateStatusBarNotificationPlace;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;

public class DesktopActivityMapper implements ActivityMapper {

    @Override
    public Activity getActivity(final Place place) {
        // TODO: If the current device is not in the cache, show a message explaining how to change the device
        if (place instanceof PlaceWithDeviceId
            && !((PlaceWithDeviceId) place).hasDeviceId())
            return null; // TODO return an activity explaining to set a device
        else if (place instanceof CreateStatusBarNotificationPlace)
            return new CreateStatusBarNotificationActivity(
                    (CreateStatusBarNotificationPlace) place);
        else
            return null;
    }

}
