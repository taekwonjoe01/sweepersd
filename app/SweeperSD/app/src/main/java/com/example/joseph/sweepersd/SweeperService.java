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
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SweeperService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final String TAG = SweeperService.class.getSimpleName();
    private static final int DRIVING_CONFIDENCE = 85;
    private static final int NOT_DRIVING_CONFIDENCE = 20;
    // TODO: Add threshold meter for controlling when these notifications happen.
    private static final int FOOT_CONFIDENCE = 30;
    public static final String CONFIDENCE_VEHICLE = "CONFIDENCE_VEHICLE";
    public static final String CONFIDENCE_FOOT = "CONFIDENCE_FOOT";
    public static final String CONFIDENCE_BICYCLE = "CONFIDENCE_BICYCLE";
    public static final String CONFIDENCE_WALKING = "CONFIDENCE_WALKING";
    public static final String CONFIDENCE_RUNNING = "CONFIDENCE_RUNNING";
    public static final String CONFIDENCE_TILTING = "CONFIDENCE_TILTING";
    public static final String CONFIDENCE_STILL = "CONFIDENCE_STILL";
    public static final String CONFIDENCE_UNKNOWN = "CONFIDENCE_UNKNOWN";
    private static final int NOTIFICATION_PARKED_ID = 0;
    private static final int NOTIFICATION_PARKED_LIMIT_ID = 1;
    private static final int NOTIFICATION_PARKED_REDZONE_ID = 2;

    public enum GooglePlayConnectionStatus {
        DISCONNECTED, CONNECTING, CONNECTED, FAILED, SUSPENDED
    }

    private volatile GooglePlayConnectionStatus mConnectionStatus =
            GooglePlayConnectionStatus.DISCONNECTED;
    private GoogleApiClient mClient;
    private LocationManager mLocationManager;
    private SweeperServiceListener mListener;
    private boolean mIsStarted = false;

    private volatile Location mLocation;
    private volatile long mLocationTimestamp = Long.MAX_VALUE;

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

            mIsStarted = true;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private PendingIntent mActivityRecognitionIntent;
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
        mActivityRecognitionIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mClient, 30000,
                mActivityRecognitionIntent);

        mLocation = LocationServices.FusedLocationApi.getLastLocation(
                mClient);
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
        mLocationTimestamp = System.currentTimeMillis();
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
                        String[] rangeParsings = parsings[1].split("-");
                        l.range[0] = Integer.parseInt(rangeParsings[0].trim());
                        l.range[1] = Integer.parseInt(rangeParsings[1].trim());
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

        for (Limit l : mPostedLimits) {
            Log.d(TAG, "Checking " + l.schedule);
            getDateForLimit(l);
        }
    }

    private void handleActivityUpdate() {
        if (mInVehicleConfidence > DRIVING_CONFIDENCE) {
            if (!mIsDriving) {
                // Request Location Updates
                mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                try {
                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000,
                            5f, this);
                } catch (SecurityException e) {
                    // TODO: What happens here.
                }

                // Speed up activity detection updates
                ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mClient,
                        mActivityRecognitionIntent);
                ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mClient, 1000,
                        mActivityRecognitionIntent);
            }
            mIsDriving = true;
            mIsParked = false;
        } else if (mInVehicleConfidence < NOT_DRIVING_CONFIDENCE &&
                (mOnFootConfidence > FOOT_CONFIDENCE || mWalkingConfidence > FOOT_CONFIDENCE ||
                mRunningConfidence > FOOT_CONFIDENCE)) {
            if (mIsDriving) {
                try {
                    mLocationManager.removeUpdates(this);
                } catch (SecurityException e) {
                    // TODO: What happens here.
                }

                ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mClient,
                        mActivityRecognitionIntent);
                ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mClient, 30000,
                        mActivityRecognitionIntent);

                mIsParked = true;
                handleParkDetected();
            }
            mIsDriving = false;
        }
    }

    private void handleParkDetected() {
        long time = System.currentTimeMillis();
        if (time - mLocationTimestamp < 15000) {
            // TODO: Handle good data here.
        } else {
            // TODO: What if the location is more than 15 seconds old?
            Log.w(TAG, "Current location hasn't been updated in more than 15 seconds! " +
                    "May be out of date!");
        }
        mParkedLimit = null;
        mParkedLocation = mLocation;

        mParkedAddresses = getAddressesForLocation(mParkedLocation);

        mParkedLimit = findLimitForAddresses(mParkedAddresses);

        // TODO: SharedPreferences for handling whether or not user wants an update every parking
        // event or only if we park in a bad location.
        sendParkedNotification();
        if (mParkedLimit != null) {
            sendParkedInLimitZoneNotification();
        }
    }

    private Limit findLimitForAddresses(List<Address> addresses) {
        Limit result = null;
        for (Address address : addresses) {
            String a = "";
            for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                a += address.getAddressLine(i) + ",";
            }
            a = a.toLowerCase();
            // TODO: This should check more dynamically the city.
            if (a.contains("ca") && a.contains("san diego")) {
                String[] split = a.split(",");
                if (split.length > 1) {
                    String streetAddress = split[0];
                    String[] streetAddressParsings = streetAddress.split(" ");
                    if (streetAddressParsings.length > 1) {
                        String streetNumber = streetAddressParsings[0];
                        String streetName = "";
                        for (int j = 1; j < streetAddressParsings.length; j++) {
                            streetName += " " + streetAddressParsings[j];
                        }
                        streetName = streetName.trim();
                        if (streetNumber.contains("-")) {
                            String[] streetNumberParsings = streetNumber.split("-");
                            if (streetNumberParsings.length == 2) {
                                try {
                                    int minNum = Integer.parseInt(streetNumberParsings[0]);
                                    int maxNum = Integer.parseInt(streetNumberParsings[1]);

                                    Limit minLimit = checkAddress(minNum, streetName);
                                    Limit maxLimit = checkAddress(maxNum, streetName);
                                    result = (minLimit != null) ? minLimit :
                                            (maxLimit != null) ? maxLimit : null;
                                } catch (NumberFormatException e) {
                                    Log.e(TAG, "Malformed Street numbers: " + streetNumber);
                                }
                            } else {
                                Log.e(TAG, "Malformed Street numbers: " + streetNumber);
                            }
                        } else {
                            try {
                                int num = Integer.parseInt(streetNumber);
                                Limit l = checkAddress(num, streetName);
                                result = (l != null) ? l : null;
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Malformed Street numbers: " + streetNumber);
                            }
                        }
                    } else {
                        Log.e(TAG, "Malformed street address " + streetAddress);
                    }
                } else {
                    Log.e(TAG, "Malformed address " + a);
                }
            } else {
                Log.w(TAG, "City is not San Diego. Address is  " + a);
            }
        }
        return result;
    }

    private Limit checkAddress(int houseNumber, String street) {
        Log.d(TAG, "houseNumber: " + houseNumber + " street " + street);
        Limit result = null;
        for (Limit l : mPostedLimits) {
            if (l.street.toLowerCase().contains(street)) {
                if (houseNumber >= l.range[0] && houseNumber <= l.range[1]) {
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
                // TODO: Catch network or other I/O problems.
            } catch (IllegalArgumentException illegalArgumentException) {
                // TODO: Catch invalid latitude or longitude values.
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
            Intent notificationIntent = new Intent(this, MapsActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            notificationIntent.putExtra("location", mParkedLocation);

            PendingIntent intent = PendingIntent.getActivity(this, 0,
                    notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setSmallIcon(android.R.drawable.ic_notification_overlay)
                    .setContentTitle("lambda")
                    .setContentText(message)
                    .setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 })
                    .setContentIntent(intent);

            NotificationManager notificationManager = (NotificationManager)
                    getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_PARKED_ID, builder.build());
        }
    }

    private void sendParkedInLimitZoneNotification() {
        // TODO: hard coded strings, Notification builder cleanup and nicer
        if (mParkedLocation != null) {
            String message = "You just parked in a street sweeping zone!";
            Intent notificationIntent = new Intent(this, MapsActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            notificationIntent.putExtra("location", mParkedLocation);

            PendingIntent intent = PendingIntent.getActivity(this, 0,
                    notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setSmallIcon(android.R.drawable.ic_notification_overlay)
                    .setContentTitle("lambda")
                    .setContentText(message)
                    .setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 })
                    .setContentIntent(intent);

            NotificationManager notificationManager = (NotificationManager)
                    getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_PARKED_LIMIT_ID, builder.build());
        }
    }

    private void sendParkedInRedZoneNotification() {
        // TODO: hard coded strings, Notification builder cleanup and nicer
        if (mParkedLocation != null) {
            String message = "Your parking spot is scheduled for street sweeping!";
            Intent notificationIntent = new Intent(this, MapsActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            notificationIntent.putExtra("location", mParkedLocation);

            PendingIntent intent = PendingIntent.getActivity(this, 0,
                    notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setSmallIcon(android.R.drawable.ic_notification_overlay)
                    .setContentTitle("lambda")
                    .setContentText(message)
                    .setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 })
                    .setContentIntent(intent);

            NotificationManager notificationManager = (NotificationManager)
                    getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_PARKED_REDZONE_ID, builder.build());
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

    private void getDateForLimit(Limit limit) {
        String first = "1st";
        String second = "2nd";
        String third = "3rd";
        String fourth = "4th";

        String monday = "Mon";
        String tuesday = "Tue";
        String wednesday = "Wed";
        String thursday = "Thu";
        String friday = "Fri";
        String saturday = "Sat";
        String sunday = "Sun";

        for (int i = 1; i < 8; i++) {
            String day = getDayStringForDay(i);
            if (limit.schedule.contains(day)) {
                if (limit.schedule.contains(first)) {
                    Log.d(TAG, "first " + day);
                }
                if (limit.schedule.contains(second)) {
                    Log.d(TAG, "second " + day);
                }
                if (limit.schedule.contains(third)) {
                    Log.d(TAG, "third " + day);
                }
                if (limit.schedule.contains(fourth)) {
                    Log.d(TAG, "fourth " + day);
                }
                Log.d(TAG, "Contains " + day);
            }
        }

        /*Calendar c = Calendar.getInstance(TimeZone.getDefault());
        int day = c.get(Calendar.DAY_OF_WEEK);
        String today = getDayStringForDay(day);
        String tomorrow = getDayStringForDay((day % 7) + 1 );
        // TODO: SharedPreference for setting time buffer of warnings
        if (limit.schedule.contains(today)) {
            int dayInMonth = c.get(Calendar.DAY_OF_MONTH);
            if (limit.schedule.contains(first) && dayInMonth < 8) {
                Log.d(TAG, "Today on the first " + today);
            } else if (limit.schedule.contains(second) && dayInMonth > 7 && dayInMonth < 15) {
                Log.d(TAG, "Today on the second " + today);
            } else if (limit.schedule.contains(third) && dayInMonth > 14 && dayInMonth < 22) {
                Log.d(TAG, "Today on the third " + today);
            } else if (limit.schedule.contains(fourth) && dayInMonth > 21 && dayInMonth < 29) {
                Log.d(TAG, "Today on the fourth " + today);
            } else {
                Log.d(TAG, "Today any " + tomorrow);
            }
        } else if (limit.schedule.contains(tomorrow)) {
            int dayInMonth = c.get(Calendar.DAY_OF_MONTH) + 1;
            if (limit.schedule.contains(first) && dayInMonth < 8) {
                Log.d(TAG, "Tomorrow on the first " + tomorrow);
            } else if (limit.schedule.contains(second) && dayInMonth > 7 && dayInMonth < 15) {
                Log.d(TAG, "Tomorrow on the second " + tomorrow);
            } else if (limit.schedule.contains(third) && dayInMonth > 14 && dayInMonth < 22) {
                Log.d(TAG, "Tomorrow on the third " + tomorrow);
            } else if (limit.schedule.contains(fourth) && dayInMonth > 21 && dayInMonth < 29) {
                Log.d(TAG, "Tomorrow on the fourth " + tomorrow);
            } else {
                Log.d(TAG, "Tomorrow any " + tomorrow);
            }
        }*/
    }

    private String getDayStringForDay(int day) {
        String result = null;
        switch (day) {
            case Calendar.SUNDAY:
                result = "Sun";
                break;
            case Calendar.MONDAY:
                result = "Mon";
                break;
            case Calendar.TUESDAY:
                result = "Tue";
                break;
            case Calendar.WEDNESDAY:
                result = "Wed";
                break;
            case Calendar.THURSDAY:
                result = "Thu";
                break;
            case Calendar.FRIDAY:
                result = "Fri";
                break;
            case Calendar.SATURDAY:
                result = "Sat";
                break;

        }
        return result;
    }

    public interface SweeperServiceListener {
        void onGooglePlayConnectionStatusUpdated(GooglePlayConnectionStatus status);
    }

    private class Limit {
        String street;
        int[] range = new int[2];
        String limit;
        String schedule;
    }
}
