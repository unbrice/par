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
import net.vleu.par.android.rpc.Transceiver;
import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.preference.Preference;

/**
 * This class should be used from outside the package (like by the UI) to
 * control synchronization.
 */
public final class SynchronizationControler {

    /**
     * All classes but {@link Preference} should call
     * {@link Preferences#isSynchronizationEnabled()} instead
     * 
     * @param context
     *            Used to access the account manager, no reference will be kept.
     * @return True if Auto Sync is enabled in the account manager.
     */
    public static boolean isAutoSyncDesired(final Context context) {
        if (ContentResolver.getMasterSyncAutomatically())
            for (final Account account : Transceiver
                    .listGoogleAccounts(context))
                if (ContentResolver.getIsSyncable(account,
                        Config.SYNC_AUTHORITY) > 0
                    && ContentResolver.getSyncAutomatically(account,
                            Config.SYNC_AUTHORITY))
                    return true;
        return false;
    }

    /**
     * Indicates to Android that the application is syncable for this account.
     * 
     * It is part of the initialization we have to perform when the
     * SYNC_EXTRAS_INITIALIZE extra is present in the {@link Syncer}'s
     * arguments, according to {@link ContentResolver#SYNC_EXTRAS_INITIALIZE}'s
     * description.
     * 
     * @param context
     *            Used to access the account manager, no reference will be kept.
     * @param account
     *            The account to initialize
     */
    public static void registerAsSyncable(final Context context,
            final Account account) {
        ContentResolver.setIsSyncable(account, Config.SYNC_AUTHORITY, 1);
    }

    /** Triggers a synchronization if an account exists for this account */
    public static void
            requestBidirectionalSynchronization(final Context context) {
        requestSynchronization(context, false);
    }

    /** Triggers a synchronization if an account exists for this account */
    private static void requestSynchronization(final Context context,
            final boolean uploadOnly) {
        final Syncer.SynchronizationParameters bundle =
                new Syncer.SynchronizationParameters();
        bundle.setManualSync(true);
        bundle.setExpeditedSync(true);
        bundle.setUploadOnly(uploadOnly);
        for (final Account account : Transceiver.listGoogleAccounts(context))
            ContentResolver.requestSync(account, Config.SYNC_AUTHORITY,
                    bundle.getBundle());
    }

    /** Triggers a synchronization if an account exists for this account */
    public static void requestUploadOnlySynchronization(final Context context) {
        requestSynchronization(context, true);
    }

    /**
     * Enables or disables synchronization for this service (but if Master
     * synchronization is disabled, no data will be exchanged) All classes but
     * {@link Preference} should call
     * {@link Preferences#setSynchronizationEnabled(boolean)} instead
     * 
     * @param context
     *            Used to access the account manager, no reference will be kept.
     * @param enable
     *            true to enable, false to disable
     */
    public static void setAutomaticSync(final Context context,
            final boolean enable) {
        for (final Account account : Transceiver.listGoogleAccounts(context))
            if (account.type.equals(Config.GOOGLE_ACCOUNT_TYPE)) {
                registerAsSyncable(context, account);
                ContentResolver.setSyncAutomatically(account,
                        Config.SYNC_AUTHORITY, enable);
            }
    }

    private SynchronizationControler() {
    }
}
