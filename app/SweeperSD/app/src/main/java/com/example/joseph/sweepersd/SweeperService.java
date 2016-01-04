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
import android.os.Handler;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

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

    private Handler mHandler = new Handler();

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
        String schedule = "";
        for (String s : mParkedLimit.schedules) {
            schedule += s;
        }
        return schedule;
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
                        l.schedules = new ArrayList<>();
                        boolean acceptable = true;
                        for (int j = 3; j < parsings.length; j++) {
                            l.schedules.add(parsings[j]);
                            if (parsings[j].contains("Not Posted")) {
                                acceptable = false;
                            }
                            if (!parsings[j].contains("Posted")) {
                                acceptable = false;
                            }
                        }
                        if (acceptable) {
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
            Log.d(TAG, "Street: " + l.street);
            String s = "";
            for (String sc : l.schedules) {
                s += sc;
            }
            Log.d(TAG, "Schedule: " + s);
            for (String schedule : l.schedules) {
                String timeString = getTimeString(schedule);
                if (timeString == null) {
                    Log.e(TAG, "Parse Error on " + l.street + " :: " + l.limit + " :: " + l.schedules);
                } else {
                    String[] parsings = timeString.split("-");
                    int startTime = convertTimeStringToHour(parsings[0]);
                    int endTime = convertTimeStringToHour(parsings[1]);
                    if (startTime > -1 && endTime > -1) {
                        List<GregorianCalendar> days = new ArrayList<>();
                        for (int i = 0; i < l.schedules.size(); i++) {
                            days.addAll(getSweepingDates(startTime, endTime, l.schedules.get(i)));
                        }
                        for (GregorianCalendar d : days) {
                            Log.d(TAG, "month: " + d.get(Calendar.MONTH) +" day: " +
                                    d.get(Calendar.DAY_OF_MONTH) + " (" +
                                    d.get(Calendar.DAY_OF_WEEK) + ") time: " +
                                    d.get(Calendar.HOUR));
                        }
                    } else {
                        Log.e(TAG, "StartTime or endTime was -1: " + startTime + " " + endTime);
                    }
                }
            }
            Log.d(TAG, "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");


            //getDateForLimit(l);
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
        } else if (mNotDrivingChecker == null) {
            mNotDrivingChecker = new Runnable() {
                @Override
                public void run() {
                    if (mInVehicleConfidence <= DRIVING_CONFIDENCE && !mIsParked) {
                        mNotDrivingCounter++;
                        if (mNotDrivingCounter < NOT_DRIVING_CHECK_COUNT) {
                            mHandler.postDelayed(mNotDrivingChecker, NOT_DRIVING_CHECK_INTERVAL);
                        } else {
                            handleParkDetected();
                            mNotDrivingCounter = 0;
                            mNotDrivingChecker = null;
                        }
                    } else {
                        mNotDrivingCounter = 0;
                        mNotDrivingChecker = null;
                    }
                }
            };
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
        mParkedLocation = mLocation = LocationServices.FusedLocationApi.getLastLocation(
                mClient);

        mParkedAddresses = getAddressesForLocation(mParkedLocation);

        mParkedLimit = findLimitForAddresses(mParkedAddresses);

        // TODO: SharedPreferences for handling whether or not user wants an update every parking
        // event or only if we park in a bad location.
        sendParkedNotification();
        if (mParkedLimit != null) {
            //sendParkedInLimitZoneNotification();

            List<GregorianCalendar> days = getSweepingDaysForLimit(mParkedLimit);
            GregorianCalendar today = new GregorianCalendar();

            long timeUntilSweepingMs = Long.MAX_VALUE;
            for (GregorianCalendar c : days) {
                long msTilSweeping = c.getTime().getTime() - today.getTime().getTime();
                if (msTilSweeping < 345600000) {
                    timeUntilSweepingMs = Math.min(msTilSweeping, timeUntilSweepingMs);
                }
            }
            if (timeUntilSweepingMs != Long.MAX_VALUE) {
                // TODO: periodic updated checks on mLastParkedLocation
                sendParkedInRedZoneNotification(timeUntilSweepingMs);
            }
        }
    }



    private List<GregorianCalendar> getSweepingDaysForLimit(Limit l) {
        List<GregorianCalendar> results = new ArrayList<>();
        for (String schedule : mParkedLimit.schedules) {
            String timeString = getTimeString(schedule);
            if (timeString == null) {
                Log.e(TAG, "Parse Error on " + mParkedLimit.street + " :: " + mParkedLimit.limit
                        + " :: " + mParkedLimit.schedules);
            } else {
                String[] parsings = timeString.split("-");
                int startTime = convertTimeStringToHour(parsings[0]);
                int endTime = convertTimeStringToHour(parsings[1]);
                if (startTime > -1 && endTime > -1) {
                    for (int i = 0; i < l.schedules.size(); i++) {
                        results.addAll(getSweepingDates(startTime, endTime, l.schedules.get(i)));
                    }
                    /*for (GregorianCalendar d : results) {
                        Log.d(TAG, "month: " + d.get(Calendar.MONTH) +" day: " +
                                d.get(Calendar.DAY_OF_MONTH) + " (" +
                                d.get(Calendar.DAY_OF_WEEK) + ") time: " +
                                d.get(Calendar.HOUR));
                    }*/
                } else {
                    Log.e(TAG, "StartTime or endTime was -1: " + startTime + " " + endTime);
                }
            }
        }
        return results;
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

    private void sendParkedInRedZoneNotification(long msTilSweeping) {
        // TODO: hard coded strings, Notification builder cleanup and nicer
        if (mParkedLocation != null) {
            long hoursUntilParking = msTilSweeping / 3600000;
            long leftOverMinutes = (msTilSweeping % 3600000) / 60000;
            String message = "Street Sweeping in "
                    + hoursUntilParking + ":" + leftOverMinutes;
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
            if (limit.schedules.contains(day)) {
                if (limit.schedules.contains(first)) {
                    Log.d(TAG, "first " + day);
                }
                if (limit.schedules.contains(second)) {
                    Log.d(TAG, "second " + day);
                }
                if (limit.schedules.contains(third)) {
                    Log.d(TAG, "third " + day);
                }
                if (limit.schedules.contains(fourth)) {
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
        if (limit.schedules.contains(today)) {
            int dayInMonth = c.get(Calendar.DAY_OF_MONTH);
            if (limit.schedules.contains(first) && dayInMonth < 8) {
                Log.d(TAG, "Today on the first " + today);
            } else if (limit.schedules.contains(second) && dayInMonth > 7 && dayInMonth < 15) {
                Log.d(TAG, "Today on the second " + today);
            } else if (limit.schedules.contains(third) && dayInMonth > 14 && dayInMonth < 22) {
                Log.d(TAG, "Today on the third " + today);
            } else if (limit.schedules.contains(fourth) && dayInMonth > 21 && dayInMonth < 29) {
                Log.d(TAG, "Today on the fourth " + today);
            } else {
                Log.d(TAG, "Today any " + tomorrow);
            }
        } else if (limit.schedules.contains(tomorrow)) {
            int dayInMonth = c.get(Calendar.DAY_OF_MONTH) + 1;
            if (limit.schedules.contains(first) && dayInMonth < 8) {
                Log.d(TAG, "Tomorrow on the first " + tomorrow);
            } else if (limit.schedules.contains(second) && dayInMonth > 7 && dayInMonth < 15) {
                Log.d(TAG, "Tomorrow on the second " + tomorrow);
            } else if (limit.schedules.contains(third) && dayInMonth > 14 && dayInMonth < 22) {
                Log.d(TAG, "Tomorrow on the third " + tomorrow);
            } else if (limit.schedules.contains(fourth) && dayInMonth > 21 && dayInMonth < 29) {
                Log.d(TAG, "Tomorrow on the fourth " + tomorrow);
            } else {
                Log.d(TAG, "Tomorrow any " + tomorrow);
            }
        }*/
    }

    private String convertHourToTimeString(int hour) {
        String suffix = hour > 12 ? "pm" : "am";
        int result = hour > 12 ? (hour - 12) : hour;
        return result + suffix;
    }

    /**
     * Example: "7pm", "10am".
     * @param time
     * @return
     */
    private int convertTimeStringToHour(String time) {
        int result = -1;
        String t = time.trim().toLowerCase();
        int base = 0;
        if (t.contains("pm")) {
            base = 12;
        }
        t = t.replace("pm", "");
        t = t.replace("am", "");

        try {
            result = Integer.parseInt(t) + base;
        } catch (NumberFormatException e) {
            Log.d(TAG, "Failed to parse time from: " + time);
        }
        return result;
    }

    private String getTimeString(String schedule) {
        String result = null;
        String[] parsings = schedule.split("\\(");
        for (int i = 1; i < parsings.length; i++) {
            String p = parsings[i];
            String[] parsings2 = p.split("\\)");
            if (parsings2.length == 2) {
                if (result != null) {
                    String[] temp = parsings2[0].split("-");
                    String[] temp2 = result.split("-");
                    int startTime = convertTimeStringToHour(temp[0]);
                    int endTime = convertTimeStringToHour(temp[1]);
                    int startTime2 = convertTimeStringToHour(temp2[0]);
                    int endTime2 = convertTimeStringToHour(temp2[1]);
                    int minStart = Math.min(startTime, startTime2);
                    int maxEnd = Math.max(endTime, endTime2);
                    String startString = convertHourToTimeString(minStart);
                    String endString = convertHourToTimeString(maxEnd);
                    result = startString + " - " + endString;
                } else {
                    result = parsings2[0];
                }
            } else {
                Log.w(TAG, "Failed to get time string from: " + schedule);
            }
        }
        if (parsings.length == 2) {

        } else {
            Log.w(TAG, "Failed to get time string from: " + schedule);
        }
        return result;
    }

    private List<GregorianCalendar> getSweepingDates(int startTime, int endTime, String schedule) {
        List<GregorianCalendar> results = new ArrayList<>();
        String s = schedule.trim().toLowerCase();
        s = s.replace(",", " ");
        s = s.replace(";", " ");
        s = s.replace("  ", " ");
        s = s.trim();
        List<String> words = new ArrayList<>(Arrays.asList(s.split(" ")));
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            int weekdayNumber = getDay(word);
            if (weekdayNumber > 0) {
                List<GregorianCalendar> potentialDays = new ArrayList<>();
                List<GregorianCalendar> potentialResults = new ArrayList<>();
                Calendar calendar = Calendar.getInstance(TimeZone.getDefault());

                for (int j = 0; j < 28; j++) {
                    int dow = calendar.get(Calendar.DAY_OF_WEEK);

                    if (dow == weekdayNumber) {
                        GregorianCalendar c = new GregorianCalendar(calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH),
                                startTime, 0, 0);
                        potentialDays.add(c);
                    }

                    calendar.add(Calendar.DATE, 1);
                }

                potentialResults.addAll(refineDays(words, i, potentialDays));
                if (potentialResults.isEmpty()) {
                    results.addAll(potentialDays);
                } else {
                    results.addAll(potentialResults);
                }
            }
        }
        return results;
    }

    private List<GregorianCalendar> refineDays(List<String> words, int index,
                                               List<GregorianCalendar> unrefinedDays) {
        List<GregorianCalendar> refinedDays = new ArrayList<>();
        String prevWord = getPreviousWord(words, index);
        if (prevWord != null) {
            int prefix = getPrefix(prevWord);
            if (prefix > 0) {
                for (GregorianCalendar day : unrefinedDays) {
                    int dom = day.get(Calendar.DAY_OF_MONTH);
                    int p = dom - ((prefix - 1) * 7);
                    if (p > 0 && p < 8) {
                        refinedDays.add(day);
                    }
                }
                refinedDays.addAll(refineDays(words, index - 1, unrefinedDays));
            } else if (prevWord.equals("&")) {
                refinedDays.addAll(refineDays(words, index - 1, unrefinedDays));
            }
        }
        return refinedDays;
    }

    private String getPreviousWord(List<String> words, int position) {
        String result = null;
        if (position > 0) {
            result = words.get(position - 1);
        }
        return result;
    }

    private int getPrefix(String word) {
        final String first = "1st";
        final String second = "2nd";
        final String third = "3rd";
        final String fourth = "4th";

        int result = 0;
        switch (word) {
            case first:
                result = 1;
                break;
            case second:
                result = 2;
                break;
            case third:
                result = 3;
                break;
            case fourth:
                result = 4;
                break;

        }
        return result;
    }

    private int getDay(String word) {
        int result = 0;
        final String monday = "mon";
        final String tuesday = "tue";
        final String wednesday = "wed";
        final String thursday = "thu";
        final String friday = "fri";
        final String saturday = "sat";
        final String sunday = "sun";
        switch (word) {
            case sunday:
                result = Calendar.SUNDAY;
                break;
            case monday:
                result = Calendar.MONDAY;
                break;
            case tuesday:
                result = Calendar.TUESDAY;
                break;
            case wednesday:
                result = Calendar.WEDNESDAY;
                break;
            case thursday:
                result = Calendar.THURSDAY;
                break;
            case friday:
                result = Calendar.FRIDAY;
                break;
            case saturday:
                result = Calendar.SATURDAY;
                break;

        }
        return result;
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

    private int mNotDrivingCounter = 0;
    private static final int NOT_DRIVING_CHECK_INTERVAL = 5000;
    private static final int NOT_DRIVING_CHECK_COUNT = 60; // 5 minutes.

    /**
     * Class used to detect if the user is not driving over a period of 5 minutes. There is a case
     * where the user might not get out of their car, but sit still for long periods of time. For
     * the sake of this app, we will determine this is a condition in which we're parked. Since
     * other activity detection won't trigger the parked condition, this is another check.
     */
    private Runnable mNotDrivingChecker;

    public interface SweeperServiceListener {
        void onGooglePlayConnectionStatusUpdated(GooglePlayConnectionStatus status);
    }

    private class Limit {
        String street;
        int[] range = new int[2];
        String limit;
        List<String> schedules;
    }
}
