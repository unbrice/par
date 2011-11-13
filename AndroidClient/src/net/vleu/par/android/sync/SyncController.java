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

public final class SyncController {

    /**
     * 
     * @param context Used to access the accoutn manager, no reference will be kept.
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
        AccountManager am = AccountManager.get(context);
        return am.getAccountsByType(Config.GOOGLE_ACCOUNT_TYPE);
    }
    
    /**
     * @param context Used to access the accoutn manager, no reference will be kept.
     * @return True if Auto Sync is enabled in the account manager.
     */
    public static boolean isAutoSyncDesired(final Context context) {
        if (ContentResolver.getMasterSyncAutomatically()) {
            for (Account account : getGoogleAccounts(context)) {
                if (ContentResolver.getIsSyncable(account, Config.SYNC_AUTHORITY) > 0 &&
                        ContentResolver.getSyncAutomatically(account, Config.SYNC_AUTHORITY)) {
                    return true;
                }
            }
        }
        return false;
    }
}
