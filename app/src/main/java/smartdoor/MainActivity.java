package smartdoor;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.thingworx.communications.client.things.VirtualThing;

import smartdoor.content.AndroidThing;
import smartdoor.preferences.SettingsActivity;
import smartdoor.utilities.Constants;
import smartdoor.utilities.ThingworxConnectService;
import smartdoor.utilities.ThingworxService;

import com.thingworx.relationships.RelationshipTypes;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.StringPrimitive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;

/**
 * This class creates an Activity that manages one or more VirtualThings. It gets all of its
 * ThingWorx specific features from its base class and focuses on the creation of your
 * virtual things. It also
 * provides a generic settings UI to configure the ThingWorx client connection.
 * Test
 */
public class MainActivity extends ThingworxService {

    private static final Logger LOG = LoggerFactory.getLogger(MainActivity.class);
    public static final int POLLING_RATE = 1000;
    public static final int SETTING_CODE = 100;
    public static final int NOTIFICATION_CODE = 001;

    private AndroidThing thing;
    private final String ThingName = "AndroidThing";
    private static boolean firstConnected = false, created = false;

    // UI references
    private EditText mIPView;
    private EditText mPortView;
    private EditText mAppKeyView;
    private CheckBox checkBoxConnected;
    private MainActivityReceiver mainReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Build User Interface
        setContentView(R.layout.activity_main);
        mIPView = (EditText) findViewById(R.id.ip);
        checkBoxConnected = (CheckBox) findViewById(R.id.checkBoxConnected);
        mPortView = (EditText) findViewById(R.id.port);
        mAppKeyView = (EditText) findViewById(R.id.appKey);

        mIPView.setText("34.227.165.169");
        mPortView.setText("80");
        mAppKeyView.setText("ce22e9e4-2834-419c-9656-ef9f844c784c");

        Button connectButton = (Button) findViewById(R.id.connect_button);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if(connectionState == ConnectionState.CONNECTED)
                        disconnect();

                    String newIp = mIPView.getText().toString();
                    String newPort = mPortView.getText().toString();
                    String newAppKey = mAppKeyView.getText().toString();

                    if(TextUtils.isEmpty(newIp) && TextUtils.isEmpty(newPort) && TextUtils.isEmpty(newAppKey)){
                        settingState = SettingLocation.PREFERENCES;
                    }
                    else{
                        settingState = SettingLocation.FRONTEND;
                    }

                    if (!TextUtils.isEmpty(newIp)) {
                        ip = newIp;
                    }
                    if (!TextUtils.isEmpty(newPort)) {
                        port = newPort;
                    }
                    if (!TextUtils.isEmpty(newAppKey)) {
                        appKey = newAppKey;
                    }

                    if (!hasConnectionPreferences()) {
                        // Show Preferences Activity
                        connectionState = ConnectionState.DISCONNECTED;
                        Intent i = new Intent(getApplicationContext(),SettingsActivity.class);
                        startActivityForResult(i, SETTING_CODE);
                        return;
                    }

                    // Create AndroidThing and bind it to the client
                    try {
                        //If thing doesn't exist on platform yet the connection is established to an
                        //unbound thing to access platform services.
                        //Create real thing when connection is established.
                        thing= new AndroidThing(ThingName, "A basic android thing.", client, getApplicationContext());
                        connect(new VirtualThing[]{thing});

                        // Setup observer
                        List<String> observers = new ArrayList<>();
                        observers.add("UPDATE_CHECK_BOX");
                        registerScanRequestThread(POLLING_RATE, observers);
                    } catch (Exception e) {
                        LOG.error("Failed to initialize with error.", e);
                        onConnectionFailed("Failed to initialize with error : " + e.getMessage());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                firstConnected=true;
            }
        });

        Button discButton = (Button) findViewById(R.id.disconnect_button);
        discButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ValueCollection payload = new ValueCollection();
                try {
                    if(connectionState == ConnectionState.CONNECTED)
                        payload.put("name", new StringPrimitive(ThingName));
                    disconnect();
                    created=false;
                    checkBoxConnected.setChecked(created);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        Button setButton = (Button) findViewById(R.id.settings_button);
        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if(connectionState == ConnectionState.CONNECTED)
                        disconnect();
                    Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
                    startActivityForResult(i, SETTING_CODE);
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        Button testButton = (Button) findViewById(R.id.test_button);
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    thing.setClientProperty("count", 99);
                    thing.processScanRequest();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(getConnectionState() == ConnectionState.DISCONNECTED && hasConnectionPreferences()) {
            try {
                connect(new VirtualThing[]{thing});
            } catch (Exception e) {
                LOG.error("Restart with new settings failed.", e);
                e.printStackTrace();
            }
        } else {
            checkBoxConnected.setChecked(created);
        }
    }

    @Override
    protected void onStart() {
        mainReceiver = new MainActivityReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_PRE_CONNECT);
        intentFilter.addAction(Constants.ACTION_CONNECTION_ESTABLISHED);
        intentFilter.addAction(Constants.ACTION_CONNECTION_FAILED);
        intentFilter.addAction(Constants.ACTION_STATE_CHANGED);
        registerReceiver(mainReceiver, intentFilter);
        super.onStart();
    }

    @Override
    protected void onStop() {
        unregisterReceiver(mainReceiver);
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        switch (item.getItemId()) {
            // action with ID action_settings was selected
            case R.id.action_clientlist:
                i = new Intent(this, ClientListActivity.class);
                startActivity(i);
                break;
            case R.id.action_settings:
                if(connectionState == ConnectionState.CONNECTED)
                    disconnect();
                i = new Intent(this, SettingsActivity.class);
                startActivityForResult(i, SETTING_CODE);
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * This function will be called from the base class to allow you to set
     * values on your virtual thing that are not configured in your aspect defaults or to perform
     * any other UI changes in response to becoming connected to the server.
     */
    @Override
    protected void onConnectionEstablished() {
        super.onConnectionEstablished();
        try {
            if(!created) {
                //The unbound thing invokes all necessarry services to create an AndroidThing
                createAndroidThing();
                //After a disconnect a connection to the real AndroidThing is established
                disconnect();
                connect(new VirtualThing[]{thing});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onConnectionFailed(String localizedMessage) {
        super.onConnectionFailed(localizedMessage);

        Intent intent = new Intent(getApplicationContext(), ThingworxConnectService.class);
        stopService(intent);
    }

    /**
     * This function creates a new AndroidThing on the Thingworx platform.
     * If a Thing already exists (because it couldn't be deleted properly
     * the exisitng thing will be connected.
     *
     * @return True if execution was successful
     */
    private boolean createAndroidThing(){
        try{
            //Build ValueCollection of parameters
            ValueCollection payload = new ValueCollection();
            payload.put("name", new StringPrimitive(ThingName));
            payload.put("description", new StringPrimitive("Remote created AndroidThing"));
            payload.put("thingTemplateName", new StringPrimitive("AndroidThingTemplate"));

            try {
                //Call CreateThing Service from the platforms EntityServices
                LOG.info(ThingName + " was created.");
                client.invokeService(RelationshipTypes.ThingworxEntityTypes.Resources, "EntityServices", "CreateThing", payload, 10000);
            } catch (Exception e){
                LOG.info(ThingName+" couldn't be created. Probably the thing already exists. "+e);
            }

            //Enable and restart thing to set it active
            client.invokeService(RelationshipTypes.ThingworxEntityTypes.Things, ThingName, "EnableThing", payload, 10000);
            client.invokeService(RelationshipTypes.ThingworxEntityTypes.Things, ThingName, "RestartThing", payload, 10000);

            thing=null;
            client.unbindAllThings();
            thing = new AndroidThing(ThingName, "A virtual thing", client, getApplicationContext());
            client.bindThing(thing);
            created = true;
            return true;
        }catch (Exception e) {
            LOG.error("An exception occurred while creating new Thing", e);
            e.printStackTrace();
            return false;
        }
    }

    public class MainActivityReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(action.equals(Constants.ACTION_PRE_CONNECT)){
                showProgressDialog();
            }else if(action.equals(Constants.ACTION_CONNECTION_ESTABLISHED)){
                onConnectionEstablished();
            }else if(action.equals(Constants.ACTION_CONNECTION_FAILED)){
                String errorMsg = intent.getStringExtra(Constants.CONNECTION_FAILED_MSG);
                onConnectionFailed(errorMsg);
            }else if(action.equals(Constants.ACTION_STATE_CHANGED)){
                String process = intent.getStringExtra(Constants.CONNECTION_OBSERVER);

                if(!TextUtils.isEmpty(process) && process.equalsIgnoreCase("UPDATE_CHECK_BOX")) {
                    final boolean isConnected = client.isConnected();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            checkBoxConnected.setChecked(isConnected);
                        }
                    });
                }
            }
        }
    }
}
