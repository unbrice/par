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
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;

public final class SynchronizationSettings {
    
    /**
     * Enables synchronization for this service (but if Master synchronization
     * is disabled, no data will be exchanged)
     * 
     * @param context
     *            Used to access the account manager, no reference will be kept.
     */
    public static void enableAutomaticSync(final Context context) {
        for (final Account account : getGoogleAccounts(context))
            if (account.type.equals(Config.GOOGLE_ACCOUNT_TYPE)) {
                ContentResolver
                        .setIsSyncable(account, Config.SYNC_AUTHORITY, 1);
                ContentResolver.setSyncAutomatically(account,
                        Config.SYNC_AUTHORITY, true);
            }
    }

    private static Account[] getGoogleAccounts(final Context context) {
        final AccountManager am = AccountManager.get(context);
        return am.getAccountsByType(Config.GOOGLE_ACCOUNT_TYPE);
    }

    /**
     * @param context
     *            Used to access the account manager, no reference will be kept.
     * @return True if Auto Sync is enabled in the account manager.
     */
    public static boolean isAutoSyncDesired(final Context context) {
        if (ContentResolver.getMasterSyncAutomatically())
            for (final Account account : getGoogleAccounts(context))
                if (ContentResolver.getIsSyncable(account,
                        Config.SYNC_AUTHORITY) > 0
                    && ContentResolver.getSyncAutomatically(account,
                            Config.SYNC_AUTHORITY))
                    return true;
        return false;
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
        bundle.setUploadOnly(uploadOnly);
        final Account[] accounts = getGoogleAccounts(context);
        if (accounts.length > 0) {
            final Account anySuitableAccount =
                    new Account(accounts[0].name,
                            SyncAdapter.GOOGLE_ACCOUNT_TYPE);
            ContentResolver.requestSync(anySuitableAccount,
                    Config.SYNC_AUTHORITY, bundle.getBundle());
        }
    }

    /** Triggers a synchronization if an account exists for this account */
    public static void requestUploadOnlySynchronization(final Context context) {
        requestSynchronization(context, true);
    }

    private SynchronizationSettings() {
    }
}
