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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.SimplePanel;

/**
 *
 */
public class DesktopBaseUi extends Composite {

    private static DesktopBaseUiUiBinder uiBinder = GWT
            .create(DesktopBaseUiUiBinder.class);
    @UiField SimplePanel mainPanel;
    /**
     * @return the main panel
     */
    public SimplePanel getMainPanel() {
        return mainPanel;
    }

    /**
     * @return the top panel
     */
    public SimplePanel getTopPanel() {
        return topPanel;
    }

    /**
     * @return the left panel
     */
    public SimplePanel getLeftPanel() {
        return leftPanel;
    }

    @UiField SimplePanel topPanel;
    @UiField SimplePanel leftPanel;

    interface DesktopBaseUiUiBinder extends UiBinder<Widget, DesktopBaseUi> {
    }

    /**
     * Because this class has a default constructor, it can
     * be used as a binder template. In other words, it can be used in other
     * *.ui.xml files as follows:
     * <ui:UiBinder xmlns:ui="urn:ui:com.google.gwt.uibinder"
     *   xmlns:g="urn:import:**user's package**">
     *  <g:**UserClassName**>Hello!</g:**UserClassName>
     * </ui:UiBinder>
     * Note that depending on the widget that is used, it may be necessary to
     * implement HasHTML instead of HasText.
     */
    public DesktopBaseUi() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    public DesktopBaseUi(String firstName) {
        initWidget(uiBinder.createAndBindUi(this));

    }
}
