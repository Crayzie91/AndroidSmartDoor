package smartdoor.utilities;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.thingworx.communications.client.AndroidConnectedThingClient;


public class ThingworxConnectService extends IntentService {
    private final String TAG = ThingworxConnectService.class.getName();

    public ThingworxConnectService() {
        super("ThingworxConnectService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "onHandleIntent Start");

        try {
            Bundle bundle = intent.getExtras();
            AndroidConnectedThingClient client = ThingworxService.client;
            String ip = intent.getStringExtra(Constants.IP_EXTRA);

            Intent showDialog = new Intent();
            showDialog.setAction(Constants.ACTION_PRE_CONNECT);
            sendBroadcast(showDialog);

            // Bind your thing to your connection and start it
            client.start();

            int counter = 0;
            boolean isConnected;
            while (counter < 10) {
                Log.d(TAG, "Waiting for initial connection to " + ip);

                try {
                    isConnected = client.getEndpoint().isConnected();
                    if(isConnected){
                        Intent connectionMade = new Intent();
                        connectionMade.setAction(Constants.ACTION_CONNECTION_ESTABLISHED);
                        sendBroadcast(connectionMade);
                        return;
                    }
                } catch (Exception e) {}
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}

                counter++;
            }

            Intent connectionFailed = new Intent();
            connectionFailed.setAction(Constants.ACTION_CONNECTION_FAILED);
            connectionFailed.putExtra(Constants.CONNECTION_FAILED_MSG, "Timeout exceeded.");
            sendBroadcast(connectionFailed);
        } catch (Exception e) {
            Log.e(TAG, "Failed to connect.",e);
            Intent connectionFailed = new Intent();
            connectionFailed.setAction(Constants.ACTION_CONNECTION_FAILED);
            connectionFailed.putExtra(Constants.CONNECTION_FAILED_MSG, e.getLocalizedMessage());
            sendBroadcast(connectionFailed);
        }
        Log.i(TAG, "onHandleIntent End");
    }
}
