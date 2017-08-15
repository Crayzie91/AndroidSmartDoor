package smartdoor;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import smartdoor.content.AndroidThing;
import smartdoor.utilities.ThingworxService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientDetailFragment extends ThingworxService {
    private static final Logger LOG = LoggerFactory.getLogger(ClientDetailFragment.class);

    public static final String ARG_ITEM_ID = "client_id";

    Bundle info;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_detail);
        Intent intent = getIntent();
        info = intent.getExtras();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_client_detail, container, false);

        StringBuilder detail = null;

        detail = new StringBuilder("Name: " + info.get("ClientName") +"\n");
        detail.append("Description: " + info.get("ClientDescription") +"\n");
        detail.append("Distance: " + info.get("ClientDistance") +" cm\n");
        detail.append("LastEntered: " + info.get("ClientLastEntered")+"\n");
        detail.append("DoorStatus: " + info.get("ClientDoorStatus") + "\n");
        detail.append("ID: " + info.get("ClientID")+"\n");
        detail.append("Location: " + info.get("ClientLocation")+"\n");

        ((TextView) rootView.findViewById(R.id.client_detail)).setText(detail);

        return rootView;
    }

}
