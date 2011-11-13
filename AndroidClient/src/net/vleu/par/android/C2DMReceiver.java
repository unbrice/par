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
/*
 * Based on work copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.vleu.par.android;

import net.vleu.par.android.sync.SyncController;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.android.c2dm.C2DMBaseReceiver;
import com.google.android.c2dm.C2DMessaging;

/**
 * Broadcast receiver that handles Android Cloud to Data Messaging (AC2DM)
 * messages, initiated by the JumpNote App Engine server and routed/delivered by
 * Google AC2DM servers. The only currently defined message is 'sync'.
 */
public class C2DMReceiver extends C2DMBaseReceiver {
    static final String TAG = Config.makeLogTag(C2DMReceiver.class);

    /**
     * Register or unregister based on phone sync settings. Called on each
     * performSync by the SyncAdapter.
     */
    public static void refreshAppC2DMRegistrationState(final Context context) {
        // Determine if there are any auto-syncable accounts. If there are, make
        // sure we are
        // registered with the C2DM servers. If not, unregister the application.
        final boolean autoSyncEnabled =
                SyncController.isAutoSyncDesired(context);

        final boolean c2dmRegistered =
                !C2DMessaging.getRegistrationId(context).equals("");

        if (c2dmRegistered != autoSyncEnabled) {
            Log.i(TAG, "System-wide desirability for auto sync has changed; "
                + (autoSyncEnabled ? "registering" : "unregistering")
                + " application with C2DM servers.");

            if (autoSyncEnabled == true)
                C2DMessaging.register(context, Config.C2DM_SENDER);
            else
                C2DMessaging.unregister(context);
        }
    }

    public C2DMReceiver() {
        super(Config.C2DM_SENDER);
    }

    @Override
    public void onError(final Context context, final String errorId) {
        Toast.makeText(context, "Messaging registration error: " + errorId,
                Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onMessage(final Context context, final Intent intent) {
        SyncController.requestBidirectionalSynchronization(context);
    }
}
