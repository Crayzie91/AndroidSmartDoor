package smartdoor;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.thingworx.relationships.RelationshipTypes;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.DatetimePrimitive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import smartdoor.utilities.ThingworxService;

public class HistoryListActivity extends ThingworxService {

    private static final Logger LOG = LoggerFactory.getLogger(HistoryListActivity.class);

    private ListView list;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> listItems = new ArrayList<String>();
    private InfoTable info;

    private String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_list);

        name = getIntent().getStringExtra("name");

        list = (ListView) findViewById(R.id.history_list);

        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                listItems);
        list.setAdapter(adapter);

        try {
            info = client.invokeService(RelationshipTypes.ThingworxEntityTypes.Things, "SmartDoorClientStream", "GetStreamEntriesWithData", new ValueCollection(), 10000);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("ListView couldn't be populated.");
        }

        try {
            populateHistoryView(info);
        } catch (Exception e) {
            e.printStackTrace();
        }

        adapter.notifyDataSetChanged();

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        switch (item.getItemId()) {
            case android.R.id.home:
                navigateUpTo(new Intent(this, ClientDetailActivity.class));
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * This Function tries to populate the HistoryView.
     *
     * @throws Exception
     * @param info
     */
    private void populateHistoryView(InfoTable info) throws Exception {
        listItems.clear();
        for (ValueCollection val : info.getRows()) {
            if(val.getStringValue("source").equals(name)) {
                String EnteredBy = val.getValue("ClientEnteredBy").toString();
                String timestamp = val.getValue("timestamp").toString();
                String[] parts = timestamp.split("\\.")[0].split("T");
                listItems.add(listItems.size(), EnteredBy + "\t : \t" + parts[0]+" "+parts[1]);
            }
        }
    }
}
