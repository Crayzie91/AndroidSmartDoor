package smartdoor;

import android.app.Activity;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import smartdoor.content.AndroidThing;
import smartdoor.utilities.ThingworxService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientDetailFragment extends Fragment {
    private static final Logger LOG = LoggerFactory.getLogger(ClientDetailFragment.class);

    public static final String ARG_ITEM_ID = "client_id";

    Bundle info;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*setContentView(R.layout.activity_client_detail);
        Intent intent = getIntent();
        info = intent.getExtras();*/

    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.client_list, container, false);

        StringBuilder detail = null;

        detail = new StringBuilder("Name: " + getArguments().getString("ClientName") +"\n");
        detail.append("Description: " + getArguments().getString("ClientDescription") +"\n");
        detail.append("Distance: " + getArguments().getString("ClientDistance") +" cm\n");
        detail.append("LastEntered: " + getArguments().getString("ClientLastEntered")+"\n");
        detail.append("DoorStatus: " + getArguments().getString("ClientDoorStatus") + "\n");
        detail.append("ID: " + getArguments().getString("ClientID")+"\n");
        detail.append("Location: " + getArguments().getString("ClientLocation")+"\n");

        ((TextView) rootView.findViewById(R.id.client_detail)).setText(detail);

        return rootView;
    }

}
