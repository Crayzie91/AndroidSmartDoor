package smartdoor.utilities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.thingworx.common.utils.XMLUtilities;
import com.thingworx.communications.client.AndroidConnectedThingClient;
import com.thingworx.communications.client.ClientConfigurator;
import com.thingworx.communications.client.things.VirtualThing;

import java.util.List;

/**
 * This class contains generic functions specific to a ThingWorx based Android application.
 * Its intent is to offer an example of how the Android SDK can be used but is not currently
 * part of the SDK itself and may change without warning. It is meant as a starting point
 * for new applications and manages all aspects of your ThingWorx connection. It provides
 * a modal dialog to be displayed during the connection process and also provides optional support
 * for the polling of your VirtualThing's processScanRequest() functions.
 *
 * It assumes that your application defines two shared preferences. These are "prefUri" and
 * "prefAppKey". These must already be established for this class to work. It is assumed that
 * you will make sure these preferences exist before calling the connect() function.
 */
public class ThingworxService extends AppCompatActivity {

    private final String TAG = ThingworxService.class.getName();
    private ProgressDialog connectionProgressDialog;
    private Thread connectionThread;
    private Thread scanningThread;
    private List<String> stateObserverActions;
    private long pollingRate;
    private boolean lastConnectionState = false;

    protected static AndroidConnectedThingClient client;
    protected String uri;
    public String ip;
    protected String port;
    protected String appKey;
    protected SharedPreferences sharedPrefs;
    protected enum ConnectionState {DISCONNECTED, CONNECTING,CONNECTED};
    public enum SettingLocation {PREFERENCES, FRONTEND};

    public ConnectionState getConnectionState() {
        return connectionState;
    }
    public static AndroidConnectedThingClient getClient(){
        return client;
    }

    public static SettingLocation settingState = SettingLocation.PREFERENCES;
    protected static ConnectionState connectionState = ConnectionState.DISCONNECTED;

    protected void registerScanRequestThread(long polling, final List<String> stateObservers) {
        pollingRate = polling;
        stateObserverActions = stateObservers;

        for(final String observer : stateObserverActions) {
            scanningThread = new Thread() {
                public void run() {
                    Intent intent = new Intent(getApplicationContext(), ThingworxProcessService.class);
                    Bundle bundle = new Bundle();

                    bundle.putLong(Constants.POLLING_RATE_EXTRA, pollingRate);
                    bundle.putBoolean(Constants.LAST_CONNECT_STATE_EXTRA, lastConnectionState);
                    bundle.putString(Constants.CONNECTION_OBSERVER, observer);
                    intent.putExtras(bundle);
                    startService(intent);
                }
            };
            scanningThread.start();
        }
    }

    protected boolean hasConnectionPreferences() {
        if(settingState == SettingLocation.FRONTEND)
            return true;

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        ip = sharedPrefs.getString("platform_ip", "");
        port = sharedPrefs.getString("platform_port", "");
        appKey = sharedPrefs.getString("platform_app_key", "");

        if(ip.equals(""))
            return false;
        else
            return true;
    }

    private AndroidConnectedThingClient buildClientFromSettings() throws Exception {
        // Determine if we have settings
        // Determine if we have settings
        if(!hasConnectionPreferences())
            return null;

        ClientConfigurator config = new ClientConfigurator();
        uri = "ws://"+ip+":"+port+"/Thingworx/WS";
        config.setUri(uri);

        /* ReconnectInterval is the max time in seconds waited after a connection is
          dropped before a reconnect attempt is made to the ThingWorx server */
        config.setReconnectInterval(15);

        config.setAppKey(appKey);

        /* ignoreSSLErrors - Accept self signed certs if using wss protocol */
        config.ignoreSSLErrors(true);

        /* How long in milliseconds to wait before giving up establishing a connection. */
        config.setConnectTimeout(10000);
        return new AndroidConnectedThingClient(config);
    }

    protected void connect(VirtualThing[] things) throws Exception {
        if(hasConnectionPreferences()){
            connectionState = ConnectionState.CONNECTING;

            client = buildClientFromSettings();

            for(VirtualThing thing:things) {
                thing.setClient(client); // If you create a thing before you create a client, the client must be set before binding
                client.bindThing(thing); // You can bind before or after a connection is established
            }

            if(connectionThread != null && connectionThread.isAlive()){
                connectionThread.interrupt();
                Thread.sleep(2000);
            }

            connectionThread = new Thread(){
                public void run(){
                    Intent intent = new Intent(getApplicationContext(), ThingworxConnectService.class);
                    intent.putExtra(Constants.IP_EXTRA, ip);
                    startService(intent);
                }
            };

            connectionThread.start();
        } else {
            disconnect();
        }
    }

    protected void disconnect() {
        Log.i(TAG,"disconnect Start");

        if(connectionState != ConnectionState.CONNECTED && connectionState != ConnectionState.CONNECTING)
            return;

        try {
            if(connectionThread != null){
                connectionThread.interrupt();
                Thread.sleep(2000);
            }

            Intent disconnect = new Intent();
            disconnect.setAction(Constants.ACTION_SHUTDOWN);
            sendBroadcast(disconnect);

            connectionState = ConnectionState.DISCONNECTED;
            client.disconnect();
            onDisconnected();
        } catch (Exception e) {
            Log.i(TAG,"Disconnecting.");
        }
        Log.i(TAG,"disconnect End");
    }

    /**
     * Called when a connection to the server is broken or fails to be created.
     */
    protected void onConnectionFailed(String localizedMessage) {
        connectionState = ConnectionState.DISCONNECTED;
        dismissProgressDialog();
        displayErrorDialog(localizedMessage);
    }

    private void displayErrorDialog(final String localizedMessage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(ThingworxService.this)
                        .setTitle("Error")
                        .setMessage(localizedMessage)
                        .setCancelable(false)
                        .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        }).create().show();
            }
        });

    }

    /**
     * Called when a connection is established by the client.
     */
    protected void onConnectionEstablished() {
        connectionState = ConnectionState.CONNECTED;
        connectionProgressDialog.dismiss();

        if(stateObserverActions == null || stateObserverActions.isEmpty())
            return;

        for(final String observer : stateObserverActions) {
            Intent stateChange = new Intent();
            stateChange.setAction(Constants.ACTION_STATE_CHANGED);
            stateChange.putExtra(Constants.CONNECTION_OBSERVER, observer);
            sendBroadcast(stateChange);
        }
    }

    /**
     * Called when a client is disconnected. Perform all state change processes. Override this method for added functionality. Will be called in the {@link #disconnect} method
     */
    protected void onDisconnected() {
        if(stateObserverActions == null || stateObserverActions.isEmpty())
            return;

        for(final String observer : stateObserverActions) {
            Intent stateChange = new Intent();
            stateChange.setAction(Constants.ACTION_STATE_CHANGED);
            stateChange.putExtra(Constants.CONNECTION_OBSERVER, observer);
            sendBroadcast(stateChange);
        }
    }

    /**
     * Shows a dialog indicating that a connection is in progress.
     */
    protected void showProgressDialog() {
        if (connectionProgressDialog == null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connectionProgressDialog = ProgressDialog.show(ThingworxService.this, "Please wait ...", "Connecting to server ...", true);
                    connectionProgressDialog.setCancelable(false);
                }
            });
        }else{
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connectionProgressDialog.show();
                    connectionProgressDialog.setCancelable(false);
                }
            });
        }
    }

    /**
     * Dismisses the connection in progress dialog.
     */
    protected void dismissProgressDialog(){
        if(connectionProgressDialog!=null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(connectionProgressDialog!=null) {
                        connectionProgressDialog.dismiss();
                        connectionProgressDialog = null;
                    }
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Android's parser does not support XXE features
        // for parsed XML documents
        XMLUtilities.ENABLE_XXE = false;
    }

    /**
     * Called when the application is closed.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        dismissProgressDialog();
        Log.d(TAG, "Destroy");
        try {
            client.shutdown();
        } catch (Exception e) {
        }
    }
}
