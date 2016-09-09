package com.example.david.cuneo;

/**
 * Created by David on 24/07/2016.
 */
import android.app.AlertDialog;
import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import android.app.Notification;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

/**
 * Created by giovanni on 01/06/16.
 */

public class simpleGFService extends IntentService {


    protected static final String TAG = "SGFservice";

    public simpleGFService() {super(TAG);}

    protected void onHandleIntent(Intent i) {
        String geofence_transition_invalid_type = "termine area";
        Log.i(TAG, "Service Started!!!");
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(i);
        // controllo se ci sono errori
        if (geofencingEvent.hasError()) {
            String errorMessage =  GeofenceStatusCodes.getStatusCodeString(
                    geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        // Get the transition type.
       // int geofenceTransition = geofencingEvent.getGeofenceTransition();

        //test
        /*if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT){

            List triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            String geofenceTransitionDetails = getGeofenceTransitionDetails(this, geofenceTransition, triggeringGeofences);

        //invio notifica
            sendNotification(geofenceTransitionDetails);
            Log.i(TAG, geofenceTransitionDetails);

        }
        else {
            Log.e(TAG, getString(R.string.geofence_transition_invalid_type, geofenceTransition));
        }*/



        String gtrString;
        switch(geofencingEvent.getGeofenceTransition()) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                gtrString = "Enter";

                NotificationCompat.Builder n = new NotificationCompat
                        .Builder(this).setContentTitle("Attenzione")
                        .setContentText("Entrati in zona pericolosa")
                        .setSmallIcon(android.R.drawable.ic_dialog_email);

                NotificationManager notificationManager = (NotificationManager)
                        getSystemService(NOTIFICATION_SERVICE);
                notificationManager.notify(0,n.build());

                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                gtrString = "Exit";
                NotificationCompat.Builder m = new NotificationCompat
                        .Builder(this).setContentTitle("Attenzione")
                        .setContentText("Usciti da una zona pericolosa")
                        .setSmallIcon(android.R.drawable.ic_dialog_email);

                NotificationManager notificationManagerExit = (NotificationManager)
                        getSystemService(NOTIFICATION_SERVICE);
                notificationManagerExit.notify(0,m.build());
                break;
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                gtrString = "Dwell";
                break;
            default:
                gtrString = "Unknown";
        }
        // Get the geofences that were triggered. A single event can trigger multiple geofences.
        List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
        for(Geofence g: triggeringGeofences) {
            Log.i(TAG, gtrString + ": " + g.toString());
        }
    }


}