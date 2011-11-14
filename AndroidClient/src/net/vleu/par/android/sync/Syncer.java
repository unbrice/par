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

import java.util.List;

import net.vleu.par.PlaceHolder;
import net.vleu.par.PlaceHolder.ExchangeWithServerCallback;
import net.vleu.par.PlaceHolder.PlaceHolderException;
import net.vleu.par.android.Config;
import net.vleu.par.android.DirectivesExecutor;
import net.vleu.par.android.preferences.Preferences;
import net.vleu.par.android.rpc.Transceiver;
import net.vleu.par.android.rpc.Transceiver.InvalidAuthTokenException;
import net.vleu.par.android.rpc.Transceiver.RequestedUserAuthenticationException;
import net.vleu.par.protocolbuffer.Commands.DirectiveData;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayRequestData;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayResponseData;

import org.apache.http.auth.AuthenticationException;

import android.accounts.Account;
import android.accounts.OperationCanceledException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.android.c2dm.C2DMessaging;

/**
 * Instantiated by the {@link SyncAdapter}, performs the synchronization
 */
final class Syncer {
    private final class MyExchangeWithServerCallback implements
            ExchangeWithServerCallback {
        private final String c2dmToken;

        public MyExchangeWithServerCallback(final String c2dmToken) {
            this.c2dmToken = c2dmToken;
        }

        @Override
        public void onServerError(final GatewayRequestData request,
                final PlaceHolderException e) {

        }

        @Override
        public void onServerResponse(final GatewayRequestData request,
                final GatewayResponseData resp) {
            applyDirectives(resp.getDirectiveList());
            setLastSentC2dmToken(this.c2dmToken);
            setLastSyncTimeToNow();
        }
    }

    public static final class SynchronizationParameters {
        private final Bundle bundle;

        public SynchronizationParameters() {
            this(new Bundle());
        }

        public SynchronizationParameters(final Bundle bundle) {
            this.bundle = bundle;
        }

        public Bundle getBundle() {
            return this.bundle;
        }

        /**
         * @see ContentResolver.SYNC_EXTRAS_INITIALIZE
         */
        public boolean getInitialize() {
            return this.bundle.getBoolean(
                    ContentResolver.SYNC_EXTRAS_INITIALIZE, false);
        }

        /**
         * @see ContentResolver.SYNC_EXTRAS_MANUAL
         */
        public boolean getManualSync() {
            return this.bundle.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL,
                    false);
        }

        /**
         * @see ContentResolver.SYNC_EXTRAS_UPLOAD
         */
        public boolean getUploadOnly() {
            return this.bundle.getBoolean(ContentResolver.SYNC_EXTRAS_UPLOAD,
                    false);
        }

        /**
         * @see ContentResolver.SYNC_EXTRAS_INITIALIZE
         */
        public void setInitialize(final boolean value) {
            this.bundle.putBoolean(ContentResolver.SYNC_EXTRAS_INITIALIZE,
                    value);
        }

        /**
         * @see ContentResolver.SYNC_EXTRAS_MANUAL
         */
        public void setManualSync(final boolean value) {
            this.bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, value);
        }

        /**
         * @see ContentResolver.SYNC_EXTRAS_UPLOAD
         */
        public void setUploadOnly(final boolean value) {
            this.bundle.putBoolean(ContentResolver.SYNC_EXTRAS_UPLOAD, value);
        }
    }

    /**
     * The account name will be appended by
     * {@link Syncer#getLastC2dmTokenKey(Account)}
     */
    private static final String KEY_PREFIX_LAST_C2DM_TOKEN = "last_c2dm_token-";

    /**
     * The account name will be appended by
     * {@link Syncer#getLastSyncMsKey(Account)}
     */
    private static final String KEY_PREFIX_LAST_SYNC_MS = "last_sync_ms-";

    private static final String METADATA_PREFERENCE_FILE = "SyncAdpterMetadata";

    private static final String TAG = Config.makeLogTag(Syncer.class);

    /**
     * Register or unregister based on phone sync settings. Called on each
     * performSync by the SyncAdapter.
     */
    private static void refreshAppC2DMRegistrationState(final Context context) {
        // Determine if there are any auto-syncable accounts. If there are, make
        // sure we are
        // registered with the C2DM servers. If not, unregister the application.
        final boolean autoSyncEnabled =
                SynchronizationSettings.isAutoSyncDesired(context);

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

    /**
     * @param account
     *            Must be of type {@value Config#GOOGLE_ACCOUNT_TYPE}
     */
    private final Account account;

    private final Context context;

    /**
     * @value {@link #KEY_PREFIX_LAST_C2DM_TOKEN} + this.account.name
     */
    private final String keyForLastC2dmTokenKey;

    /**
     * @value {@link #KEY_PREFIX_LAST_SYNC_MS} + this.account.name
     */
    private final String keyForLastSyncMs;

    private final Syncer.SynchronizationParameters parameters;

    private final Preferences preferences;

    /**
     * 
     * @param account
     *            Must be of type {@value Config#GOOGLE_ACCOUNT_TYPE}
     */
    public Syncer(final Account account, final Context context,
            final Syncer.SynchronizationParameters parameters,
            final Preferences preferences) {
        if (!account.type.equals(Config.GOOGLE_ACCOUNT_TYPE))
            throw new IllegalArgumentException("Invalid account type: "
                + account.type);
        this.account = account;
        this.context = context;
        this.keyForLastSyncMs = KEY_PREFIX_LAST_SYNC_MS + account.name;
        this.keyForLastC2dmTokenKey = KEY_PREFIX_LAST_C2DM_TOKEN + account.name;
        this.parameters = parameters;
        this.preferences = preferences;
    }

    private void applyDirectives(final List<DirectiveData> directives) {
        final DirectivesExecutor executor =
                new DirectivesExecutor(this.context);
        for (final DirectiveData directive : directives)
            executor.execute(directive);
    }

    /**
     * @return The C2DM token registered the last time the account was synced
     */
    private String getLastSentC2dmToken() {
        return getSyncMetadata().getString(this.keyForLastC2dmTokenKey, null);
    }

    /**
     * @return The last time the account was synced
     */
    private long getLastSyncTimeMs() {
        return getSyncMetadata().getLong(this.keyForLastSyncMs, 0);
    }

    /**
     * @return A {@link SharedPreferences} used to store synchronization
     *         metadata
     */
    private SharedPreferences getSyncMetadata() {
        return this.context.getSharedPreferences(METADATA_PREFERENCE_FILE,
                Context.MODE_PRIVATE);
    }

    /**
     * @return whether preferences have been modified since last upload for this
     *         account
     */
    private boolean localDataChangedSinceLastSync() {
        if (!C2DMessaging.getRegistrationId(this.context).equals(
                getLastSentC2dmToken()))
            return true;
        else if (getLastSyncTimeMs() < this.preferences.getLastUpdateTimeMs())
            return true;
        else
            return false;
    }

    /**
     * Logs the message as an error, displays it if the synchronization is
     * manual
     * 
     * @param message
     *            The message to display
     */
    private void logOrDisplayErrorMessage(final String message) {
        Log.e(TAG, message);

        // Note: in general, showing any form of UI from a service is bad.
        if (this.parameters.getManualSync()) {
            final Looper mainLooper = this.context.getMainLooper();
            if (mainLooper != null)
                new Handler(mainLooper).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(Syncer.this.context, message,
                                Toast.LENGTH_SHORT).show();
                    }
                });
        }
    }

    /**
     * @param account
     *            The account to synchronize
     * @param parameters
     *            As described in {@link SynchronizationParameters}
     * @throws Exception
     */
    public void performSynchronization(final SyncResult syncResult) {
        final String c2dmToken =
                C2DMessaging.getRegistrationId(Syncer.this.context);
        final GatewayRequestData.Builder requestBuilder =
                GatewayRequestData.newBuilder();
        final MyExchangeWithServerCallback calledAfterExchange =
                new MyExchangeWithServerCallback(c2dmToken);

        refreshAppC2DMRegistrationState(this.context);

        if (this.parameters.getUploadOnly())
            if (localDataChangedSinceLastSync())
                PlaceHolder.addDeviceRegistrationToRequest(requestBuilder);
            else
                return;
        else
            PlaceHolder.addGetDirectiveToRequest(requestBuilder);

        try {
            PlaceHolder.blockingAuthenticateAccount(this.account,
                    this.parameters.getManualSync()
                            ? Transceiver.NEED_AUTH_INTENT
                            : Transceiver.NEED_AUTH_NOTIFICATION, false);
        }
        catch (final AuthenticationException e) {
            logOrDisplayErrorMessage("Authentication exception when attempting to sync.");
            e.printStackTrace();
            syncResult.stats.numAuthExceptions++;
            return;
        }
        catch (final OperationCanceledException e) {
            Log.i(TAG, "Sync for account " + this.account.name
                + " manually canceled.");
            return;
        }
        catch (final RequestedUserAuthenticationException e) {
            syncResult.stats.numAuthExceptions++;
            return;
        }
        catch (final InvalidAuthTokenException e) {
            logOrDisplayErrorMessage("Invalid auth token provided by AccountManager when attempting to sync.");
            e.printStackTrace();
            syncResult.stats.numAuthExceptions++;
            return;
        }
        catch (final Exception e) {
            // TODO: Remove me along with the placeholder
            throw (RuntimeException) e;
        }

        PlaceHolder.exchangeWithServer(requestBuilder.build(),
                calledAfterExchange);

    }

    /**
     * Updates the C2DM token of the last synchronization
     * 
     * @param value
     *            The C2DM token registered the last time the account was synced
     */
    private void setLastSentC2dmToken(final String value) {
        // TODO: Use apply
        getSyncMetadata().edit().putString(this.keyForLastC2dmTokenKey, value)
                .commit();
    }

    /**
     * Updates the timestamp of the last synchronization to the current time
     */
    private void setLastSyncTimeToNow() {
        final long timeStampMs = System.currentTimeMillis();
        // TODO: Use apply
        getSyncMetadata().edit().putLong(this.keyForLastSyncMs, timeStampMs)
                .commit();
    }

}
