package com.example.joseph.sweepersd;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
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
}
