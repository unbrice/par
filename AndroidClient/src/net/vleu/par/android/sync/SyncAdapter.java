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
package net.vleu.par.android.sync;

import net.vleu.par.android.Config;
import net.vleu.par.android.preferences.Preferences;
import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

/**
 * The glue between the {@link SyncService} and the {@link Syncer} There is only
 * one instance in a given process, created by the {@link SyncService}
 */
final class SyncAdapter extends AbstractThreadedSyncAdapter {
    public static final String DEVICE_TYPE = "android";
    public static final String DM_REGISTERED = "dm_registered";

    public static final String LAST_SYNC = "last_sync";
    public static final String SERVER_LAST_SYNC = "server_last_sync";

    @SuppressWarnings("unused")
    private static final String TAG = Config.makeLogTag(SyncAdapter.class);

    private final Context context;
    private final Preferences preferences;

    SyncAdapter(final Context context) {
        super(context, false);
        this.context = context;
        this.preferences = new Preferences(this.context);
    }

    @Override
    public void onPerformSync(final Account account, final Bundle extras,
            final String authority, final ContentProviderClient provider,
            final SyncResult syncResult) {
        final Syncer.SynchronizationParameters parameters =
                new Syncer.SynchronizationParameters(extras);
        final Syncer syncer =
                new Syncer(account, this.context, parameters, this.preferences);
        syncer.performSynchronization(syncResult);
    }

}
