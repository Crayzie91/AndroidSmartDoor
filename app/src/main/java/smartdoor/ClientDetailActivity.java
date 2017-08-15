package smartdoor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;


import smartdoor.utilities.Constants;
import smartdoor.utilities.ThingworxService;


public class ClientDetailActivity extends ThingworxService {
    protected static final String TAG = ClientDetailActivity.class.getName();
    Bundle info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_detail);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        info = intent.getExtras();

        StringBuilder detail = null;

        detail = new StringBuilder("Name: " + info.get("ClientName") +"\n\n");
        detail.append("Description: " + info.get("ClientDescription") +"\n\n");
        detail.append("Distance: " + info.get("ClientDistance") +" cm\n\n");
        detail.append("LastEntered: " + info.get("ClientLastEntered")+"\n\n");
        detail.append("DoorStatus: " + info.get("ClientDoorStatus") + "\n\n");
        detail.append("ID: " + info.get("ClientID")+"\n\n");
        detail.append("Location: " + info.get("ClientLocation")+"\n");

        ((TextView) findViewById(R.id.client_detail)).setText(detail);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            navigateUpTo(new Intent(this, ClientListActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
