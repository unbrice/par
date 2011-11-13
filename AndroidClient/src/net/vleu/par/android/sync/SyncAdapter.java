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

import net.vleu.par.PlaceHolder;
import net.vleu.par.PlaceHolder.ExchangeWithServerCallack;
import net.vleu.par.PlaceHolder.PlaceHolderException;
import net.vleu.par.android.Config;
import net.vleu.par.android.rpc.Transceiver;
import net.vleu.par.android.rpc.Transceiver.InvalidAuthTokenException;
import net.vleu.par.android.rpc.Transceiver.RequestedUserAuthenticationException;
import net.vleu.par.protocolbuffer.GatewayCommands.GatewayResponseData;

import org.apache.http.auth.AuthenticationException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
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

public final class SyncAdapter extends AbstractThreadedSyncAdapter {
    public static final String DEVICE_TYPE = "android";

    public static final String DM_REGISTERED = "dm_registered";
    public static final String[] GOOGLE_ACCOUNT_REQUIRED_SYNCABILITY_FEATURES =
            new String[] { "service_ah" };

    public static final String GOOGLE_ACCOUNT_TYPE = "com.google";
    public static final String LAST_SYNC = "last_sync";
    public static final String SERVER_LAST_SYNC = "server_last_sync";
    private static final String TAG = Config.makeLogTag(SyncAdapter.class);

    public static void clearSyncData(final Context context) {
        final AccountManager am = AccountManager.get(context);
        final Account[] accounts = am.getAccounts();
        for (final Account account : accounts) {
            final SharedPreferences syncMeta =
                    context.getSharedPreferences("sync:" + account.name, 0);
            syncMeta.edit().clear().commit();
        }
    }

    private final Context context;

    public SyncAdapter(final Context context) {
        super(context, false);
        assert(this.context != null);
        this.context = context;
    }

    private void logErrorMessage(final String message, final boolean showToast) {
        Log.e(TAG, message);

        // Note: in general, showing any form of UI from a service is bad.
        // showToast should only
        // be true if this is a manual sync, i.e. the user has just invoked some
        // UI that indicates she wants to perform a sync.
        final Looper mainLooper = this.context.getMainLooper();
        if (mainLooper != null)
            new Handler(mainLooper).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(SyncAdapter.this.context, message,
                            Toast.LENGTH_SHORT).show();
                }
            });
    }

    @Override
    public void onPerformSync(final Account account, final Bundle extras,
            final String authority, final ContentProviderClient provider,
            final SyncResult syncResult) {
        final boolean uploadOnly =
                extras.getBoolean(ContentResolver.SYNC_EXTRAS_UPLOAD, false);
        final boolean manualSync =
                extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
        final boolean initialize =
                extras.getBoolean(ContentResolver.SYNC_EXTRAS_INITIALIZE, false);
        // TODO: C2DMReceiver.refreshAppC2DMRegistrationState(context);
        Log.i(TAG, "Beginning " + (uploadOnly ? "upload-only" : "full")
            + " sync for account " + account.name);

        // Read this account's sync metadata
        final SharedPreferences syncMeta =
                this.context.getSharedPreferences("sync:" + account.name, 0);
        final long lastSyncTime = syncMeta.getLong(LAST_SYNC, 0);
        final long lastServerSyncTime = syncMeta.getLong(SERVER_LAST_SYNC, 0);

        // Check for changes in either app-wide auto sync registration
        // information, or changes in
        // the user's preferences for auto sync on this account; if either
        // changes, piggy back the
        // new registration information in this sync.
        final long lastRegistrationChangeTime =
                C2DMessaging.getLastRegistrationChange(this.context);

        final boolean autoSyncDesired =
                ContentResolver.getMasterSyncAutomatically()
                    && ContentResolver.getSyncAutomatically(account,
                            PlaceHolder.AUTHORITY);
        final boolean autoSyncEnabled =
                syncMeta.getBoolean(DM_REGISTERED, false);

        // Will be 0 for no change, -1 for unregister, 1 for register.
        final int deviceRegChange;

        if (autoSyncDesired != autoSyncEnabled
            || lastRegistrationChangeTime > lastSyncTime || initialize
            || manualSync) {

            final String registrationId =
                    C2DMessaging.getRegistrationId(this.context);
            deviceRegChange =
                    (autoSyncDesired && registrationId != null) ? 1 : -1;

            if (Log.isLoggable(TAG, Log.DEBUG))
                Log.d(TAG,
                        "Auto sync selection or registration information has changed, "
                            + (deviceRegChange == 1 ? "registering"
                                    : "unregistering")
                            + " messaging for this device, for account "
                            + account.name);

            try {
                if (deviceRegChange == 1)
                    // Register device for auto sync on this account.
                    PlaceHolder.registerDevice();
                else
                    PlaceHolder.unregisterDevice();
            }
            catch (final PlaceHolderException e) {
                logErrorMessage(
                        "Error generating device registration remote RPC parameters.",
                        manualSync);
                syncResult.stats.numIoExceptions++;
                e.printStackTrace();
                return;
            }
        }
        else
            deviceRegChange = 0;

        if (uploadOnly && PlaceHolder.hasNewStuffToUpload()
            && deviceRegChange == 0) {
            Log.i(TAG, "No local changes; upload-only sync canceled.");
            return;
        }

        // Set up the RPC sync calls
        try {
            PlaceHolder.blockingAuthenticateAccount(account, manualSync
                    ? Transceiver.NEED_AUTH_INTENT
                    : Transceiver.NEED_AUTH_NOTIFICATION, false);
        }
        catch (final AuthenticationException e) {
            logErrorMessage(
                    "Authentication exception when attempting to sync.",
                    manualSync);
            e.printStackTrace();
            syncResult.stats.numAuthExceptions++;
            return;
        }
        catch (final OperationCanceledException e) {
            Log.i(TAG, "Sync for account " + account.name
                + " manually canceled.");
            return;
        }
        catch (final RequestedUserAuthenticationException e) {
            syncResult.stats.numAuthExceptions++;
            return;
        }
        catch (final InvalidAuthTokenException e) {
            logErrorMessage(
                    "Invalid auth token provided by AccountManager when attempting to "
                        + "sync.", manualSync);
            e.printStackTrace();
            syncResult.stats.numAuthExceptions++;
            return;
        }
        catch (final Exception e) {
            // TODO: Remove me along with the placeholder
            throw (RuntimeException) e;
        }

        // Set up the notes sync call.

        if (deviceRegChange != 0)
            /* TODO Add a register/unregister to the request */;

        PlaceHolder.exchangeWithServer(null, new ExchangeWithServerCallack() {

            @Override
            public void onError(final int callIndex,
                    final PlaceHolderException /* JsonRpcException */e) {
                /*
                 * if (e.getHttpCode() == 403) { Log.w(TAG,
                 * "Got a 403 response, invalidating App Engine ACSID token");
                 * jsonRpcClient.invalidateAccountAcsidToken(account); }
                 */

                provider.release();
                logErrorMessage("Error calling remote note sync RPC",
                        manualSync);
                e.printStackTrace();
            }

            @Override
            public void onResponse(final GatewayResponseData resp) {
                if (resp != null)
                    // Read notes sync data.
                    try {
                        final long newSyncTime = 0; // TODO
                        final long newServerSyncTime = 0; // TODO
                        syncMeta.edit().putLong(LAST_SYNC, newSyncTime)
                                .commit();
                        syncMeta.edit()
                                .putLong(SERVER_LAST_SYNC, newServerSyncTime)
                                .commit();
                        Log.i(TAG, "Sync complete, setting last sync time to "
                            + Long.toString(newSyncTime));
                    }
                    catch (final PlaceHolderException /* ParseException */e) {
                        logErrorMessage("Error parsing note sync RPC response",
                                manualSync);
                        e.printStackTrace();
                        syncResult.stats.numParseExceptions++;
                        return;
                    }
                    finally {
                        provider.release();
                    }

                // Read device reg data.
                if (deviceRegChange != 0) {
                    // data[1] will be null in case of an error (successful
                    // unregisters
                    // will have an empty JSONObject, not null).
                    final boolean registered =
                            (resp != null && deviceRegChange == 1);
                    syncMeta.edit().putBoolean(DM_REGISTERED, registered)
                            .commit();
                    if (Log.isLoggable(TAG, Log.DEBUG))
                        Log.d(TAG,
                                "Stored account auto sync registration state: "
                                    + Boolean.toString(registered));
                }
            }
        });
    }

}
