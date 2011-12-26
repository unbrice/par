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

import net.vleu.par.gwt.client.events.NewDirectiveEvent;
import net.vleu.par.gwt.shared.DeviceId;
import net.vleu.par.protocolbuffer.DirectiveData;
import net.vleu.par.protocolbuffer.StatusBarNotificationData;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.web.bindery.event.shared.EventBus;

public class CreateStatusBarNotificationActivity extends
        Bug6653AbstractActivity implements
        CreateStatusBarNotificationView.Presenter {
    private static CreateStatusBarNotificationView viewSingleton = null;
    private final DeviceId currentDeviceId;

    private EventBus eventBus;

    /**
     * @param place
     */
    public CreateStatusBarNotificationActivity(
            final CreateStatusBarNotificationPlace place) {
        this.currentDeviceId = place.getDeviceId();
    }

    /**
     * 
     * @see CreateStatusBarNotificationView.Presenter#send(net.vleu.par.protocolbuffer.StatusBarNotificationData)
     */
    @Override
    public void send(final StatusBarNotificationData statusBarNotification) {
        final DirectiveData directive = DirectiveData.create();
        directive.addStatusbarNotification(statusBarNotification);
        final NewDirectiveEvent event =
                new NewDirectiveEvent(this.currentDeviceId, directive);
        this.eventBus.fireEventFromSource(event, this);
    }

    @Override
    public void start(final AcceptsOneWidget panel, final EventBus eventBus) {
        if (viewSingleton == null)
            viewSingleton = new CreateStatusBarNotificationView(this);
        this.eventBus = eventBus;
        panel.setWidget(viewSingleton);
    }
}
