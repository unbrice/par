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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.place.shared.Place;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class SelectDirectiveView extends Composite {

    interface DirectiveSelectorViewUiBinder extends
            UiBinder<Widget, SelectDirectiveView> {
    }

    /** As per MVP, this glues the view with the rest of the app */
    public interface Presenter {
        /**
         * @return the currently manipulated {@link DeviceId}, null if there is
         *         no current device
         */
        public DeviceId getCurrentDeviceId();

        /**
         * Called when the view wants to change the current {@link Place}
         * 
         * @param place
         *            The place where to go
         */
        public void goTo(PlaceWithDeviceId place);
    }

    private static DirectiveSelectorViewUiBinder uiBinder = GWT
            .create(DirectiveSelectorViewUiBinder.class);

    private Presenter presenter;

    @UiField
    Button statusBarNotificationButton;

    public SelectDirectiveView(final Presenter presenter) {
        initWidget(uiBinder.createAndBindUi(this));
        this.presenter = presenter;
    }

    @UiHandler("statusBarNotificationButton")
    void statusBarNotificationButtonClick(final ClickEvent e) {
        this.presenter.goTo(new CreateStatusBarNotificationPlace(this.presenter
                .getCurrentDeviceId()));
    }
}
