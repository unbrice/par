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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

@ThreadSafe
public final class Transceiver {
    /**
     * Simple wrapper for an ACSID token, as per Google App Engine
     */
    public static final class AcsidToken extends WrappedString {
        public AcsidToken(final String value) {
            super(value);
        }

        public Cookie asCookie() {
            final BasicClientCookie cookie =
                    new BasicClientCookie("ACSID", this.value);
            cookie.setDomain(Config.SERVER_DOMAIN);
            return cookie;
        }
    }

    /**
     * Thrown when a {@link GoogleAuthToken} or an {@link AcsidToken} we have
     * been using has expired and needs to be renewed.
     */
    @SuppressWarnings("serial")
    public static class AuthenticationTokenExpired extends Exception {
    }

    /**
     * Allows to select the way that will be used to communicate with the user
     */
    public enum AuthUiChoice {
        USE_INTENT, USE_NOTIFICATION
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

    private static final String APPENGINE_TOKEN_TYPE = "ah";

    /**
     * Can be used as an argument to {@link HttpGet#setParams(HttpParams)} to
     * disable redirections
     */
    private static final HttpParams HTTP_PARAMS_NO_REDIRECTIONS =
            (new BasicHttpParams()).setBooleanParameter(
                    ClientPNames.HANDLE_REDIRECTS, false);

    /**
     * Used by {@link #setCachedGoogleAuthToken(GoogleAuthToken)},
     * {@link #clearCachedGoogleAuthToken()} and
     * {@link #getCachedGoogleAuthToken()} to store the latest
     * {@link GoogleAuthToken} in the {@link #sharedPreferences}
     */
    private static final String KEY_NAME_CACHED_GOOGLE_AUTH =
            "cached_google_auth";

    private static final String SERVER_AUTH_URL_PREFIX = Config.SERVER_BASE_URL
        + "/_ah/login?continue=http://localhost/&auth=";

    private static final String SHARED_PREFERENCES_PREFIX = "transceiver-";

    private static String TAG = Config.makeLogTag(Transceiver.class);

    private final Account account;

    private final Context context;

    private final DefaultHttpClient httpClient;

    private final SharedPreferences sharedPreferences;

    public Transceiver(final Account account, final Context context) {
        this.account = account;
        this.context = context;
        this.httpClient = new DefaultHttpClient();
        final String sharedPrefsName = SHARED_PREFERENCES_PREFIX + account.name;
        this.sharedPreferences =
                context.getSharedPreferences(sharedPrefsName,
                        Context.MODE_PRIVATE);
        final PersistentCookieStore cookieStore =
                new PersistentCookieStore(this.sharedPreferences);
        this.httpClient.setCookieStore(cookieStore);
    }

    /**
     * This method may block while a network request completes, and must never
     * be made from the main thread.
     * 
     * @param account
     *            The account to fetch an auth token for
     * @param uiChoice
     *            If {@link AuthUiChoice#USE_NOTIFICATION}, display a
     *            notification and return null if authentication fails; if
     *            {@link AuthUiChoice#USE_INTENT}, prompt and wait for the user
     *            to re-enter correct credentials before returning
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
    private GoogleAuthToken
            blockingGetNewAuthToken(final AuthUiChoice uiChoice)
                    throws OperationCanceledException, AuthenticatorException,
                    IOException {
        final AccountManager am = AccountManager.get(this.context);
        final String authTokenStr =
                am.blockingGetAuthToken(this.account, APPENGINE_TOKEN_TYPE,
                        uiChoice == AuthUiChoice.USE_NOTIFICATION);
        return new GoogleAuthToken(authTokenStr);
    }

    /**
     * Clears the Acsid token we might have had
     */
    public void clearAcsidToken() {
        this.httpClient.getCookieStore().clear();
    }

    /**
     * Clears the cached {@link GoogleAuthToken} stored in the
     * {@link #sharedPreferences}
     */
    private void clearCachedGoogleAuthToken() {
        this.sharedPreferences.edit().remove(KEY_NAME_CACHED_GOOGLE_AUTH)
                .commit();
    }

    /**
     * Releases allocated resources. The instance cannot be used afterward.
     */
    public void dispose() {
        this.httpClient.getConnectionManager().shutdown();
    }


    /**
     * Performs authentication if necessary, then exchange the data with the server and
     * returns the response
     * @param uiChoice
     * @param request
     * @return The response to the request, null if none
     * @throws IOException
     * @throws OperationCanceledException
     * @throws AuthenticatorException
     */
    public GatewayResponseData exchangeWithServer(final AuthUiChoice uiChoice,
     
            final GatewayRequestData request) throws IOException,
            OperationCanceledException, AuthenticatorException {
        final int maxRetries = 10;
        for (int retry = 0; retry < maxRetries; retry++) {
            /* Gets Google Auth Token */
            GoogleAuthToken googleAuthToken = getCachedGoogleAuthToken();
            if (googleAuthToken == null) {
                googleAuthToken = blockingGetNewAuthToken(uiChoice);
                setCachedGoogleAuthToken(googleAuthToken);
                clearAcsidToken();
            }

            /* Promotes to ACSID Token */
            if (!hasAcsidToken())
                try {
                    promoteToken(googleAuthToken);
                }
                catch (final InvalidGoogleAuthTokenException e) {
                    Log.e(TAG,
                            "The google auth token is invalid. Refreshing all cookies. "
                                + e.toString());
                    clearAcsidToken();
                    clearCachedGoogleAuthToken();
                    continue;
                }

            /* Executes the query */
            try {
                return postData(request);
            }
            catch (final AuthenticationTokenExpired e) {
                Log.w(TAG, "Google and/or ACSID tokens expired. Retried "
                    + retry + " times.");
                continue;
            }
        }
        final String failureMessage =
                "Failed to get valid Google and ACSID tokens !";
        Log.e(TAG, failureMessage);
        throw new AuthenticatorException(failureMessage);
    }

    /**
     * Goes through {@link #httpClient}'s cookies to find an {@link AcsidToken}.
     * 
     * @return The token if it finds one, else null
     */
    public AcsidToken getAcsidToken() {
        final List<Cookie> cookies =
                this.httpClient.getCookieStore().getCookies();
        for (final Cookie cookie : cookies)
            if (cookie.getName().equals("ACSID"))
                return new AcsidToken(cookie.getValue());
        return null;
    }

    /**
     * @return the googleAuthToken associated with
     *         {@link #KEY_NAME_CACHED_GOOGLE_AUTH} in the
     *         {@link #sharedPreferences}, null if there is none
     */
    private GoogleAuthToken getCachedGoogleAuthToken() {
        final String resultAsString =
                this.sharedPreferences.getString(KEY_NAME_CACHED_GOOGLE_AUTH,
                        null);
        if (resultAsString == null)
            return null;
        else
            return new GoogleAuthToken(resultAsString);
    }

    /**
     * Goes through {@link #httpClient}'s cookies to find an {@link AcsidToken}.
     * 
     * @return true if it finds one, else false
     */
    public boolean hasAcsidToken() {
        final List<Cookie> cookies =
                this.httpClient.getCookieStore().getCookies();
        for (final Cookie cookie : cookies)
            if (cookie.getName().equals("ACSID"))
                return true;
        return false;
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
        httpRequest.setEntity(requestEntity);
        try {
            final HttpResponse response = this.httpClient.execute(httpRequest);
            final int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 403)
                throw new AuthenticationTokenExpired();
            else if (statusCode != 200)
                throw new IOException("Status code is: " + statusCode);
            else if (response.getEntity() == null)
                return null;
            else {
                responseStream = response.getEntity().getContent();
                return GatewayResponseData.parseFrom(responseStream);
            }
        }
        finally {
            httpRequest.abort();
            if (responseStream != null)
                responseStream.close();
        }
    }

    /**
     * Promotes a {@link GoogleAuthToken} into an {@link AcsidToken} by talking
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

        final HttpGet httpGet =
                new HttpGet(SERVER_AUTH_URL_PREFIX + googleAuthToken.value);
        /* The auth page will redirect to an url we don't care about */
        httpGet.setParams(HTTP_PARAMS_NO_REDIRECTIONS);

        final HttpResponse response = this.httpClient.execute(httpGet);

        if (response.getStatusLine().getStatusCode() == 403) {
            clearAcsidToken();
            throw new InvalidGoogleAuthTokenException(
                    "Token rejected by the server");
        }

        if (!hasAcsidToken()) {
            clearAcsidToken();
            // If no ACSID cookie was passed, it usually means the auth
            // token was invalid;
            throw new InvalidGoogleAuthTokenException(
                    "ACSID cookie not found in HTTP response: "
                        + response.getStatusLine().toString()
                        + "; assuming invalid auth token.");

        }
    }

    /**
     * Associates googleAuthToken with {@link #KEY_NAME_CACHED_GOOGLE_AUTH} in
     * the {@link #sharedPreferences}
     * 
     * @param googleAuthToken
     *            The token to store
     */
    private void
            setCachedGoogleAuthToken(final GoogleAuthToken googleAuthToken) {
        this.sharedPreferences.edit()
                .putString(KEY_NAME_CACHED_GOOGLE_AUTH, googleAuthToken.value)
                .commit();

    }

}
