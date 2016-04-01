package com.example.joseph.sweepersd;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements SweeperService.SweeperServiceListener, OnMapReadyCallback {
    private static final String TAG = MainActivity.class.getSimpleName();

    private boolean mIsBound = false;

    private Handler mActivityHandler;
    private Runnable mRunnable;
    private GoogleMap mMap;

    private SweeperService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, true);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notification, true);
        PreferenceManager.setDefaultValues(this, R.xml.pref_advanced_settings, true);

        setContentView(R.layout.activity_main);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Intent serviceIntent = new Intent(this, SweeperService.class);
        startService(serviceIntent);

        mActivityHandler = new Handler(getMainLooper());
        mRunnable = new UpdateActivityRunnable();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;
            case R.id.debugactivity:
                startActivity(new Intent(MainActivity.this, DebugActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, SweeperService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mIsBound) {
            unbindService(mServiceConnection);
            mIsBound = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mActivityHandler.postDelayed(mRunnable, 33);

        NotificationManager notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(0);
        notificationManager.cancel(1);
        notificationManager.cancel(2);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mActivityHandler.removeCallbacks(mRunnable);
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
        mMap = googleMap;

        plotParkedLocations();
    }

    private void plotParkedLocations() {
        if (mIsBound && mMap != null) {
            for (Location location : mService.getPotentialParkingLocations()) {
                // Add a marker in Sydney and move the camera
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.addMarker(new MarkerOptions().position(latLng).title("You parked here!"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f));
            }

            String url = getMapsApiDirectionsUrl();
            Log.d(TAG, "URL:" + url);
            ReadTask downloadTask = new ReadTask();
            downloadTask.execute(url);

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(BROOKLYN_BRIDGE,
                    13));
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service is connected!");
            mService = ((SweeperService.SweeperBinder)service).getService(MainActivity.this);
            mIsBound = true;
            plotParkedLocations();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mIsBound = false;
        }
    };

    private class UpdateActivityRunnable implements Runnable {
        @Override
        public void run() {
            if (mIsBound) {
            }
            mActivityHandler.postDelayed(mRunnable, 33);
        }
    }

    @Override
    public void onGooglePlayConnectionStatusUpdated(SweeperService.GooglePlayConnectionStatus status) {

    }


















    private static final LatLng LOWER_MANHATTAN = new LatLng(40.722543,
            -73.998585);
    private static final LatLng BROOKLYN_BRIDGE = new LatLng(40.7057, -73.9964);
    private static final LatLng WALL_STREET = new LatLng(40.7064, -74.0094);

    /*@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MarkerOptions options = new MarkerOptions();
        options.position(LOWER_MANHATTAN);
        options.position(BROOKLYN_BRIDGE);
        options.position(WALL_STREET);
        googleMap.addMarker(options);
        String url = getMapsApiDirectionsUrl();
        ReadTask downloadTask = new ReadTask();
        downloadTask.execute(url);

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(BROOKLYN_BRIDGE,
                13));
        addMarkers();

    }*/

    private String getMapsApiDirectionsUrl() {
        //https://maps.googleapis.com/maps/api/directions/json?origin=40.722543,-73.998585&destination=40.7064,-74.0094&waypoints=40.7057,-73.9964&key=AIzaSyBmyXKDu2TKJM9uJxpDV_uJ2_Qpm9uTwZI


        String waypoints = "" + LOWER_MANHATTAN.latitude + "," + LOWER_MANHATTAN.longitude
                + "&destination=" + BROOKLYN_BRIDGE.latitude + ","
                + BROOKLYN_BRIDGE.longitude + "&waypoints=" + WALL_STREET.latitude + ","
                + WALL_STREET.longitude;


        String sensor = "sensor=false";
        String params = "origin=" + waypoints + "&" + sensor;
        String output = "json";
        String key = "&key=" + getResources().getString(R.string.google_directions_key);
        String url = "https://maps.googleapis.com/maps/api/directions/"
                + output + "?" + params + key;
        return url;
    }

    private void addMarkers() {
        if (mMap != null) {
            mMap.addMarker(new MarkerOptions().position(BROOKLYN_BRIDGE)
                    .title("First Point"));
            mMap.addMarker(new MarkerOptions().position(LOWER_MANHATTAN)
                    .title("Second Point"));
            mMap.addMarker(new MarkerOptions().position(WALL_STREET)
                    .title("Third Point"));
        }
    }

    private class ReadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try {
                HttpConnection http = new HttpConnection();
                data = http.readUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            new ParserTask().execute(result);
        }
    }

    private class ParserTask extends
            AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(
                String... jsonData) {
            Log.d(TAG, jsonData.toString());
            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                PathJSONParser parser = new PathJSONParser();
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> routes) {
            ArrayList<LatLng> points = null;
            PolylineOptions polyLineOptions = null;

            // traversing through routes
            for (int i = 0; i < routes.size(); i++) {
                points = new ArrayList<LatLng>();
                polyLineOptions = new PolylineOptions();
                List<HashMap<String, String>> path = routes.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                polyLineOptions.addAll(points);
                polyLineOptions.width(2);
                polyLineOptions.color(Color.BLUE);
            }

            mMap.addPolyline(polyLineOptions);
        }
    }
}
