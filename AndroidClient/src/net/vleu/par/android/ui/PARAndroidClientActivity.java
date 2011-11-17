package net.vleu.par.android.ui;

import net.vleu.par.DeviceName;
import net.vleu.par.android.R;
import net.vleu.par.android.preferences.Preferences;
import net.vleu.par.android.rpc.Transceiver;
import net.vleu.par.android.sync.SynchronizationControler;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

public class PARAndroidClientActivity extends Activity {
    private class SynchronizationRequester implements Runnable {
        @Override
        public void run() {
            SynchronizationControler
                    .requestUploadOnlySynchronization(PARAndroidClientActivity.this);
        }

    }

    /**
     * @return true if both are argument are equal or if one of them is null
     */
    private static boolean
            isEqualOrNull(final String first, final String second) {
        return (first == null || second == null || first.equals(second));
    }

    /** Created in {@link #onStart()}, destroyed in {@link #onStop()}. */
    private Preferences prefs;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.prefs = new Preferences(PARAndroidClientActivity.this);
        setContentView(R.layout.main);
        setupEnableSwitchToggleButton();
        setupSetDeviceNameButton();
    }

    @Override
    public void onStart() {
        super.onStart();
        this.prefs = new Preferences(this);
        refreshFromPreferences(null);
        this.prefs.registerOnChangeListener(new Preferences.OnChangeListener() {

            @Override
            public void onPreferencesChanged(final String changedKey) {
                refreshFromPreferences(changedKey);
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        this.prefs.unregisterAllOnchangeListeners();
        this.prefs = null;
    }

    /**
     * This method is in charge for applying preferences to this interface.
     * 
     * @param key
     *            A value as from {@link Preferences}.KEY_* or null if unknown.
     */
    private void refreshFromPreferences(final String key) {
        if (isEqualOrNull(key, Preferences.KEY_DEVICE_NAME)) {
            final EditText input =
                    (EditText) findViewById(R.id.main_set_device_name_edittext);
            input.setText(this.prefs.getDeviceName().value);
        }
        if (isEqualOrNull(key, Preferences.KEY_SYNCHRONIZATION_ENABLED)) {
            final ToggleButton toggle =
                    (ToggleButton) findViewById(R.id.main_enable_switch_toggleButton);
            toggle.setChecked(this.prefs.isSynchronizationEnabled());
        }
    }

    private void setupEnableSwitchToggleButton() {
        ((ToggleButton) findViewById(R.id.main_enable_switch_toggleButton))
                .setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(
                            final CompoundButton buttonView,
                            final boolean isChecked) {
                        PARAndroidClientActivity.this.prefs
                                .setSynchronizationEnabled(isChecked);
                        SynchronizationControler
                                .requestUploadOnlySynchronization(PARAndroidClientActivity.this);
                    }
                });
    }

    private void setupSetDeviceNameButton() {
        ((Button) findViewById(R.id.main_set_device_name_button))
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(final View ignored) {
                        final EditText input =
                                (EditText) findViewById(R.id.main_set_device_name_edittext);
                        final DeviceName deviceName =
                                new DeviceName(input.getText().toString());
                        if (deviceName.isValid()) {
                            PARAndroidClientActivity.this.prefs
                                    .setDeviceName(deviceName);
                            Transceiver.askUserForPermissionsIfNecessary(
                                    PARAndroidClientActivity.this,
                                    new SynchronizationRequester());
                        }
                        else {
                            /*
                             * Displays a message and reloads the device name,
                             * ignoring changes
                             */
                            final String message =
                                    PARAndroidClientActivity.this
                                            .getResources()
                                            .getString(
                                                    R.string.main_invalid_device_name);
                            Toast.makeText(PARAndroidClientActivity.this,
                                    message, Toast.LENGTH_LONG).show();
                            refreshFromPreferences(Preferences.KEY_DEVICE_NAME);
                        }
                    }
                });
    }
}
