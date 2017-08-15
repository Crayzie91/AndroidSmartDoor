package smartdoor.utilities;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.Nullable;
import android.util.Log;

import com.thingworx.communications.client.AndroidConnectedThingClient;
import com.thingworx.communications.client.things.VirtualThing;


public class ThingworxProcessService extends Service {
    public final String TAG = ThingworxProcessService.class.getName();

    private Looper mServiceLooper;
    private ServiceHandler serviceHandler;
    private AndroidConnectedThingClient client;
    private IntentServiceReceiver serviceReceiver;

    @Override
    public void onCreate() {
        serviceReceiver = new IntentServiceReceiver();
        HandlerThread thread = new HandlerThread("ThingworxProcessService", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job

        Message msg = serviceHandler.obtainMessage();
        msg.setData(intent.getExtras());
        msg.arg1 = startId;
        serviceHandler.sendMessage(msg);

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onDestroy() {
        unregisterReceiver(serviceReceiver);
        super.onDestroy();
    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "handleMessage Start");

            try {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(Constants.ACTION_SHUTDOWN);
                registerReceiver(serviceReceiver, intentFilter);

                Bundle bundle = msg.getData();
                long pollingRate = bundle.getLong(Constants.POLLING_RATE_EXTRA, 1000l);
                boolean lastConnectionState = bundle.getBoolean(Constants.LAST_CONNECT_STATE_EXTRA);
                String stateObserver = bundle.getString(Constants.CONNECTION_OBSERVER);
                client = ThingworxService.client;

                boolean isConnected;

                while (client == null || !client.isShutdown()) {
                    if (client != null && client.isConnected()) {
                        isConnected = true;

                        for (VirtualThing thing : client.getThings().values()) {
                            try {
                                Thread.sleep(15000);
                                Log.v(TAG, "Scanning device");
                                thing.processScanRequest();
                            } catch (Exception eProcessing) {
                                Log.e(TAG, "Error Processing Scan Request for [" + thing.getName() + "] : " + eProcessing.getMessage());
                            }
                        }

                        Intent scanned = new Intent();
                        scanned.setAction(Constants.ACTION_SCAN_COMPLETE);
                        sendBroadcast(scanned);
                    } else {
                        isConnected = false;
                        Thread.sleep(15000);
                    }

                    if(isConnected != lastConnectionState){
                        if(stateObserver != null) {
                            Intent stateChange = new Intent();

                            stateChange.setAction(Constants.ACTION_STATE_CHANGED);
                            stateChange.putExtra(Constants.CONNECTION_OBSERVER, stateObserver);
                            sendBroadcast(stateChange);

                            lastConnectionState = isConnected;
                        }
                    }

                    Thread.sleep(pollingRate);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Polling thread exiting." + e.getMessage());
            }

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1);
            Log.i(TAG, "handleMessage End");
        }
    }

    public class IntentServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(action.equals(Constants.ACTION_SHUTDOWN)){
                if(client != null && client.isConnected())
                    try {
                        client.shutdown();
                    } catch (Exception e) {
                        Log.e(TAG, "Error Shutting Down Client", e);
                    }
            }
        }
    }
}
