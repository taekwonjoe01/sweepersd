package com.example.joseph.sweepersd;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SweeperService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener{
    private static final String TAG = SweeperService.class.getSimpleName();
    private static final int DRIVING_CONFIDENCE = 85;
    private static final int NOT_DRIVING_CONFIDENCE = 20;
    private static final int FOOT_CONFIDENCE = 30;
    public static final String CONFIDENCE_VEHICLE = "CONFIDENCE_VEHICLE";
    public static final String CONFIDENCE_FOOT = "CONFIDENCE_FOOT";
    public static final String CONFIDENCE_BICYCLE = "CONFIDENCE_BICYCLE";
    public static final String CONFIDENCE_WALKING = "CONFIDENCE_WALKING";
    public static final String CONFIDENCE_RUNNING = "CONFIDENCE_RUNNING";
    public static final String CONFIDENCE_TILTING = "CONFIDENCE_TILTING";
    public static final String CONFIDENCE_STILL = "CONFIDENCE_STILL";
    public static final String CONFIDENCE_UNKNOWN = "CONFIDENCE_UNKNOWN";

    public enum GooglePlayConnectionStatus {
        DISCONNECTED, CONNECTING, CONNECTED, FAILED, SUSPENDED
    }

    private volatile GooglePlayConnectionStatus mConnectionStatus =
            GooglePlayConnectionStatus.DISCONNECTED;
    private GoogleApiClient mClient;
    private LocationManager mLocationManager;
    private SweeperServiceListener mListener;
    private boolean mIsConnected = false;
    private boolean mIsStarted = false;
    private Location mLocation;

    private final IBinder mBinder = new SweeperBinder();

    private volatile int mInVehicleConfidence;
    private volatile int mOnFootConfidence;
    private volatile int mStillConfidence;
    private volatile int mUnknownConfidence;
    private volatile int mOnBicycleConfidence;
    private volatile int mWalkingConfidence;
    private volatile int mRunningConfidence;
    private volatile int mTiltingConfidence;
    private volatile boolean mIsDriving = false;
    private volatile boolean mIsParked = false;
    private volatile Location mParkedLocation;
    private volatile List<Address> mParkedAddresses = new ArrayList<>();
    private volatile List<Address> mCurrentLocationAddresses = new ArrayList<>();
    private volatile Limit mParkedLimit;

    private List<Limit> mLimits = new ArrayList<>();
    private List<Limit> mPostedLimits = new ArrayList<>();

    public SweeperService() {
    }

    public int getConfidence(String confidence) {
        int result = -1;
        switch (confidence) {
            case CONFIDENCE_BICYCLE:
                result = mOnBicycleConfidence;
                break;
            case CONFIDENCE_FOOT:
                result = mOnFootConfidence;
                break;
            case CONFIDENCE_RUNNING:
                result = mRunningConfidence;
                break;
            case CONFIDENCE_WALKING:
                result = mWalkingConfidence;
                break;
            case CONFIDENCE_STILL:
                result = mStillConfidence;
                break;
            case CONFIDENCE_TILTING:
                result = mTiltingConfidence;
                break;
            case CONFIDENCE_UNKNOWN:
                result = mUnknownConfidence;
                break;
            case CONFIDENCE_VEHICLE:
                result = mInVehicleConfidence;
                break;
        }
        return result;
    }

    public Location getLastKnownParkingLocation() {
        return mParkedLocation;
    }

    public boolean isDriving() {
        return mIsDriving;
    }

    public boolean isParked() {
        return mIsParked;
    }

    public List<Address> getLastKnownParkingAddresses() {
        return mParkedAddresses;
    }

    public List<Address> getCurrentAddresses() {
        return mCurrentLocationAddresses;
    }

    public String getCurrentParkedSchedule() {
        if (mParkedLimit == null) {
            return null;
        }
        return mParkedLimit.schedule;
    }

    public GooglePlayConnectionStatus getConnectionStatus() {
        return mConnectionStatus;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mIsStarted) {
            loadDatabase();

            connectToGooglePlayServices();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    /*
    GoogleApi.ConnectionCallbacks
     */
    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");

        changeConnectionStatus(GooglePlayConnectionStatus.CONNECTED);

        // Register IntentFilter for receiving activity updates from the ActivityDetection
        // IntentService.
        IntentFilter filter = new IntentFilter();
        filter.addAction(ActivityDetectionService.ACTION_ACTIVITY_UPDATE);
        registerReceiver(mActivityDetectorReceiver, filter);

        // Register the ActivityDetectionService for activity updates. That service will just
        // forward that data to the mActivityDetectorReceiver.
        Intent intent = new Intent(this, ActivityDetectionService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mClient, 0,
                pendingIntent);

        mLocation = LocationServices.FusedLocationApi.getLastLocation(
                mClient);

        // Request Location Updates
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            // TODO: only request location updates when we're driving.
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
                    5f, this);
        } catch (SecurityException e) {
            // TODO: What happens here.
        }
    }

    /*
    GoogleApi.ConnectionCallbacks
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed");
        changeConnectionStatus(GooglePlayConnectionStatus.FAILED);
    }

    /*
    GoogleApi.ConnectionCallbacks
     */
    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended");
        changeConnectionStatus(GooglePlayConnectionStatus.SUSPENDED);
    }

    /*
    LocationListener Callback
     */
    @Override
    public void onLocationChanged(Location location) {
        mLocation = location;

        mCurrentLocationAddresses = getAddressesForLocation(mLocation);
    }

    /*
    LocationListener Callback
     */
    @Override
    public void onProviderEnabled(String provider) {
        // TODO
    }

    /*
    LocationListener Callback
     */
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO
    }

    /*
    LocationListener Callback
     */
    @Override
    public void onProviderDisabled(String provider) {
        // TODO
    }

    /**
     * @param intent
     * @return SweeperBinder if google play services is available. Otherwise returns null.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class SweeperBinder extends Binder {
        public SweeperService getService(SweeperServiceListener listener) {
            mListener = listener;
            return SweeperService.this;
        }
    }

    private void connectToGooglePlayServices() {
        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this)
                == ConnectionResult.SUCCESS) {
            mClient = new GoogleApiClient.Builder(this)
                    .addApi(ActivityRecognition.API)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mClient.connect();

            changeConnectionStatus(GooglePlayConnectionStatus.CONNECTING);
        } else {
            changeConnectionStatus(GooglePlayConnectionStatus.FAILED);
        }
    }

    private void loadDatabase() {
        try {
            for (int i = 1; i < 10; i++) {
                String filename = "district" + i + ".txt";
                InputStream is=getAssets().open(filename);
                BufferedReader in=
                        new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String str;

                while ((str=in.readLine()) != null) {
                    String[] parsings = str.split("\t");
                    if (parsings.length > 3) {
                        Limit l = new Limit();
                        l.street = parsings[0];
                        l.range = parsings[1];
                        l.limit = parsings[2];
                        l.schedule = "";
                        for (int j = 3; j < parsings.length; j++) {
                            l.schedule += parsings[j];
                        }
                        if (!l.schedule.contains("Not Posted")) {
                            mPostedLimits.add(l);
                        }
                        mLimits.add(l);
                    } else {
                        Log.e(TAG, "Parsed a bad line in " + filename);
                    }
                }

                in.close();
                is.close();
            }
        } catch (IOException e) {

        }

        Log.d(TAG, "Number of Limits posted: " + mPostedLimits.size());
        Log.d(TAG, "Looking for Beryl St...");
        for (Limit l : mPostedLimits) {
            if (l.street.contains("BERYL")) {
                Log.d(TAG, "Found BERYL!");
            }
        }
    }

    private void handleActivityUpdate() {
        if (mInVehicleConfidence > DRIVING_CONFIDENCE) {
            mIsDriving = true;
            mIsParked = false;
        } else if (mInVehicleConfidence < NOT_DRIVING_CONFIDENCE &&
                (mOnFootConfidence > FOOT_CONFIDENCE || mWalkingConfidence > FOOT_CONFIDENCE ||
                mRunningConfidence > FOOT_CONFIDENCE)) {
            if (mIsDriving) {
                mIsParked = true;
                handleParkDetected();
            }
            mIsDriving = false;
        }
    }

    private void handleParkDetected() {
        mParkedLimit = null;

        mParkedLocation = mLocation;

        mParkedAddresses = getAddressesForLocation(mParkedLocation);

        // TODO: SharedPreferences for handling whether or not user wants an update every parking
        // event or only if we park in a bad location.
        sendParkedNotification();

        for (Address address : mParkedAddresses) {
            String street = address.getThoroughfare();
            String housenumber = address.getFeatureName();
            String city = address.getLocality();
            if (housenumber != null && street != null && city != null) {
                if (city.contains("San Diego")) {
                    mParkedLimit = checkAddress(housenumber, street);
                } else {
                    Log.w(TAG, "City is not San Diego. City is " + city);
                }
            }
        }
    }

    private Limit checkAddress(String houseNumber, String street) {
        Log.d(TAG, "housenumber: " + houseNumber + " street " + street);
        Limit result = null;
        for (Limit l : mPostedLimits) {
            if (l.street.toLowerCase().contains(street.toLowerCase())) {
                String[] rangeParsings = l.range.split("-");
                int minRange = Integer.parseInt(rangeParsings[0].trim());
                int maxRange = Integer.parseInt(rangeParsings[1].trim());

                int num = Integer.parseInt(houseNumber.trim());
                if (num >= minRange && num <= maxRange) {
                    result = l;
                    Log.d(TAG, "THIS HOUSE IS IN LIMIT RANGE");
                }
            }
        }
        return result;
    }

    private List<Address> getAddressesForLocation(Location location) {
        long start = System.nanoTime();
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses = new ArrayList<>();

        if (location != null) {
            try {
                addresses = geocoder.getFromLocation(
                        location.getLatitude(),
                        location.getLongitude(),
                        10);
            } catch (IOException ioException) {
                // Catch network or other I/O problems.
            } catch (IllegalArgumentException illegalArgumentException) {
                // Catch invalid latitude or longitude values.
            }
        }
        long end = System.nanoTime();
        Log.d(TAG, "getting Address took: " + (end - start) / 1000000 + "ms");
        return addresses;
    }

    private void sendParkedNotification() {
        // TODO: hard coded strings, Notification builder cleanup and nicer
        if (mParkedLocation != null) {
            String message = "You just parked!";
            if ((System.currentTimeMillis() - mLocation.getTime()) > 60000) {
                message += " Cannot find location!";
            }
            Intent notificationIntent = new Intent(this, MapsActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            notificationIntent.putExtra("location", mParkedLocation);

            PendingIntent intent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setSmallIcon(android.R.drawable.ic_notification_overlay)
                    .setContentTitle("lambda")
                    .setContentText(message)
                    .setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 })
                    .setContentIntent(intent);

            NotificationManager notificationManager = (NotificationManager)
                    getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(0, builder.build());
        }
    }

    private String getActivityString(int type) {
        switch (type) {
            case DetectedActivity.IN_VEHICLE:
                return "In Vehicle";
            case DetectedActivity.ON_BICYCLE:
                return "On Bicycle";
            case DetectedActivity.ON_FOOT:
                return "On Foot";
            case DetectedActivity.WALKING:
                return "Walking";
            case DetectedActivity.STILL:
                return "Still";
            case DetectedActivity.TILTING:
                return "Tilting";
            case DetectedActivity.RUNNING:
                return "Running";
            case DetectedActivity.UNKNOWN:
                return "Unknown";
        }
        return "N/A";
    }

    private void changeConnectionStatus(GooglePlayConnectionStatus status) {
        mConnectionStatus = status;
        if (mListener != null) {
            mListener.onGooglePlayConnectionStatusUpdated(status);
        }
    }

    private final BroadcastReceiver mActivityDetectorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received activity update!");
            String action = intent.getAction();
            if (ActivityDetectionService.ACTION_ACTIVITY_UPDATE.equals(action)) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    mInVehicleConfidence = extras.getInt(CONFIDENCE_VEHICLE);
                    mStillConfidence = extras.getInt(CONFIDENCE_STILL);
                    mWalkingConfidence = extras.getInt(CONFIDENCE_WALKING);
                    mRunningConfidence = extras.getInt(CONFIDENCE_RUNNING);
                    mTiltingConfidence = extras.getInt(CONFIDENCE_TILTING);
                    mUnknownConfidence = extras.getInt(CONFIDENCE_UNKNOWN);
                    mOnBicycleConfidence = extras.getInt(CONFIDENCE_BICYCLE);
                    mOnFootConfidence = extras.getInt(CONFIDENCE_FOOT);

                    handleActivityUpdate();
                }
            }
        }
    };

    public interface SweeperServiceListener {
        void onGooglePlayConnectionStatusUpdated(GooglePlayConnectionStatus status);
    }

    private class Limit {
        String street;
        String range;
        String limit;
        String schedule;
    }
}
