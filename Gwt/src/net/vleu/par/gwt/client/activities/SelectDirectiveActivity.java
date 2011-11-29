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
import net.vleu.par.gwt.shared.DeviceId;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.web.bindery.event.shared.EventBus;

public class SelectDirectiveActivity extends Bug6653AbstractActivity implements
        SelectDirectiveView.Presenter {
    private final PlaceController placeController;

    public SelectDirectiveActivity(final PlaceController placeController) {
        this.placeController = placeController;
    }

    @Override
    public void goTo(final PlaceWithDeviceId place) {
        this.placeController.goTo(place);
    }

    @Override
    public void start(final AcceptsOneWidget panel, final EventBus eventBus) {
        panel.setWidget(new SelectDirectiveView(this));
    }
    
    @Override
    public DeviceId getCurrentDeviceId() {
        Place where = this.placeController.getWhere();
        if (where instanceof PlaceWithDeviceId)
            return ((PlaceWithDeviceId) where).getDeviceId();
        else
            return null;
    }
}
