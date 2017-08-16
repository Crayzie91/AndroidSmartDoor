package smartdoor.content;


        import android.app.IntentService;
        import android.app.Notification;
        import android.app.NotificationManager;
        import android.app.PendingIntent;
        import android.content.Context;
        import android.content.Intent;
        import android.graphics.Bitmap;
        import android.graphics.BitmapFactory;
        import android.os.AsyncTask;

        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;

        import com.thingworx.communications.client.ConnectedThingClient;
        import com.thingworx.communications.client.things.VirtualThing;
        import com.thingworx.metadata.PropertyDefinition;
        import com.thingworx.metadata.annotations.ThingworxPropertyDefinition;
        import com.thingworx.metadata.annotations.ThingworxPropertyDefinitions;
        import com.thingworx.metadata.annotations.ThingworxServiceDefinition;
        import com.thingworx.metadata.annotations.ThingworxServiceParameter;
        import com.thingworx.metadata.annotations.ThingworxServiceResult;
        import com.thingworx.relationships.RelationshipTypes;
        import com.thingworx.types.InfoTable;
        import com.thingworx.types.collections.ValueCollection;
        import com.thingworx.types.constants.CommonPropertyNames;
        import com.thingworx.types.primitives.IPrimitiveType;
        import com.thingworx.types.primitives.ImagePrimitive;
        import com.thingworx.types.primitives.StringPrimitive;

        import java.io.BufferedReader;
        import java.io.DataOutputStream;
        import java.io.InputStream;
        import java.io.InputStreamReader;
        import java.net.HttpURLConnection;
        import java.net.URL;

        import ch.qos.logback.core.net.server.Client;
        import smartdoor.ClientListActivity;
        import smartdoor.R;

        import static android.content.Context.NOTIFICATION_SERVICE;
        import static smartdoor.MainActivity.NOTIFICATION_CODE;
        import static smartdoor.utilities.ThingworxService.getClient;

@SuppressWarnings("serial")
@ThingworxPropertyDefinitions(properties = {
        @ThingworxPropertyDefinition(name="count",
                description="Test",
                baseType="INTEGER",
                aspects={"dataChangeType:ALWAYS",
                        "dataChangeThreshold:0",
                        "cacheTime:0",
                        "isPersistent:FALSE",
                        "isReadOnly:FALSE",
                        "pushType:ALWAYS",
                        "defaultValue:0"}),
})

public class AndroidThing extends VirtualThing {

    public static final String ACTION_1 = "action_1";
    public static final String ACTION_2 = "action_2";

    private static final Logger LOG = LoggerFactory.getLogger(AndroidThing.class);
    private static ConnectedThingClient ClientHandle;
    private Context Context;

    /**
     * A custom constructor. The Constructor is needed to call initializeFromAnnotations,
     * which processes all of the VirtualThing's annotations and applies them to the
     * object.
     *
     * @param name The name of the thing.
     * @param description A description of the thing.
     * @param client The client that this thing is associated with.
     */
    public AndroidThing(String name, String description, ConnectedThingClient client, Context ctx) throws Exception {
        // Call the super class's constrcutor
        super(name, description, client);
        ClientHandle = client;
        Context = ctx;
        // Call the initializeFromAnnotations method to initialize all of the properties, services, and definitions created from annotations.
        this.initializeFromAnnotations();
    }

    public void setClientHandle(ConnectedThingClient handle){
        ClientHandle=handle;
    }

    public ConnectedThingClient getClientHandle(){
        return ClientHandle;
    }

    /**
     * This method will get called when a connect or reconnect happens
     * The called functions synchronize the state and the properties of the virtual thing
     */
    @Override
    public void synchronizeState() {
        super.synchronizeState();

        // Send the property values to ThingWorx when a synchronization is required
        super.syncProperties();
    }

    /**
     * This method will get called when a connect or reconnect happens
     * The called functions synchronize the state and the properties of the virtual thing
     */
    @Override
    public void processScanRequest() {
        try {
            this.updateSubscribedProperties(1000);
            this.updateSubscribedEvents(1000);
        } catch (Exception e) {
            // This will occur if we provide an unknown property name.
            LOG.error("Exception occured while updating properties.", e);
        }
    }

    /**
     * This Method handles the property writes from the server
     *
     */
    @Override
    public void processPropertyWrite(PropertyDefinition property, @SuppressWarnings("rawtypes") IPrimitiveType value) throws Exception {
        String propName = property.getName();
        setProperty(propName,value);
        this.updateSubscribedProperties(1000);
        LOG.info("{} was set. New Value: {}", propName, value);
    }

    /**
     * This Method is used to read a Property of a Thing on the Thingworx Platform.
     *
     * @param PropertyName	Name of the Property to change
     * @return Returns Object that contains the read value
     */
    public Object getClientProperty(String PropertyName) {
        Object var = getProperty(PropertyName).getValue().getValue();
        LOG.info("{} was read. Value: {}", PropertyName, var);
        return var;
    }

    /**
     * This Method is used to write a Property of a Thing on the Thingworx Platform.
     * Value is casted to a generic type for further use.
     *
     * @param PropertyName	Name of the Property to change
     * @param value	New Value of the Property
     * @throws Exception
     */
    public void setClientProperty(String PropertyName, Object value) throws Exception{
        setProperty(PropertyName, value);
        LOG.info("{} was set. New Value: {}", this.getBindingName(), value);
    }

    /**
     * This function is called by the platform to handle the unknown entry call.
     *
     * @param img Image taken by the client of the unknown entry
     * @return TRUE if execution was successful
     * @throws Exception
     */
    @ThingworxServiceDefinition(
            name="UnknownEntry",
            description="Gets the url to the taken image of the unknown person and displays it in a notification in the app.\n" +
                    "The user can then decide if he will permit or deny the access.")
    @ThingworxServiceResult(name=CommonPropertyNames.PROP_RESULT, baseType="BOOLEAN", description="TRUE if execution was successful.")
    public boolean UnknownEntry(
            @ThingworxServiceParameter(name="img", baseType="IMAGE", description="Image from the repository") byte[] img,
            @ThingworxServiceParameter(name="caller", baseType="STRING", description="Name of caller") String caller) throws Exception {
        pushNotification(caller, img);
        return true;
    }

    /**
     * This function handles the notfication calls from outside classes
     *
     * @param img Image to show in the notificaton
     */
    public void pushNotification(String caller, byte[] img){
        new NotificationClass().execute(caller, img);
    }

    /**
     * This class impelements an AsyncTask which is used to retrieve a picture from the platform
     * and show it in a big picture notification.
     */
    @SuppressWarnings("deprecation")
    public class NotificationClass extends AsyncTask<Object, Object, Void> {

        public NotificationClass() {
            super();
        }

        @Override
        protected Void doInBackground(Object... params) {
            Bitmap bmp = BitmapFactory.decodeByteArray(((byte[])params[1]), 0, ((byte[])params[1]).length);
            Bitmap largelogo = BitmapFactory.decodeResource(Context.getResources(), R.drawable.ic_logo);

            // Create intent for action 1
            Intent action1Intent = new Intent(Context.getApplicationContext(), NotificationActionService.class)
                    .setAction(ACTION_1)
                    .putExtra("Caller",(String)params[0]);
            PendingIntent action1PendingIntent = PendingIntent.getService(Context.getApplicationContext(), 0,
                    action1Intent, PendingIntent.FLAG_ONE_SHOT);
            // Create intent for action 2
            Intent action2Intent = new Intent(Context.getApplicationContext(), NotificationActionService.class)
                    .setAction(ACTION_2)
                    .putExtra("Caller",(String)params[0]);
            PendingIntent action2PendingIntent = PendingIntent.getService(Context.getApplicationContext(), 0,
                    action2Intent, PendingIntent.FLAG_ONE_SHOT);

            Notification notif = new Notification.Builder(Context.getApplicationContext())
                    .setContentTitle("New Entry Request")
                    .setContentText("Picture of unknown Person was taken")
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setLargeIcon(largelogo)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setStyle(new Notification.BigPictureStyle()
                            .bigPicture(bmp))
                    .addAction(0, "Permit", action1PendingIntent)
                    .addAction(0, "Dismiss", action2PendingIntent)
                    .build();

            // hide the notification after its selected
            notif.flags |= Notification.FLAG_AUTO_CANCEL;

            // call notification
            NotificationManager mgr = (NotificationManager) Context.getSystemService(NOTIFICATION_SERVICE);
            mgr.notify(NOTIFICATION_CODE, notif);

            return null;
        }
    }

    /**
     * This class implements the handling for the inten of the notifications action buttons
     */
    public static class NotificationActionService extends IntentService {

        public NotificationActionService() {
            super(NotificationActionService.class.getSimpleName());
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            String action = intent.getAction();
            String caller = intent.getStringExtra("Caller");

            if (ACTION_1.equals(action)) {
                HTTPRequest(caller, "Open");
            } else if (ACTION_2.equals(action)) {
                HTTPRequest(caller, "Closed");
            }
        }

        protected void HTTPRequest(String name, String status){
            try {
                URL url = new URL("http://34.227.165.169/Thingworx/Things/" + name + "/Services/remoteDoor");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("PUT");
                connection.setRequestProperty("appKey", "ce22e9e4-2834-419c-9656-ef9f844c784c");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");

                connection.setUseCaches(false);
                connection.setDoOutput(true);
                connection.setDoInput(true);

                //Send request
                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                String body = "{\"Status\":\""+status+"\"}";
                wr.writeBytes(body);
                wr.close();

                InputStream is = connection.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            }
            catch (Exception e){
                LOG.info("Put couldn't be send. {}",e);
            }
        }
    }
}
