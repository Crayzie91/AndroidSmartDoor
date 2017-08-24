package smartdoor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import com.thingworx.communications.client.things.VirtualThing;
import com.thingworx.relationships.RelationshipTypes;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.StringPrimitive;

import smartdoor.content.AndroidThing;
import smartdoor.utilities.ThingworxService;


public class ClientDetailActivity extends ThingworxService {
    ValueCollection info = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_detail);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        final String name = intent.getStringExtra("ClientName");

        try {
            if (!client.isConnected()){
                disconnect();
                connect(new VirtualThing[]{client.getThing("AndroidThing")});}
            info = getClient().invokeService(RelationshipTypes.ThingworxEntityTypes.Things, name, "GetPropertyValues", new ValueCollection(), 10000).getLastRow();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(info!=null) {
            StringBuilder detail = null;
            detail = new StringBuilder("Name: " + info.get("name") + "\n");
            detail.append("Description: " + info.get("description") + "\n");
            detail.append("Distance: " + info.get("Distance").getValue() + " cm\n");
            detail.append("LastEntered: " + info.get("LastEntered") + "\n");
            detail.append("DoorStatus: " + info.get("DoorStatus") + "\n");
            detail.append("ID: " + info.get("ID").getValue() + "\n");
            detail.append("Location: " + info.get("Location") + "\n");

            ((TextView) findViewById(R.id.client_detail)).setText(detail);
        }

        Button openButton = (Button) findViewById(R.id.opendoor_button);
        openButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    ValueCollection payload = new ValueCollection();
                    payload.put("Status", new StringPrimitive("Open"));
                    client.invokeService(RelationshipTypes.ThingworxEntityTypes.Things, name, "remoteDoor", payload, 10000);
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        Button historyButton = (Button) findViewById(R.id.history_button);
        historyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent i = new Intent(getApplicationContext(), HistoryListActivity.class);
                    i.putExtra("name",info.get("name").toString());
                    startActivityForResult(i,1);
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (!client.isConnected()){
                AndroidThing thing = (AndroidThing) client.getThing("AndroidThing");
                disconnect();
                connect(new VirtualThing[]{thing});}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.detailmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        switch (item.getItemId()) {
            // action with ID action_settings was selected
            case R.id.action_main:
                i = new Intent(this, MainActivity.class);
                startActivityForResult(i,1);
                break;
            case R.id.action_clientlist:
                i = new Intent(this, ClientListActivity.class);
                startActivityForResult(i,1);
                break;
            case android.R.id.home:
                navigateUpTo(new Intent(this, ClientListActivity.class));
                break;
            default:
                break;
        }
        return true;
    }
}
