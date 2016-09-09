package com.example.david.cuneo;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.app.NotificationManager;
import android.app.Notification;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private final String TAG = "SGFactivity";
    private Map<String, LatLng> bkStations;
    PendingIntent pIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // on post-Android 6 device ifthere is no locate permission show toast and stop
        //messaggio di errore in caso di mancanza permessi localizzazione tramite toast


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Locate permission not set!", Toast.LENGTH_LONG).show();
            Log.w(TAG, "No locate permission: nothing will work");
            return;
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        //Ottiene il supportMapFragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
        // Create intent for service
        Intent intent = new Intent(this, simpleGFService.class);
        pIntent = PendingIntent.getService(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        // init bike station map
        //inizializza una hash contenente le coordinate dei punti
        bkStations = createBKhash();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.i(TAG, "onMapReady");
        mMap = googleMap;

        // Add a blue marker in Alessandria and move the camera
        //inserisce nella mappa i marker con lat e long con icona
        LatLng upo = new LatLng(44.9237555, 8.6179071); //università
        mMap.addMarker(new MarkerOptions().position(upo).title("DiSIT")
                .icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        LatLng casa = new LatLng(44.770382, 8.786369);//Casa
        mMap.addMarker(new MarkerOptions().position(casa).title("Casa")
                .icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        LatLng primoPunto = new LatLng(44.401467,7.171129);// primo punto a Cuneo
        mMap.addMarker(new MarkerOptions().position(primoPunto).title("primoPunto")
                .icon(BitmapDescriptorFactory.
                        defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        //imposto la camera sulle coordinate dove voglio che si posizioni
        CameraPosition cm = new CameraPosition.Builder()
                .target(casa).tilt(45).zoom(16).build();
        // reach the new position in 3 seconds
        mMap.animateCamera(
                CameraUpdateFactory.newCameraPosition(cm), 500, null);
        // enable the display of my location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);
        //draw red circles
        //imposto le caratteristiche delle mie geofence
        for (String s : bkStations.keySet()) {
            CircleOptions circleOptions = new CircleOptions()
                    .center(bkStations.get(s))    // a must be a LatLng object
                    .radius(150)  // radius in meters
                    .fillColor(0x40ff0000)
                    .strokeColor(Color.TRANSPARENT)
                    .strokeWidth(2);
            // draw circle on map
            Circle c = mMap.addCircle(circleOptions);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "onConnected");
        // create geofences
        //creo le geofence per tutte le string inserite nell'array
        ArrayList<Geofence> gList = new ArrayList<Geofence>();
        for (String s : bkStations.keySet()) {
            Geofence gf1 = new Geofence.Builder()
                    // Set the request ID identifying this geofence.
                    .setRequestId(s)
                    .setCircularRegion(bkStations.get(s).latitude,
                            bkStations.get(s).longitude, 150) // lat,long,radius
                    .setExpirationDuration(24 * 3600000) // 24 hours in millisec
                    .setLoiteringDelay(5000)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                            Geofence.GEOFENCE_TRANSITION_EXIT |
                            Geofence.GEOFENCE_TRANSITION_DWELL)
                    .build();
            gList.add(gf1);
        }
        // create request
        GeofencingRequest gr = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER |
                        GeofencingRequest.INITIAL_TRIGGER_DWELL |
                        GeofencingRequest.INITIAL_TRIGGER_EXIT)
                .addGeofences(gList)
                .build();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.GeofencingApi.addGeofences(
                mGoogleApiClient, gr, pIntent)
                .setResultCallback(this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "onConnectedSuspended");
    }

    private HashMap<String, LatLng> createBKhash() {
        HashMap<String, LatLng> h = new HashMap<String, LatLng>();
        h.put("Garibaldi", new LatLng(44.909045, 8.614381));
        h.put("Marengo", new LatLng(44.916316, 8.623474));
        h.put("Municipio", new LatLng(44.913129, 8.615593));
        h.put("Park Multipiano", new LatLng(44.911780, 8.620137));
        h.put("Piazza Libertà", new LatLng(44.913585, 8.615443));
        h.put("Università", new LatLng(44.922667, 8.616993));
        h.put("Stazione FF.SS.", new LatLng(44.909083, 8.607788));
        h.put("Cittadella", new LatLng(44.924156, 8.611135));
        h.put("Carducci", new LatLng(44.913527, 8.610126));
        h.put("Provvidenza", new LatLng(44.920897, 8.620121));
        h.put("Porta a Lucca", new LatLng(43.724249448743706,10.402196310659065));

        h.put("S.P. 26 zona pericolosa", new LatLng(44.670741, 7.286460)); //giusto
        h.put("Fine pericolo S.P. 26", new LatLng(44.673754, 7.283747)); //giusto
        h.put("S.P. 8 zona pericolosa", new LatLng(44.577631, 7.202308)); //giusto
        h.put("Fine pericolo S.P. 8", new LatLng(44.576372, 7.197850)); //giusto
        h.put("S.P. 12 zona pericolosa", new LatLng(44.430906, 7.860333)); //giusto
        h.put("Fine pericolo S.P. 12", new LatLng(44.433711, 7.863483)); //giusto
        h.put("S.P. 211 zona pericolosa", new LatLng(44.342772, 7.693383)); //giusto
        h.put("Fine pericolo S.P. 211", new LatLng(44.339192, 7.692800)); //giusto
        h.put("S.P.271, km 0 zona pericolosa", new LatLng(44.350908, 7.818975)); //giusto
        h.put("Fine Pericolo S.P. 271 km 0", new LatLng(44.347606, 7.820783)); //giusto
        h.put("S.P.271 km 1", new LatLng(44.353656, 7.804189)); //giusto
        h.put("Fine Pericolo S.P. 271 km 1", new LatLng(44.353994, 7.809164)); //giusto

        h.put("casa", new LatLng(44.770382, 8.786369));

        h.put("CasaG", new LatLng(44.769369, 8.794782));
        //h.put("Cuneo", new LatLng(44.382783, 7.542102));
        //h.put("Cuneo2", new LatLng(44.384463, 7.542669));
        h.put("Tokyo Restourant", new LatLng(44.46137,8.47206));
        h.put("Pathos", new LatLng(44.763063, 8.801019));
        h.put("Corso Nizza", new LatLng(44.382958, 7.541117));
        h.put("Sede provincia", new LatLng(44.385967, 7.544207));
        h.put("Prima rotonda ss20", new LatLng(44.378154, 7.536101));
        h.put("Seconda rotonda ss20", new LatLng(44.376644, 7.534595));
        h.put("Viale rimembranza", new LatLng( 44.767797, 8.786316));


        return h;
    }
    @Override
    public void onResult(Status status) {
        Log.i(TAG, "onResult: Status " + status.toString());
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "onConnectionFailed");

    }

    }
