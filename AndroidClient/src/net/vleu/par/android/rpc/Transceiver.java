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
import net.vleu.par.PlaceHolder.PlaceHolderException;
import net.vleu.par.WrappedString;
import net.vleu.par.android.Config;
import net.vleu.par.android.rpc.Transceiver.AuthUiChoice;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayRequestData;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayResponseData;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthenticationException;
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
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
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

    public static interface AuthentifyWithUICallback {
        public void onAuthDenied();

        /**
         * Called with the Google auth token when the authentication succeeds
         * 
         * @param authToken
         *            The Google auth token
         */
        public void onHasToken(GoogleAuthToken authToken);

        /**
         * Called when the authenticator failed to respond
         */
        public void onLocalError(AuthenticatorException e);

        /**
         * Called if there is a bug in the {@link Transceiver}'s code
         * 
         * @param e
         *            Its message should give indication about the bug
         */
        public void onLocalError(InternalError e);

        /**
         * Called if the authenticator experienced an I/O problem creating a new
         * auth token, usually because of network trouble
         */
        public void onLocalError(IOException e);
    }

    /**
     * Allows to select the way that will be used to communicate with the user
     */
    public enum AuthUiChoice {
        USE_INTENT, USE_NOTIFICATION
    }

    public static interface ExchangeWithServerCallback {

        /**
         * Called when the authenticator failed to respond
         */
        public void onLocalError(AuthenticatorException e);

        /**
         * Called if there is a bug in the {@link Transceiver}'s code
         * 
         * @param e
         *            Its message should give indication about the bug
         */
        public void onLocalError(InternalError e);

        /**
         * Called if the request was canceled by the user
         */
        public void onLocalError(OperationCanceledException e);

        /**
         * Called if the authenticator experienced an I/O problem creating a new
         * auth token, usually because of network trouble
         */
        public void onNetworkError(IOException e);

        public void onServerError(GatewayRequestData request,
                PlaceHolderException e);

        public void onServerResponse(GatewayRequestData request,
                GatewayResponseData response);

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
     * Thrown by
     * {@link Transceiver#blockingAuthenticateAccount(Account, AuthUiChoice, boolean)}
     * when the authenticating the token requires an user interaction.
     * 
     * It means that the authentication should be retried later, possibly after
     * the user answered.
     */
    @SuppressWarnings("serial")
    public static class RequestedUserAuthenticationException extends Exception {
    }

    private static final String APPENGINE_TOKEN_TYPE = "ah";

    /**
     * Can be used as an argument to {@link HttpGet#setParams(HttpParams)} to
     * disable redirections
     */
    private static final HttpParams HTTP_PARAMS_NO_REDIRECTIONS =
            (new BasicHttpParams()).setBooleanParameter(
                    ClientPNames.HANDLE_REDIRECTS, false);

    private static final String SERVER_AUTH_URL_PREFIX = Config.SERVER_BASE_URL
        + "/_ah/login?continue=http://localhost/&auth=";

    private static String TAG = Config.makeLogTag(Transceiver.class);

    /**
     * Gets an auth token for a particular account, prompting the user for
     * credentials if necessary. This method is intended for applications
     * running in the foreground where it makes sense to ask the user directly
     * for a password.
     * 
     * @param account
     *            The account to fetch an auth token for
     * @param activity
     *            The Activity context to use for launching a new
     *            authenticator-defined sub-Activity to prompt the user for a
     *            password if necessary; used only to call startActivity(); must
     *            not be null.
     * @param callback
     *            Callback to invoke when the request completes
     */
    private static void authentifyWithUiIfNeeded(final Account account,
            final Activity activity, final AuthentifyWithUICallback callback) {
        final AccountManager am = AccountManager.get(activity);
        final AccountManagerCallback<Bundle> amCallback =
                new AccountManagerCallback<Bundle>() {
                    @Override
                    public void run(
                            final AccountManagerFuture<Bundle> bundleFuture) {
                        final Bundle authBundle;
                        try {
                            authBundle = bundleFuture.getResult();
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
                            callback.onHasToken(new GoogleAuthToken(
                                    (String) authBundle
                                            .get(AccountManager.KEY_AUTHTOKEN)));
                        else
                            callback.onError(new InternalError(
                                    "No auth token available, but operation not canceled."));
                    }
                };
        am.getAuthToken(account, APPENGINE_TOKEN_TYPE, null, activity,
                amCallback, null);
    }

    private final Account account;

    private final Context context;

    private final DefaultHttpClient httpclient;

    public Transceiver(final Account account, final Context context) {
        this.account = account;
        this.context = context;
        this.httpclient = new DefaultHttpClient();
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
        this.httpclient.getCookieStore().clear();
    }

    /**
     * Releases allocated resources. The instance cannot be used afterward.
     */
    public void dispose() {
        this.httpclient.getConnectionManager().shutdown();
    }

    public GatewayResponseData exchangeWithServer(final AuthUiChoice uiChoice,
            final GatewayRequestData request) throws IOException,
            OperationCanceledException, AuthenticatorException {

        /* Get Google Auth Token */
        final GoogleAuthToken googleAuthToken =
                blockingGetNewAuthToken(uiChoice);

        /* Promote to ACSID Token */
        try {
            promoteTokenIfNoCookie(googleAuthToken);
        }
        catch (final InvalidGoogleAuthTokenException e) {
            final String message =
                    "blockingGetNewAuthToken returned an invalid token: "
                        + e.toString();
            Log.e(TAG, message);
            throw new InternalError(message);
        }

        return postData(request);
    }

    /**
     * Goes through {@link #httpclient}'s cookies to find an {@link AcsidToken}.
     * 
     * @return The token if it finds one, else null
     */
    public AcsidToken getAcsidToken() {
        final List<Cookie> cookies =
                this.httpclient.getCookieStore().getCookies();
        for (final Cookie cookie : cookies)
            if (cookie.getName().equals("ACSID"))
                return new AcsidToken(cookie.getValue());
        return null;
    }

    /**
     * Goes through {@link #httpclient}'s cookies to find an {@link AcsidToken}.
     * 
     * @return true if it finds one, else false
     */
    public boolean hasAcsidToken() {
        final List<Cookie> cookies =
                this.httpclient.getCookieStore().getCookies();
        for (final Cookie cookie : cookies)
            if (cookie.getName().equals("ACSID"))
                return true;
        return false;
    }

    private GatewayResponseData postData(final GatewayRequestData request)
            throws IOException {
        HttpPost httpRequest = null;
        InputStream responseStream = null;
        final ByteArrayEntity requestEntity =
                new ByteArrayEntity(request.toByteArray());
        try {
            // Setup the client
            httpRequest = new HttpPost(Config.SERVER_RPC_URL);
            httpRequest.setEntity(requestEntity);

            // Execute HTTP Post Request
            final HttpResponse response = this.httpclient.execute(httpRequest);
            if (response.getEntity() == null)
                return null;
            else {
                responseStream = response.getEntity().getContent();
                return GatewayResponseData.parseFrom(responseStream);
            }
        }
        finally {
            if (httpRequest != null)
                httpRequest.abort();
            if (responseStream != null)
                responseStream.close();
        }
    }

    /**
     * If {{@link #hasAcsidToken()} returns true does nothing. Else, promotes a
     * {@link GoogleAuthToken} into an {@link AcsidToken} by talking with the
     * AppEngine server and adds this token to {@link #httpclient}'s cookie jar.
     * 
     * @param account
     *            The account to which the {@link GoogleAuthToken} is associated
     * @param googleAuthToken
     *            Associated to the {@link Account}
     * @throws AuthenticationException
     * @throws InvalidGoogleAuthTokenException
     *             The token was rejected by the server
     * @throws IOException
     */
    private void promoteTokenIfNoCookie(final GoogleAuthToken googleAuthToken)
            throws IOException, InvalidGoogleAuthTokenException {

        if (hasAcsidToken())
            return;

        final HttpGet httpGet =
                new HttpGet(SERVER_AUTH_URL_PREFIX + googleAuthToken.value);
        /* The auth page will redirect to an url we don't care about */
        httpGet.setParams(HTTP_PARAMS_NO_REDIRECTIONS);

        final HttpResponse response = this.httpclient.execute(httpGet);

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

}
