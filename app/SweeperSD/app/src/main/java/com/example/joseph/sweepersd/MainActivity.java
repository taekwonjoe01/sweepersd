package com.example.joseph.sweepersd;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Address;
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
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.joseph.sweepersd.utils.LocationUtils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

public class MainActivity extends AppCompatActivity
        implements SweeperService.SweeperServiceListener, OnMapReadyCallback {
    private static final String TAG = MainActivity.class.getSimpleName();

    private boolean mIsBound = false;

    private Handler mActivityHandler;
    private Runnable mRunnable;
    private GoogleMap mMap;

    // Status UI elements
    private TextView mDrivingStatusText;
    private ImageView mDrivingStatusImage;

    private FrameLayout mNoParkingLocationsDisplay;
    private FrameLayout mCurrentLocationUnknownDisplay;

    // Timer display UI elements
    private ImageView mTimerImage;
    private TextView mTimerText;

    // Address UI elements
    private TextView mAddressStreetText;
    private TextView mAddressCityText;

    // Limit UI elements
    private TextView mLimitStreetText;
    private TextView mLimitRangeText;
    private TextView mLimitScheduleText;

    private SweeperService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, true);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notification, true);
        PreferenceManager.setDefaultValues(this, R.xml.pref_advanced_settings, true);

        setContentView(R.layout.activity_main);

        mDrivingStatusText = (TextView) findViewById(R.id.text_driving_status);
        mDrivingStatusImage = (ImageView) findViewById(R.id.image_driving_status);

        mNoParkingLocationsDisplay = (FrameLayout) findViewById(R.id.layout_no_parked_locations);
        mCurrentLocationUnknownDisplay = (FrameLayout) findViewById(
                R.id.layout_current_location_unknown);

        mTimerText = (TextView) findViewById(R.id.text_time_until_sweeping);
        mTimerImage = (ImageView) findViewById(R.id.image_timer);

        mAddressStreetText = (TextView) findViewById(R.id.address_street);
        mAddressCityText = (TextView) findViewById(R.id.address_city);

        mLimitStreetText = (TextView) findViewById(R.id.limit_street);
        mLimitRangeText = (TextView) findViewById(R.id.limit_range);
        mLimitScheduleText = (TextView) findViewById(R.id.limit_schedule);

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
        notificationManager.cancel(NotificationPresenter.NotificationType.PARKED.ordinal());
        notificationManager.cancel(NotificationPresenter.NotificationType.REDZONE.ordinal());
        notificationManager.cancel(NotificationPresenter.NotificationType.REDZONE_WARNING.ordinal());
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

        updateUI();
    }

    private void updateUI() {
        if (mIsBound && mMap != null) {
            clearMap();

            // Setup the status
            if (mService.isDriving()) {
                mDrivingStatusText.setText("Driving");

                LocationDetails location = mService.getCurrentLocationDetails();

                if (location != null) {
                    mNoParkingLocationsDisplay.setVisibility(View.GONE);
                    mCurrentLocationUnknownDisplay.setVisibility(View.GONE);
                    long timeUntilSweeping = LocationUtils.getMsUntilLimit(location.limit);

                    long hoursUntilParking = timeUntilSweeping / 3600000;
                    long leftOverMinutes = (timeUntilSweeping % 3600000) / 60000;
                    long daysUntilSweeping = hoursUntilParking / 24;
                    long leftOverHours = hoursUntilParking % 24;
                    String timerMessage = daysUntilSweeping + " days, "
                            + leftOverHours + " hours, and " + leftOverMinutes + " minutes.";
                    if (timeUntilSweeping == Long.MAX_VALUE) {
                        timerMessage = "No Sweeping";
                    } else if (timeUntilSweeping < 0) {
                        timerMessage = "Now";
                    }
                    if (timeUntilSweeping < 0) {
                        mDrivingStatusImage.setImageResource(R.drawable.ic_lrg_car_red);
                        addMapMarker(location.location, R.drawable.ic_lrg_car_red);
                        mTimerImage.setImageResource(R.drawable.ic_timer_red);
                    } else if (timeUntilSweeping < mService.getRedzoneLimit()) {
                        mDrivingStatusImage.setImageResource(R.drawable.ic_lrg_car_yellow);
                        addMapMarker(location.location, R.drawable.ic_lrg_car_yellow);
                        mTimerImage.setImageResource(R.drawable.ic_timer_yellow);
                    } else {
                        mDrivingStatusImage.setImageResource(R.drawable.ic_lrg_car_green);
                        addMapMarker(location.location, R.drawable.ic_lrg_car_green);
                        mTimerImage.setImageResource(R.drawable.ic_timer_green);
                    }
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
                            location.location.getLatitude(),
                            location.location.getLongitude()), 16f));
                    mTimerText.setText(timerMessage);

                    if (location.addresses == null || location.addresses.isEmpty()) {
                        mAddressCityText.setText("");
                        mAddressStreetText.setText("");
                    } else {
                        Address address = location.addresses.get(0);
                        String street = address.getThoroughfare();
                        String housenumber = address.getFeatureName();
                        String city = address.getLocality();
                        String state = address.getAdminArea();
                        String zipCode = address.getPostalCode();

                        mAddressStreetText.setText(housenumber + " " + street);
                        mAddressCityText.setText(city + ", " + state + " " + zipCode);
                    }

                    if (location.limit == null) {
                        mLimitStreetText.setText("");
                        mLimitRangeText.setText("");
                        mLimitScheduleText.setText("");
                    } else {
                        mLimitStreetText.setText(location.limit.getStreet());
                        mLimitRangeText.setText(location.limit.getRange()[0] + " "
                                + location.limit.getRange()[1]);
                        String concatenatedSchedules = "";
                        for (String schedule : location.limit.getSchedules()) {
                            concatenatedSchedules += "\n" + schedule;
                        }
                        mLimitScheduleText.setText(concatenatedSchedules);
                    }
                } else {
                    mDrivingStatusImage.setImageResource(R.drawable.ic_lrg_car_black);
                    mTimerImage.setImageResource(R.drawable.ic_timer_black);
                    mCurrentLocationUnknownDisplay.setVisibility(View.VISIBLE);
                    mTimerText.setText("");
                    mAddressCityText.setText("");
                    mAddressStreetText.setText("");
                    mLimitStreetText.setText("");
                    mLimitRangeText.setText("");
                    mLimitScheduleText.setText("");
                }
            } else {
                mDrivingStatusText.setText("Parked");

                List<LocationDetails> parkedLocations =
                        mService.getParkedLocationDetails();

                for (LocationDetails location : parkedLocations) {
                    long timeUntilSweeping = LocationUtils.getMsUntilLimit(
                            location.limit);

                    if (timeUntilSweeping < 0) {
                        addMapMarker(location.location,
                                R.drawable.ic_lrg_no_parking_red);
                    } else if (timeUntilSweeping < mService.getRedzoneLimit()) {
                        addMapMarker(location.location,
                                R.drawable.ic_lrg_no_parking_yellow);
                    } else {
                        addMapMarker(location.location,
                                R.drawable.ic_lrg_parking_green);
                    }
                }

                if (!parkedLocations.isEmpty()) {
                    mNoParkingLocationsDisplay.setVisibility(View.GONE);
                    mCurrentLocationUnknownDisplay.setVisibility(View.GONE);
                    LocationDetails currentParkedLocation =
                            parkedLocations.get(parkedLocations.size() - 1);

                    long timeUntilSweeping = LocationUtils.getMsUntilLimit(
                            currentParkedLocation.limit);

                    long hoursUntilParking = timeUntilSweeping / 3600000;
                    long leftOverMinutes = (timeUntilSweeping % 3600000) / 60000;
                    long daysUntilSweeping = hoursUntilParking / 24;
                    long leftOverHours = hoursUntilParking % 24;
                    String timerMessage = daysUntilSweeping + " days, "
                            + leftOverHours + " hours, and " + leftOverMinutes + " minutes.";
                    if (timeUntilSweeping == Long.MAX_VALUE) {
                        timerMessage = "No Sweeping";
                    } else if (timeUntilSweeping < 0) {
                        timerMessage = "Now";
                    }
                    if (timeUntilSweeping < 0) {
                        mDrivingStatusImage.setImageResource(R.drawable.ic_lrg_no_parking_red);
                        mTimerImage.setImageResource(R.drawable.ic_timer_red);
                    } else if (timeUntilSweeping < mService.getRedzoneLimit()) {
                        mDrivingStatusImage.setImageResource(R.drawable.ic_lrg_no_parking_yellow);
                        mTimerImage.setImageResource(R.drawable.ic_timer_yellow);
                    } else {
                        mDrivingStatusImage.setImageResource(R.drawable.ic_lrg_parking_green);
                        mTimerImage.setImageResource(R.drawable.ic_timer_green);
                    }
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
                            currentParkedLocation.location.getLatitude(),
                            currentParkedLocation.location.getLongitude()), 16f));
                    mTimerText.setText(timerMessage);

                    if (currentParkedLocation.addresses == null ||
                            currentParkedLocation.addresses.isEmpty()) {
                        mAddressCityText.setText("");
                        mAddressStreetText.setText("");
                    } else {
                        Address address = currentParkedLocation.addresses.get(0);
                        String street = address.getThoroughfare();
                        String housenumber = address.getFeatureName();
                        String city = address.getLocality();
                        String state = address.getAdminArea();
                        String zipCode = address.getPostalCode();

                        mAddressStreetText.setText(housenumber + " " + street);
                        mAddressCityText.setText(city + ", " + state + " " + zipCode);
                    }

                    if (currentParkedLocation.limit == null) {
                        mLimitStreetText.setText("");
                        mLimitRangeText.setText("");
                        mLimitScheduleText.setText("");
                    } else {
                        mLimitStreetText.setText(currentParkedLocation.limit.getStreet());
                        mLimitRangeText.setText(currentParkedLocation.limit.getRange()[0] + " "
                                + currentParkedLocation.limit.getRange()[1]);
                        String concatenatedSchedules = "";
                        for (String schedule : currentParkedLocation.limit.getSchedules()) {
                            concatenatedSchedules += "\n" + schedule;
                        }
                        mLimitScheduleText.setText(concatenatedSchedules);
                    }
                } else {
                    mDrivingStatusImage.setImageResource(R.drawable.ic_lrg_parking_black);
                    mTimerImage.setImageResource(R.drawable.ic_timer_black);
                    mNoParkingLocationsDisplay.setVisibility(View.VISIBLE);
                    mTimerText.setText("");
                    mAddressCityText.setText("");
                    mAddressStreetText.setText("");
                    mLimitStreetText.setText("");
                    mLimitRangeText.setText("");
                    mLimitScheduleText.setText("");
                }
            }
        }
    }

    private void addMapMarker(Location location, int resourceId) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.addMarker(new MarkerOptions().position(latLng)
                .title("You parked here!")
                .icon(BitmapDescriptorFactory.fromResource(resourceId))
                .anchor(0.5f, 0.5f));
    }

    private void clearMap() {
        mMap.clear();
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service is connected!");
            mService = ((SweeperService.SweeperBinder)service).getService(MainActivity.this);
            mIsBound = true;
            updateUI();
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

    /*
    SweeperServiceListener callback.
     */
    @Override
    public void onGooglePlayConnectionStatusUpdated(SweeperService.GooglePlayConnectionStatus status) {

    }

    /*
    SweeperServiceListener callback.
     */
    @Override
    public void onParked(List<LocationDetails> results) {
        updateUI();
    }

    /*
    SweeperServiceListener callback.
     */
    @Override
    public void onDriving() {
        updateUI();
        mService.registerDrivingLocationListener(new SweeperService.DrivingLocationListener() {
            @Override
            public void onLocationChanged(LocationDetails location) {
                updateUI();
            }
        });
    }
}
