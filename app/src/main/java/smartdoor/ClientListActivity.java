package smartdoor;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.thingworx.relationships.RelationshipTypes;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import smartdoor.preferences.SettingsActivity;
import smartdoor.utilities.ThingworxService;

public class ClientListActivity extends ThingworxService {

    private static final Logger LOG = LoggerFactory.getLogger(ClientListActivity.class);

    private InfoTable info = null;
    private ValueCollection valC = null;

    private Map<Number, Number> ClientList = new HashMap<Number, Number>();
    private ArrayList<String> listItems = new ArrayList<String>();
    private ArrayAdapter<String> adapter;
    private ListView list;
    private boolean wide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_list);

        if (findViewById(R.id.client_detail) != null) {
            wide = true;
        }

        list = (ListView) findViewById(R.id.client_list);

        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                listItems);
        list.setAdapter(adapter);

        try {
            populateListView();
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("ListView couldn't be populated.");
            Intent i = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(i);

            if (client.getThings().isEmpty())
                Toast.makeText(getApplicationContext(), "App not connected to platform.", Toast.LENGTH_LONG).show();
            else
                Toast.makeText(getApplicationContext(), "Client list couldn't be populated. Propably no clients connected.", Toast.LENGTH_LONG).show();
        }

        adapter.notifyDataSetChanged();

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    valC = client.invokeService(RelationshipTypes.ThingworxEntityTypes.Things, "ClientThing_" + ClientList.get((int) id), "GetPropertyValues", new ValueCollection(), 10000).getLastRow();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (wide) {
                    populateDetailList(valC);
                } else {
                    callListDetailActivity(valC.getStringValue("name"));
                }
            }
        });

        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {
                ValueCollection info = null;
                try {
                    valC = client.invokeService(RelationshipTypes.ThingworxEntityTypes.Things, "ClientThing_" + ClientList.get((int) id), "GetPropertyValues", new ValueCollection(), 10000).getLastRow();
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }

                callListDetailActivity(valC.getStringValue("name"));
                return true;
            }
        });

        if (!hasConnectionPreferences()) {
            connectionState = ConnectionState.DISCONNECTED;
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
            return;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        ClientList.isEmpty();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setContentView(R.layout.activity_client_list);

        if (findViewById(R.id.client_detail) != null) {
            wide = true;
        }

        list = (ListView) findViewById(R.id.client_list);

        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                listItems);
        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    valC = client.invokeService(RelationshipTypes.ThingworxEntityTypes.Things, "ClientThing_" + ClientList.get((int) id), "GetPropertyValues", new ValueCollection(), 10000).getLastRow();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (wide) {
                    populateDetailList(valC);
                } else {
                    callListDetailActivity(valC.getStringValue("name"));
                }
            }
        });

        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {
                try {
                    valC = client.invokeService(RelationshipTypes.ThingworxEntityTypes.Things, "ClientThing_" + ClientList.get((int) id), "GetPropertyValues", new ValueCollection(), 10000).getLastRow();
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }

                callListDetailActivity(valC.getStringValue("name"));
                return true;
            }
        });

        try {
            populateListView();
        } catch (Exception e) {
            e.printStackTrace();
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.listmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        switch (item.getItemId()) {
            // action with ID action_settings was selected
            case R.id.action_main:
                i = new Intent(this, MainActivity.class);
                startActivity(i);
                break;
            case R.id.action_settings:
                if (connectionState == ConnectionState.CONNECTED)
                    disconnect();
                i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                break;
            case android.R.id.home:
                navigateUpTo(new Intent(this, ClientListActivity.class));
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * This Function tries to populate the ListView.
     *
     * @throws Exception
     */
    private void populateListView() throws Exception {
        info = client.invokeService(RelationshipTypes.ThingworxEntityTypes.Things, "ServerThing", "getConnectedClients", new ValueCollection(), 10000);
        listItems.clear();
        for (ValueCollection val : info.getRows()) {
            int id = (int) val.getValue("ID");
            String name = (String) val.getValue("name");
            String loc = (String) val.getValue("Location");
            ClientList.put(listItems.size(), id);
            listItems.add(listItems.size(), "ID:" + id + "\t\t\t" + name + "\t\t\t" + loc);
        }

        if(info.getRowCount()==0)
            throw new IllegalArgumentException( "RowCount = 0!" );
    }

    /**
     * This function populates the detail list
     *
     * @param val Collection of info
     */
    private void populateDetailList(ValueCollection val) {
        StringBuilder detail = null;

        detail = new StringBuilder("Name: " + val.get("name") + "\n");
        detail.append("Description: " + val.get("description") + "\n");
        detail.append("Distance: " + val.get("Distance").getValue() + " cm\n");
        detail.append("LastEntered: " + val.get("LastEntered") + "\n");
        detail.append("DoorStatus: " + val.get("DoorStatus") + "\n");
        detail.append("ID: " + val.get("ID").getValue() + "\n");
        detail.append("Location: " + val.get("Location") + "\n");

        ((TextView) findViewById(R.id.client_detail)).setText(detail);
    }

    /**
     * This functions calls the DetailActivity
     *
     * @param val Collection of info
     */
    private void callListDetailActivity(String name) {
        Intent i = new Intent(getApplicationContext(), ClientDetailActivity.class);
        i.putExtra("ClientName", name);

        startActivity(i);
    }

}
