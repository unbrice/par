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
package net.vleu.par.android.rpc;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import net.jcip.annotations.ThreadSafe;
import net.vleu.par.android.Config;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayRequestData;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;

@ThreadSafe
public final class Transceiver {
    public static interface EnsureHasTokenWithUICallback {
        public void onAuthDenied();

        public void onError(Throwable e);

        public void onHasToken(String authToken);
    }

    @SuppressWarnings("serial")
    public static class InvalidAuthTokenException extends Exception {
        public InvalidAuthTokenException() {
            super();
        }

        public InvalidAuthTokenException(final String message) {
            super(message);
        }
    }

    @SuppressWarnings("serial")
    public static class RequestedUserAuthenticationException extends Exception {
    }

    /**
     * This class helps manage stored ACSID tokens. TODO: use a persistent
     * cookie store instead of this intermediate structure
     */
    private static class TokenStoreHelper extends SQLiteOpenHelper {
        private static final String[] ALL_COLUMNS = new String[] { "account",
                "token" };
        private static final String TABLE_NAME = "tokens";

        TokenStoreHelper(final Context context) {
            super(context, "tokens.db", null, 1);
        }

        public String getToken(final Account account) {
            final SQLiteDatabase db = getReadableDatabase();
            final Cursor c =
                    db.query(TABLE_NAME, ALL_COLUMNS, "account = ?",
                            new String[] { account.name }, null, null, null);
            if (!c.moveToNext()) {
                c.close();
                db.close();
                return null;
            }
            final String token = c.getString(1);
            c.close();
            db.close();
            return token;
        }

        public void invalidateToken(final Account account) {
            final SQLiteDatabase db = getWritableDatabase();
            db.delete(TABLE_NAME, "account = ?", new String[] { account.name });
            db.close();
        }

        @Override
        public void onCreate(final SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_NAME
                + " (account TEXT UNIQUE, token TEXT);");
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
                final int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }

        public void putToken(final Account account, final String token) {
            final SQLiteDatabase db = getWritableDatabase();
            final ContentValues values = new ContentValues();
            values.put("account", account.name);
            values.put("token", token);
            db.insertWithOnConflict(TABLE_NAME, null, values,
                    SQLiteDatabase.CONFLICT_REPLACE);
            db.close();
        }
    }

    public static final String APPENGINE_SERVICE_NAME = "ah";

    public static final int NEED_AUTH_INTENT = 2;

    public static final int NEED_AUTH_NOTIFICATION = 1;

    public static void ensureHasTokenWithUI(final Activity activity,
            final Account account, final EnsureHasTokenWithUICallback callback) {
        final AccountManager am = AccountManager.get(activity);
        am.getAuthToken(account, APPENGINE_SERVICE_NAME, null, activity,
                new AccountManagerCallback<Bundle>() {
                    @Override
                    public
                            void
                            run(final AccountManagerFuture<Bundle> authBundleFuture) {
                        Bundle authBundle = null;
                        try {
                            authBundle = authBundleFuture.getResult();
                        }
                        catch (final OperationCanceledException e) {
                            callback.onAuthDenied();
                            return;
                        }
                        catch (final AuthenticatorException e) {
                            callback.onError(e);
                            return;
                        }
                        catch (final IOException e) {
                            callback.onError(e);
                            return;
                        }

                        if (authBundle
                                .containsKey(AccountManager.KEY_AUTHTOKEN))
                            callback.onHasToken((String) authBundle
                                    .get(AccountManager.KEY_AUTHTOKEN));
                        else
                            callback.onError(new IllegalStateException(
                                    "No auth token available, but operation not canceled."));
                    }
                }, null);
    }

    private final String mAuthUrlTemplate;

    private final Context mContext;

    private final DefaultHttpClient mHttpClient;

    private final TokenStoreHelper mTokenStoreHelper;

    public Transceiver(final Context context, final String authUrlTemplate,
            final String rpcUrl) {
        this.mContext = context;
        this.mAuthUrlTemplate = authUrlTemplate;
        this.mTokenStoreHelper = new TokenStoreHelper(context);
        this.mHttpClient = new DefaultHttpClient();
    }

    public void blockingAuthenticateAccount(final Account account,
            final int needAuthAction, final boolean forceReauthenticate)
            throws AuthenticationException, OperationCanceledException,
            RequestedUserAuthenticationException, InvalidAuthTokenException {

        final String existingToken = this.mTokenStoreHelper.getToken(account);
        if (!forceReauthenticate && existingToken != null) {
            final BasicClientCookie c =
                    new BasicClientCookie("ACSID", existingToken);
            try {
                c.setDomain(new URI(Config.SERVER_BASE_URL).getHost());
                this.mHttpClient.getCookieStore().addCookie(c);
                return;
            }
            catch (final URISyntaxException e) {
            }
        }

        // Get an auth token for this account.
        final AccountManager am = AccountManager.get(this.mContext);
        Bundle authBundle = null;
        String authToken = null;

        // Block on getting the auth token result.
        try {
            authBundle =
                    am.getAuthToken(account, APPENGINE_SERVICE_NAME,
                            needAuthAction == NEED_AUTH_NOTIFICATION, null,
                            null).getResult();
        }
        catch (final IOException e) {
            throw new AuthenticationException(
                    "IOException while getting auth token.", e);
        }
        catch (final AuthenticatorException e) {
            throw new AuthenticationException(
                    "AuthenticatorException while getting auth token.", e);
        }

        if (authBundle.containsKey(AccountManager.KEY_INTENT)
            && needAuthAction == NEED_AUTH_INTENT) {
            final Intent authRequestIntent =
                    (Intent) authBundle.get(AccountManager.KEY_INTENT);
            this.mContext.startActivity(authRequestIntent);
            throw new RequestedUserAuthenticationException();
        }
        else if (authBundle.containsKey(AccountManager.KEY_AUTHTOKEN))
            authToken = authBundle.getString(AccountManager.KEY_AUTHTOKEN);

        if (authToken == null)
            throw new AuthenticationException("Retrieved auth token was null.");

        try {
            blockingAuthenticateWithToken(account, authToken);
        }
        catch (final InvalidAuthTokenException e) {
            am.invalidateAuthToken(account.type, authToken);
            throw e;
        }
    }

    private void blockingAuthenticateWithToken(final Account account,
            final String authToken) throws AuthenticationException,
            InvalidAuthTokenException {
        // Promote the given auth token into an App Engine ACSID token.
        final HttpGet httpGet =
                new HttpGet(String.format(this.mAuthUrlTemplate, authToken));
        String acsidToken = null;

        try {
            final HttpResponse response = this.mHttpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == 403)
                throw new InvalidAuthTokenException();

            final List<Cookie> cookies =
                    this.mHttpClient.getCookieStore().getCookies();
            for (final Cookie cookie : cookies)
                if (cookie.getName().equals("ACSID")) {
                    acsidToken = cookie.getValue();
                    break;
                }

            if (acsidToken == null
                && response.getStatusLine().getStatusCode() == 500)
                // If no ACSID cookie was passed, it usually means the auth
                // token was invalid;
                throw new InvalidAuthTokenException(
                        "ACSID cookie not found in HTTP response: "
                            + response.getStatusLine().toString()
                            + "; assuming invalid auth token.");

            this.mTokenStoreHelper.putToken(account, acsidToken);
        }
        catch (final ClientProtocolException e) {
            throw new AuthenticationException(
                    "HTTP Protocol error authenticating to App Engine", e);
        }
        catch (final IOException e) {
            throw new AuthenticationException(
                    "IOException authenticating to App Engine", e);
        }
    }

    public void invalidateAccountAcsidToken(final Account account) {
        this.mTokenStoreHelper.invalidateToken(account);
    }

    public void postData(final GatewayRequestData request) {
        // Create a new HttpClient and Post Header
        final HttpClient httpclient = new DefaultHttpClient();
        final HttpPost httppost =
                new HttpPost("http://www.yoursite.com/script.php");

        try {
            // Add your data
            final ArrayList<BasicNameValuePair> nameValuePairs =
                    new ArrayList<BasicNameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("id", "12345"));
            nameValuePairs.add(new BasicNameValuePair("stringdata",
                    "AndDev is Cool!"));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // Execute HTTP Post Request
            final HttpResponse response = httpclient.execute(httppost);

        }
        catch (final ClientProtocolException e) {
            // TODO Auto-generated catch block
        }
        catch (final IOException e) {
            // TODO Auto-generated catch block
        }
    }

}
