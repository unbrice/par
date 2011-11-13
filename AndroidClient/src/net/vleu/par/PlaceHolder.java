package net.vleu.par;

import net.vleu.par.android.Config;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayRequestData;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayResponseData;
import android.accounts.Account;
import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

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

/**
 * Bunch of static methods that acts as placeholder for the real code
 */
public final class PlaceHolder {
    public static interface ExchangeWithServerCallack {
        public void onError(int callIndex, PlaceHolderException e);

        public void onResponse(GatewayResponseData resp);
    }

    public static class PlaceHolderException extends RuntimeException {
        private PlaceHolderException() {

        }
    }

    static final String EMPTY_ACCOUNT_NAME = "-";

    public static final Uri ROOT_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(Config.SYNC_AUTHORITY).appendPath("notes").build();

    private static final String TAG = "PlaceHolder";

    public static void blockingAuthenticateAccount(final Object... ignored)
            throws Exception {
        Log.w(TAG, "blockingAuthenticateAccount");

    }

    public static Uri buildNoteListUri(final String accountName) {
        return Uri.withAppendedPath(ROOT_URI, accountName == null
                ? EMPTY_ACCOUNT_NAME : accountName);
    }

    public static Uri buildNoteUri(final String accountName, final long noteId) {
        return Uri.withAppendedPath(buildNoteListUri(accountName),
                Long.toString(noteId));
    }

    public static void exchangeWithServer(final GatewayRequestData request,
            final ExchangeWithServerCallack exchangeWithServerCallack) {
        Log.w(TAG, "fetchDirectives");
    }

    /**
     * @param account
     */
    public static void fetchOrderFromAccount(final Account account) {
        Log.w(TAG, "fetchOrderFromAccount " + account.name);
    }

    public static String getAccountNameFromUri(final Uri uri) {
        if (!uri.toString().startsWith(ROOT_URI.toString()))
            throw new IllegalArgumentException("Uri is not a JumpNote URI.");

        return uri.getPathSegments().get(1);
    }

    public static boolean hasNewStuffToUpload() {
        Log.w(TAG, "needToReRegister");
        return false;
    }

    public static void registerDevice() {
        Log.w(TAG, "registerDevice");
    }

    public static void unregisterDevice() {
        Log.w(TAG, "unregisterDevice");
    }

    private PlaceHolder() {
    }
}
