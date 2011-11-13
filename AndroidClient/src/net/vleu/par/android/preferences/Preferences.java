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
package net.vleu.par.android.preferences;

import java.util.ArrayList;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;

/**
 * This class allows to set and retrieve the preferences and parameters shared
 * among classes of this application.
 */
@ThreadSafe
public final class Preferences {
    public static interface OnChangeListener {
        public void onPreferencesChanged(String changedKey);
    }

    /**
     * Serves as a change listener for {@link #privatePrefs}
     */
    @ThreadSafe
    private final class SharedPreferencesChangeListener implements
            OnSharedPreferenceChangeListener {

        @Override
        public synchronized void onSharedPreferenceChanged(
                final SharedPreferences sharedPreferences,
                final String changedKey) {
            for (final OnChangeListener listener : Preferences.this.listeners)
                listener.onPreferencesChanged(changedKey);
        }

    }

    /** The key for the Device Name as presented to the user by the server */
    public static final String KEY_DEVICE_NAME = "device_name";
    /** The filename for the current version */
    private static final String PRIVATE_PREFERENCES_NAME = "version0";
    /**
     * Added by {@link #registerOnChangeListener(OnChangeListener)}, removed by
     * {@link #unregisterAllOnchangeListeners()} and
     * {@link #unregisterOnChangeListener(OnChangeListener)}
     */
    @GuardedBy(value="itself")
    private final ArrayList<OnChangeListener> listeners;

    // ThreadSafe
    private final SharedPreferences privatePrefs;

    /*
     * It is important to keep this thread, otherwise it will be
     * garbage-collected ! CF.
     * http://stackoverflow.com/questions/2542938/sharedpreferences
     * -onsharedpreferencechangelistener
     * -not-being-called-consistently/3104265#3104265
     */
    /** Registered as a ChangeListener in {@link #privatePrefs} */
    private final SharedPreferencesChangeListener sharedPreferencesListener;

    public Preferences(final Context context) {
        this.privatePrefs =
                context.getSharedPreferences(PRIVATE_PREFERENCES_NAME,
                        Context.MODE_PRIVATE);
        this.listeners = new ArrayList<Preferences.OnChangeListener>();
        this.sharedPreferencesListener = new SharedPreferencesChangeListener();
        this.privatePrefs
                .registerOnSharedPreferenceChangeListener(this.sharedPreferencesListener);
    }

    public String getDeviceName() {
        return this.privatePrefs.getString(KEY_DEVICE_NAME, Build.MODEL);
    }

    public void registerOnChangeListener(final OnChangeListener listener) {
        synchronized (this.listeners) {
            this.listeners.add(listener);
        }
    }

    public void setDeviceName(final String newName) {
        this.privatePrefs.edit().putString(KEY_DEVICE_NAME, newName).commit();
    }

    public void unregisterAllOnchangeListeners() {
        synchronized (this.listeners) {
            this.listeners.clear();
        }
    }

    public void unregisterOnChangeListener(final OnChangeListener listener) {
        synchronized (this.listeners) {
            this.listeners.remove(listener);
        }
    }
}
