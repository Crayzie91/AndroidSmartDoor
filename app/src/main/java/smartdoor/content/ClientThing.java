package smartdoor.content;

import com.thingworx.communications.client.AndroidConnectedThingClient;
import com.thingworx.communications.client.ConnectedThingClient;
import com.thingworx.relationships.RelationshipTypes;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.types.primitives.IntegerPrimitive;

public class ClientThing {
    private String ClientName;
    private String ClientDescription;
    private int ClientDistance;
    private String ClientLastEntered;
    private String ClientDoorStatus;
    private int ClientID;
    private String ClientLocation;
    private String ClientLastConnected;

    public void setup(int ID, AndroidConnectedThingClient client) throws Exception {
        ValueCollection info = client.invokeService(RelationshipTypes.ThingworxEntityTypes.Things, "ClientThing_"+ID, "GetPropertyValues", new ValueCollection(), 10000).getLastRow();
        ClientName = info.getStringValue("name");
        ClientDescription = info.getStringValue("description");
        ClientDistance = (int)info.get("distance").getValue();
        ClientLastEntered = info.getStringValue("description");
        ClientDoorStatus = info.getStringValue("DoorStatus");
        ClientID = (int)info.get("ID").getValue();
        ClientLocation = info.getStringValue("location");
        ClientLastConnected = info.getStringValue("lastConnection");
    }

    public String getClientName() {
        return ClientName;
    }

    public void setClientName(String clientName) {
        ClientName = clientName;
    }

    public String getClientDescription() {
        return ClientDescription;
    }

    public void setClientDescription(String clientDescription) {
        ClientDescription = clientDescription;
    }

    public int getClientDistance() {
        return ClientDistance;
    }

    public void setClientDistance(int clientDistance) {
        ClientDistance = clientDistance;
    }

    public String getClientLastEntered() {
        return ClientLastEntered;
    }

    public void setClientLastEntered(String clientLastEntered) {
        ClientLastEntered = clientLastEntered;
    }

    public String getClientDoorStatus() {
        return ClientDoorStatus;
    }

    public void setClientDoorStatus(String clientDoorStatus) {
        ClientDoorStatus = clientDoorStatus;
    }

    public int getClientID() {
        return ClientID;
    }

    public void setClientID(int clientID) {
        ClientID = clientID;
    }

    public String getClientLocation() {
        return ClientLocation;
    }

    public void setClientLocation(String clientLocation) {
        ClientLocation = clientLocation;
    }

    public String getClientLastConnected() {
        return ClientLastConnected;
    }

    public void setClientLastConnected(String clientLastConnected) {
        ClientLastConnected = clientLastConnected;
    }
}
