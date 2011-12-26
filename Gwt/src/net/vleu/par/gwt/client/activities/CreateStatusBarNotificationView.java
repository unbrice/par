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

import net.vleu.par.protocolbuffer.StatusBarNotificationData;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * A view that allows the use to create and send a
 * {@link StatusBarNotificationData}
 */
public class CreateStatusBarNotificationView extends Composite {

    interface CreateStatusBarNotificationViewUiBinder extends
            UiBinder<Widget, CreateStatusBarNotificationView> {
    }

    /**
     * A presenter (in the MVP sense) for
     * {@link CreateStatusBarNotificationView}
     */
    public static interface Presenter {
        /**
         * Called with the directive to transmit when the user asks for a such
         * transmission
         * 
         * @param proto
         *            The data to transmit
         */
        public void send(StatusBarNotificationData proto);
    }

    private static CreateStatusBarNotificationViewUiBinder uiBinder = GWT
            .create(CreateStatusBarNotificationViewUiBinder.class);

    private final Presenter presenter;

    @UiField
    Button sendButton;

    @UiField
    TextBox textBox;
    @UiField
    TextBox titleBox;

    public CreateStatusBarNotificationView(final Presenter presenter) {
        initWidget(uiBinder.createAndBindUi(this));
        this.presenter = presenter;
    }

    @UiHandler("sendButton")
    void onClick(final ClickEvent e) {
        final String text = this.textBox.getText();
        final String title = this.titleBox.getText();
        final StatusBarNotificationData notification =
                StatusBarNotificationData.create();
        if (text.length() > 0)
            notification.setText(text);
        if (title.length() > 0)
            notification.setTitle(title);
        this.presenter.send(notification);
    }
}
