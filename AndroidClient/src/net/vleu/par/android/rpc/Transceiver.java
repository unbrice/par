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
import java.io.InputStream;
import java.util.List;

import net.jcip.annotations.ThreadSafe;
import net.vleu.par.WrappedString;
import net.vleu.par.android.Config;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayRequestData;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayResponseData;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

/**
 * This class is in charge for handling HTTP transport and authentication
 */
@ThreadSafe
public final class Transceiver {
    /**
     * Thrown when a {@link GoogleAuthToken} or an {@link SacsidToken} we have
     * been using has expired and needs to be renewed.
     */
    @SuppressWarnings("serial")
    public static class AuthenticationTokenExpired extends Exception {
    }

    /**
     * Simple wrapper for a Google authentication token, as per Android's
     * authentication managers
     */
    public static final class GoogleAuthToken extends WrappedString {
        public GoogleAuthToken(final String value) {
            super(value);
        }
    }

    /**
     * Used to signal that a GoogleAuthToken is invalid
     */
    @SuppressWarnings("serial")
    public static class InvalidGoogleAuthTokenException extends Exception {
        public InvalidGoogleAuthTokenException() {
            super();
        }

        public InvalidGoogleAuthTokenException(final String message) {
            super(message);
        }
    }

    /**
     * Simple wrapper for an SACSID token, as per Google App Engine
     */
    public static final class SacsidToken extends WrappedString {
        public SacsidToken(final String value) {
            super(value);
        }

        public Cookie asCookie() {
            final BasicClientCookie cookie =
                    new BasicClientCookie("SACSID", this.value);
            cookie.setDomain(Config.SERVER_DOMAIN);
            return cookie;
        }
    }

    private static final String APPENGINE_TOKEN_TYPE = "ah";

    public static final String[] GOOGLE_ACCOUNT_REQUIRED_SYNCABILITY_FEATURES =
            new String[] { "service_ah" };

    public static final String GOOGLE_ACCOUNT_TYPE = "com.google";

    /**
     * Can be used as an argument to {@link HttpGet#setParams(HttpParams)} to
     * disable redirections
     */
    private static final HttpParams HTTP_PARAMS_NO_REDIRECTIONS =
            (new BasicHttpParams()).setBooleanParameter(
                    ClientPNames.HANDLE_REDIRECTS, false);

    private static final String SERVER_AUTH_URL_PREFIX = Config.SERVER_BASE_URL
        + "/_ah/login?continue=http://localhost/&auth=";

    private static final String SHARED_PREFERENCES_PREFIX = "transceiver-";

    private static String TAG = Config.makeLogTag(Transceiver.class);

    /**
     * Ensures that application has a fresh permission from the user to use all
     * of his Google accounts
     * 
     * @param activity
     *            The Activity context to use for launching a new sub-Activity
     *            to prompt the user for a password if necessary; used only to
     *            call startActivity(); must not be null.
     * @param onUserResponse
     *            If not null, will be called in the main thread after the user
     *            answered for all accounts
     */
    public static void askUserForPermissionsIfNecessary(
            final Activity activity, final Runnable onUserResponse) {
        final Account[] allAcounts = listGoogleAccounts(activity);
        final Runnable callback = new Runnable() {
            int count = 0;

            @Override
            public void run() {
                this.count++;
                if (this.count == allAcounts.length && onUserResponse != null)
                    onUserResponse.run();
            }
        };
        for (final Account account : allAcounts)
            askUserForSinglePermissionIfNecessary(activity, callback, account);
    }

    /**
     * Ensures that application has a fresh permission from the user to use his
     * account
     * 
     * @param activity
     *            The Activity context to use for launching a new sub-Activity
     *            to prompt the user for a password if necessary; used only to
     *            call startActivity(); must not be null.
     * @param onUserResponse
     *            If not null, will be called in the main thread after the user
     *            answered
     */
    public static void askUserForSinglePermissionIfNecessary(
            final Activity activity, final Runnable onUserResponse,
            final Account account) {
        final AccountManager am = AccountManager.get(activity);
        final AccountManagerCallback<Bundle> callback =
                new AccountManagerCallback<Bundle>() {
                    @Override
                    public void run(final AccountManagerFuture<Bundle> bundle) {
                        if (onUserResponse != null)
                            activity.runOnUiThread(onUserResponse);
                    }
                };
        am.getAuthToken(account, APPENGINE_TOKEN_TYPE, null, activity,
                callback, null);
    }

    /**
     * Lists all Google accounts on this device
     * 
     * @param context
     *            Won't be kept anywhere
     * @return A possibly empty array of Google Accounts
     */
    public static Account[] listGoogleAccounts(final Context context) {
        final AccountManager am = AccountManager.get(context);
        return am.getAccountsByType(GOOGLE_ACCOUNT_TYPE);
    }

    private final Account account;

    private final Context context;

    private final DefaultHttpClient httpClient;

    private final SharedPreferences sharedPreferences;

    public Transceiver(final Account account, final Context context) {
        final String sharedPrefsName = SHARED_PREFERENCES_PREFIX + account.name;
        this.account = account;
        this.context = context;
        this.httpClient = new DefaultHttpClient();
        this.sharedPreferences =
                context.getSharedPreferences(sharedPrefsName,
                        Context.MODE_PRIVATE);
        final PersistentCookieStore cookieStore =
                new PersistentCookieStore(this.sharedPreferences);
        this.httpClient.setCookieStore(cookieStore);
    }

    /**
     * This method may block while a network request completes, and must never
     * be made from the main thread. It requests a new GoogleAuthToken.
     * 
     * @param account
     *            The account to fetch an auth token for
     * @return A token associated with this account
     * @throws OperationCanceledException
     *             if the request was canceled for any reason, including the
     *             user canceling a credential request
     * @throws AuthenticatorException
     *             if the authenticator failed to respond
     * @throws IOException
     *             if the authenticator experienced an I/O problem creating a
     *             new auth token, usually because of network trouble
     */
    private GoogleAuthToken blockingGetNewAuthToken()
            throws OperationCanceledException, AuthenticatorException,
            IOException {
        final AccountManager am = AccountManager.get(this.context);
        final String authTokenStr =
                am.blockingGetAuthToken(this.account, APPENGINE_TOKEN_TYPE,
                        true);
        if (Log.isLoggable(TAG, Log.INFO))
            Log.i(TAG, "Got a new GoogleAuthToken for account: "
                + this.account.name + ": " + authTokenStr);
        if (authTokenStr == null)
            throw new AuthenticatorException("Could not get an auth token");
        else
            // return new GoogleAuthToken(authTokenStr);
            return new GoogleAuthToken(
                    "DQAAAEQBAADgWo7arlxrkkC3rhu3BKsd3BWLkxYDBX88yjMw3hXzNPrXqoZK6-FrJzTyDpOc4qhnv0daT6QgFQr4DYPSEfVZnlMBFTE_jKyR0UJRC73tbGd7Alz8mrUcHi7ODAxuXB-C4g5iGEsUyNFGPw8P6KGcRQVNrdGiq4CNDogZ5fJDziFXZ0SUHmcCGIlF8Pj_L57mZ3fp9xbbxI4Gw2MgUYbVKvkd5cGnCz3fPME5ZD6ohcurNcZ8Tm-P59lNkV1mPFG6iNE8QAD6XoDZen2wHwjkwg0tdQRlgTGrQs3w5Vgwi1CpaIFIGAtN6Wiq_SDzLdq3EHfRXkFiplPzXczN-7LsWw-V2T0XNwsD5xUOFfYgCkmGDwsaEi8Ua8hsUntOTQ3tuAxYrAcyXaNeWMOZzOnhmaXbbqr_7iFgMgrgZygbqJf3Hm-OHrlcFYjRVuR54vM");
    }

    /**
     * Clears the SACSID token we might have had
     */
    public void clearSacsidToken() {
        this.httpClient.getCookieStore().clear();
    }

    /**
     * Releases allocated resources. The instance cannot be used afterward.
     */
    public void dispose() {
        this.httpClient.getConnectionManager().shutdown();
    }

    /**
     * Performs authentication if necessary, then exchange the data with the
     * server and returns the response
     * 
     * @param request
     * @return The response to the request, null if none
     * @throws IOException
     * @throws OperationCanceledException
     * @throws AuthenticatorException
     */
    public GatewayResponseData exchangeWithServer(
            final GatewayRequestData request) throws IOException,
            OperationCanceledException, AuthenticatorException {
        final int maxRetries = 10;
        for (int retry = 0; retry < maxRetries; retry++) {
            if (!hasSacsidToken()) {
                /* Gets a Google Auth Token and promotes it to a SACSID Token */
                final GoogleAuthToken googleAuthToken =
                        blockingGetNewAuthToken();
                try {
                    promoteToken(googleAuthToken);
                }
                catch (final InvalidGoogleAuthTokenException e) {
                    if (Log.isLoggable(TAG, Log.WARN))
                        Log.w(TAG,
                                "The google auth token is invalid. Refreshing all cookies. ",
                                e);
                    invalidatesGoogleAuthToken(googleAuthToken);
                    clearSacsidToken();
                    continue;
                }
            }
            /* Executes the query */
            try {
                return postData(request);
            }
            catch (final AuthenticationTokenExpired e) {
                clearSacsidToken();
                if (Log.isLoggable(TAG, Log.WARN))
                    Log.w(TAG, "Google and/or SACSID tokens expired. Retried "
                        + retry + " times.", e);
                continue;
            }
        }
        final String failureMessage =
                "Failed to get valid Google and SACSID tokens !";
        if (Log.isLoggable(TAG, Log.ERROR))
            Log.e(TAG, failureMessage);
        throw new AuthenticatorException(failureMessage);
    }

    /**
     * Goes through {@link #httpClient}'s cookies to find an {@link SacsidToken}
     * 
     * @return true if it finds one, else false
     */
    public boolean hasSacsidToken() {
        final List<Cookie> cookies =
                this.httpClient.getCookieStore().getCookies();
        for (final Cookie cookie : cookies)
            if (cookie.getName().equals("SACSID"))
                return true;
        return false;
    }

    /**
     * Invalidates the {@link GoogleAuthToken} stored in the
     * {@link AccountManager}
     */
    private void invalidatesGoogleAuthToken(final GoogleAuthToken token) {
        if (token != null) {
            final AccountManager am = AccountManager.get(this.context);
            if (Log.isLoggable(TAG, Log.DEBUG))
                Log.d(TAG, "Invalidating GoogleAuthToken : " + token.value);
            am.invalidateAuthToken(APPENGINE_TOKEN_TYPE, token.value);
        }
    }

    /**
     * Sends the request to the distant server and parses the response
     * 
     * @param request
     *            Won't be checked
     * @return The response, or null if there weren't any
     * @throws IOException
     *             I/O error, usually because of network trouble
     * @throws AuthenticationTokenExpired
     *             The token expired while we were using it, it needs to be
     *             renewed
     */
    private GatewayResponseData postData(final GatewayRequestData request)
            throws IOException, AuthenticationTokenExpired {
        final HttpPost httpRequest = new HttpPost(Config.SERVER_RPC_URL);
        final ByteArrayEntity requestEntity =
                new ByteArrayEntity(request.toByteArray());
        InputStream responseStream = null;
        HttpResponse response = null;
        httpRequest.setEntity(requestEntity);
        try {
            response = this.httpClient.execute(httpRequest);
            final int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 403)
                throw new AuthenticationTokenExpired();
            else if (response.getEntity() == null)
                return null;
            else if (statusCode != 200) {
                final String answer =
                        EntityUtils.toString(response.getEntity());
                throw new IOException("GAE server answered code: " + statusCode
                    + "; message: " + answer);
            }
            else {
                responseStream = response.getEntity().getContent();
                return GatewayResponseData.parseFrom(responseStream);
            }
        }
        finally {
            httpRequest.abort();
            if (responseStream != null)
                responseStream.close();
            /*
             * if (response != null) response.getEntity().consumeContent();
             */
        }
    }

    /**
     * Promotes a {@link GoogleAuthToken} into an {@link SacsidToken} by talking
     * with the AppEngine server and adds this token to {@link #httpClient}'s
     * cookie jar.
     * 
     * @param account
     *            The account to which the {@link GoogleAuthToken} is associated
     * @param googleAuthToken
     *            Associated to the {@link Account}
     * @throws InvalidGoogleAuthTokenException
     *             The token was rejected by the server
     * @throws IOException
     *             Most likely a network failure
     */
    private void promoteToken(final GoogleAuthToken googleAuthToken)
            throws IOException, InvalidGoogleAuthTokenException {

        HttpResponse response = null;
        HttpGet request = null;
        try {
            request =
                    new HttpGet(SERVER_AUTH_URL_PREFIX + googleAuthToken.value);
            /* The auth page will redirect to an url we don't care about */
            request.setParams(HTTP_PARAMS_NO_REDIRECTIONS);

            response = this.httpClient.execute(request);
        }
        finally {
            if (request != null)
                request.abort();
            /*
             * if (response != null) response.getEntity().consumeContent();
             */
        }

        if (response.getStatusLine().getStatusCode() == 403) {
            clearSacsidToken();
            throw new InvalidGoogleAuthTokenException(
                    "Token rejected by the server");
        }

        if (!hasSacsidToken()) {
            clearSacsidToken();
            // If no SACSID cookie was passed, it usually means the auth
            // token was invalid;
            throw new InvalidGoogleAuthTokenException(
                    "SACSID cookie not found in HTTP response: "
                        + response.getStatusLine().toString()
                        + "; assuming invalid auth token.");

        }
    }

}
