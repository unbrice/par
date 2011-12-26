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

import net.vleu.par.gwt.shared.Device;
import net.vleu.par.gwt.shared.DeviceId;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * This view allows to select a Device and refresh the device list and occupies
 * just a single line of the screen.
 */
public class SelectDeviceTinyView extends Composite implements HasEnabled {

    /**
     * A presenter (in the MVP sense) for {@link SelectDeviceTinyView}
     */
    public static interface Presenter {
        public void goToSamePlaceOtherDevice(DeviceId newDeviceId);

        public void refreshDevices();
    }

    interface SelectDeviceTinyViewUiBinder extends
            UiBinder<Widget, SelectDeviceTinyView> {
    }

    /**
     * An item that represents the fact that no Device is selected with the
     * {@link #deviceCombo}
     */
    private static final String NO_DEVICE_COMBO_ITEM = "";

    private static SelectDeviceTinyViewUiBinder uiBinder = GWT
            .create(SelectDeviceTinyViewUiBinder.class);
    /**
     * The name is the friendly Device name as per {@link Device#deviceName},
     * the value is the ID as per {@link Device#deviceId}
     */
    @UiField
    ListBox deviceCombo;

    private final Presenter presenter;

    @UiField
    Button refreshButton;

    public SelectDeviceTinyView(final Presenter presenter) {
        initWidget(uiBinder.createAndBindUi(this));
        this.presenter = presenter;
        this.deviceCombo.setEnabled(false);
    }

    /**
     * Updates the list of devices
     * 
     * If the newDeviceList has a {@link Device} with the same
     * {@link Device#deviceId} as the currently selected, it will stay selected
     * 
     * @param newDeviceList
     *            The new devices
     */
    public synchronized void changeDeviceList(
            final ArrayList<Device> newDeviceList) {
        final DeviceId currentlySelected = getCurrentlySelectDeviceId();
        int index = 0;
        this.deviceCombo.clear();
        for (final Device device : newDeviceList) {
            this.deviceCombo.addItem(device.deviceName.value,
                    HasDirection.Direction.LTR, device.deviceId.value);
            if (device.equals(currentlySelected))
                this.deviceCombo.setSelectedIndex(index);
            index++;
        }
        this.deviceCombo.addItem(NO_DEVICE_COMBO_ITEM);
        if (currentlySelected == null)
            this.deviceCombo.setSelectedIndex(index);
    }

    /**
     * @return The {@link DeviceId} of the currently selected device, null if
     *         none.
     */
    public synchronized DeviceId getCurrentlySelectDeviceId() {
        final int index = this.deviceCombo.getSelectedIndex();
        if (index == -1)
            return null;
        final String deviceIdStr = this.deviceCombo.getValue(index);
        if (NO_DEVICE_COMBO_ITEM.equals(deviceIdStr))
            return null;
        else
            return new DeviceId(deviceIdStr);
    }

    @Override
    public synchronized boolean isEnabled() {
        return this.deviceCombo.isEnabled() && this.refreshButton.isEnabled();
    }

    @UiHandler("deviceCombo")
    void onDeviceComboChange(final ChangeEvent event) {
        this.presenter.goToSamePlaceOtherDevice(getCurrentlySelectDeviceId());
    }

    @UiHandler("refreshButton")
    void onRefreshButtonClick(final ClickEvent e) {
        this.presenter.refreshDevices();
    }

    @Override
    public synchronized void setEnabled(final boolean enabled) {
        this.deviceCombo.setEnabled(enabled);
        this.refreshButton.setEnabled(enabled);
    }
}
