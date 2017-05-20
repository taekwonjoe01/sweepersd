package com.example.joseph.sweepersd.presentation.manualalarms;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.joseph.sweepersd.R;
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

public class CreateWatchZoneActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {
    private static final String TAG = CreateWatchZoneActivity.class.getSimpleName();

    public static final String LABEL_KEY = "LABEL_KEY";
    public static final String LOCATION_KEY = "LOCATION_KEY";
    public static final String RADIUS_KEY = "RADIUS_KEY";

    private TextView mRadiusDisplay;
    private SeekBar mRadiusSeekbar;

    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;

    private Circle mMarkerRadius;

    private LatLng mLatLng;

    private String mLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_watch_zone);

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
        getMenuInflater().inflate(R.menu.menu_activity_create_watch_zone, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_done:
                showCreateLabelDialog();
                return true;
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    0);
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();

        dismissCreateLabelDialog();
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

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                setAlarmLocation(latLng);
            }
        });
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ) {
            Location currentLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            if (currentLocation != null) {
                setAlarmLocation(new LatLng(currentLocation.getLatitude(),
                        currentLocation.getLongitude()));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mLatLng, 16f));
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private void showCreateLabelDialog() {
        DialogFragment fragment = (DialogFragment) getFragmentManager().findFragmentByTag(
                CreateAlarmLabelDialogFragment.class.getSimpleName());
        if (fragment == null) {
            fragment = new CreateAlarmLabelDialogFragment().newInstance(
                    new CreateAlarmLabelDialogFragment.CreateAlarmLabelDialogListener() {
                @Override
                public void onLabelCreated(String label) {
                    mLabel = label;
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra(LABEL_KEY, mLabel);
                    returnIntent.putExtra(LOCATION_KEY, mLatLng);
                    returnIntent.putExtra(
                            RADIUS_KEY, getRadiusForProgress(mRadiusSeekbar.getProgress()));
                    returnIntent.putExtra(RADIUS_KEY,
                            getRadiusForProgress(mRadiusSeekbar.getProgress()));
                    setResult(Activity.RESULT_OK,returnIntent);
                    finish();
                }
            });
        }
        fragment.show(getFragmentManager(), CreateAlarmLabelDialogFragment.class.getSimpleName());
    }

    private void dismissCreateLabelDialog() {
        DialogFragment fragment = (DialogFragment) getFragmentManager().findFragmentByTag(
                CreateAlarmLabelDialogFragment.class.getSimpleName());
        if (fragment != null) {
            fragment.dismiss();
        }
    }

    private void setAlarmLocation(LatLng location) {
        mMap.clear();

        mLatLng = location;

        mMarkerRadius = mMap.addCircle(new CircleOptions()
                .center(mLatLng)
                .radius(getRadiusForProgress(mRadiusSeekbar.getProgress()))
                .strokeColor(getResources().getColor(R.color.app_primary))
                .fillColor(getResources().getColor(R.color.map_radius_fill)));
    }

    private int getRadiusForProgress(int progress) {
        return 20 + progress * 2;
    }
}
