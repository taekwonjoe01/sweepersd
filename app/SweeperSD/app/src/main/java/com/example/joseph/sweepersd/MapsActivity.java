package com.example.joseph.sweepersd;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {
    private static final String TAG = MapsActivity.class.getSimpleName();

    public static final String LOCATION_KEY = "LOCATION_KEY";
    public static final String RADIUS_KEY = "RADIUS_KEY";

    private TextView mRadiusDisplay;
    private SeekBar mRadiusSeekbar;
    private GoogleMap mMap;

    private GoogleApiClient mGoogleApiClient;

    private Circle mMarkerRadius;

    private LatLng mLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mRadiusDisplay = (TextView) findViewById(R.id.radius_display);
        mRadiusSeekbar = (SeekBar) findViewById(R.id.seekbar_radius);

        mRadiusDisplay.setText(String.format(getString(R.string.alarm_radius_string),
                getRadiusForProgress(mRadiusSeekbar.getProgress())));
        mRadiusSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mRadiusDisplay.setText(String.format(getString(R.string.alarm_radius_string),
                        getRadiusForProgress(progress)));
                mMarkerRadius.setRadius(getRadiusForProgress(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
     public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_maps, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_done:
                Intent returnIntent = new Intent();
                returnIntent.putExtra(LOCATION_KEY, mLatLng);
                returnIntent.putExtra(
                        RADIUS_KEY, getRadiusForProgress(mRadiusSeekbar.getProgress()));
                returnIntent.putExtra(RADIUS_KEY,
                        getRadiusForProgress(mRadiusSeekbar.getProgress()));
                setResult(Activity.RESULT_OK,returnIntent);
                finish();
                return true;
        }
        return false;
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

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();

        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                if (mMarkerRadius != null) {
                    mMarkerRadius.remove();
                }
            }

            @Override
            public void onMarkerDrag(Marker marker) {
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                LatLng location = new LatLng(marker.getPosition().latitude,
                        marker.getPosition().longitude);
                setAlarmLocation(location);
            }
        });
    }

    @Override
    public void onConnected(Bundle bundle) {
        Location currentLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (currentLocation != null) {
            setAlarmLocation(new LatLng(currentLocation.getLatitude(),
                    currentLocation.getLongitude()));
        } else {
            //setAlarmLocation(new LatLng(32.715736, -117.161087));
            setAlarmLocation(new LatLng(32.803680778620155, -117.25259441882373));
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private void setAlarmLocation(LatLng location) {
        mMap.clear();

        mLatLng = location;

        LatLng center = mLatLng;
        mMap.addMarker(new MarkerOptions()
                .position(center)
                .draggable(true));
        mMarkerRadius = mMap.addCircle(new CircleOptions()
                .center(center)
                .radius(getRadiusForProgress(mRadiusSeekbar.getProgress()))
                .strokeColor(Color.RED)
                .fillColor(R.color.map_radius_fill));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 18f));
    }

    private int getRadiusForProgress(int progress) {
        return 20 + progress * 2;
    }
}
