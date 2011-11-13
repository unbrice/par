package net.vleu.par.android.ui;

import net.vleu.par.android.R;
import net.vleu.par.android.preferences.Preferences;
import net.vleu.par.android.sync.SynchronizationSettings;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class PARAndroidClientActivity extends Activity {
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
        setupButtons();
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
            input.setText(this.prefs.getDeviceName());
        }
    }

    private void setupButtons() {
        ((Button) findViewById(R.id.main_set_device_name_button))
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(final View ignored) {
                        final EditText input =
                                (EditText) findViewById(R.id.main_set_device_name_edittext);
                        final String deviceName = input.getText().toString();
                        PARAndroidClientActivity.this.prefs.setDeviceName(deviceName);
                        SynchronizationSettings
                                .requestUploadOnlySynchronization(PARAndroidClientActivity.this);
                    }
                });
    }
}
