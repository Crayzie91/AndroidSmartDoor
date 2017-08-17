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
import android.widget.TextView;


import smartdoor.preferences.SettingsActivity;
import smartdoor.utilities.Constants;
import smartdoor.utilities.ThingworxService;


public class ClientDetailActivity extends AppCompatActivity {
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
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
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
                startActivityForResult(i, 1);
                break;
            case R.id.action_clientlist:
                i = new Intent(this, ClientListActivity.class);
                startActivityForResult(i, 1);
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
