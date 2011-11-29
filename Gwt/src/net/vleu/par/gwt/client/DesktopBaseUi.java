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

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 *
 */
public class DesktopBaseUi extends Composite {

    interface DesktopBaseUiUiBinder extends UiBinder<Widget, DesktopBaseUi> {
    }

    private static DesktopBaseUiUiBinder uiBinder = GWT
            .create(DesktopBaseUiUiBinder.class);
    @UiField
    SimplePanel leftPanel;

    @UiField
    SimplePanel mainPanel;

    @UiField
    SimplePanel topPanel;

    /**
     * Because this class has a default constructor, it can be used as a binder
     * template. In other words, it can be used in other *.ui.xml files as
     * follows: <ui:UiBinder xmlns:ui="urn:ui:com.google.gwt.uibinder"
     * xmlns:g="urn:import:**user's package**">
     * <g:**UserClassName**>Hello!</g:**UserClassName> </ui:UiBinder> Note that
     * depending on the widget that is used, it may be necessary to implement
     * HasHTML instead of HasText.
     */
    public DesktopBaseUi() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    /**
     * @return the left panel
     */
    public SimplePanel getLeftPanel() {
        return this.leftPanel;
    }

    /**
     * @return the main panel
     */
    public SimplePanel getMainPanel() {
        return this.mainPanel;
    }

    /**
     * @return the top panel
     */
    public SimplePanel getTopPanel() {
        return this.topPanel;
    }
}
