package com.example.joseph.sweepersd;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

public class SweeperService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener,
        ParkDetectionManager.ParkDetectionListener {
    private static final String TAG = SweeperService.class.getSimpleName();

    // TODO: Add threshold meter for controlling when these notifications happen.
    /**
     * TODO LIST BEFORE COMMIT:
     * 1. Settings for ParkDetectionManager
     * 2. Way to add Limits manually for testing
     * 3. Fix simulation of parking logic
     */

    private static final int NOTIFICATION_PARKED_ID = 0;
    private static final int NOTIFICATION_PARKED_LIMIT_ID = 1;
    private static final int NOTIFICATION_PARKED_REDZONE_ID = 2;
    private static final int MY_PERMISSION_ACCESS_COURSE_LOCATION = 0;
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 1;

    public enum GooglePlayConnectionStatus {
        DISCONNECTED, CONNECTING, CONNECTED, FAILED, SUSPENDED
    }

    private volatile GooglePlayConnectionStatus mConnectionStatus =
            GooglePlayConnectionStatus.DISCONNECTED;
    private GoogleApiClient mClient;
    private LocationManager mLocationManager;
    private SweeperServiceListener mListener;
    private boolean mIsStarted = false;
    private ParkDetectionManager mParkManager;

    private volatile Location mLocation;
    private volatile long mLocationTimestamp = Long.MAX_VALUE;

    private final IBinder mBinder = new SweeperBinder();

    private volatile boolean mIsDriving = false;
    private volatile boolean mIsParked = false;

    private List<Limit> mLimits = new ArrayList<>();
    private List<Limit> mPostedLimits = new ArrayList<>();

    private List<Location> mPotentialParkedLocations = new ArrayList<>();

    private Handler mHandler = new Handler();

    public SweeperService() {
    }

    public int getConfidence(String confidence) {
        return mParkManager.getConfidence(confidence);
    }

    public List<Location> getPotentialParkingLocations() {
        return mPotentialParkedLocations;
    }

    public ParkDetectionManager.Status getParkDetectionStatus() {
        return mParkManager.getStatus();
    }

    public boolean isDriving() {
        return mIsDriving;
    }

    public boolean isParked() {
        return mIsParked;
    }

    public List<Address> getParkingAddressesForLocation(Location location) {
        List<Address> results = new ArrayList<>();
        results = getAddressesForLocation(location);
        return results;
    }

    public GooglePlayConnectionStatus getConnectionStatus() {
        return mConnectionStatus;
    }

    public ParkDetectionManager.ParkDetectionSettings getParkDetectionSettings() {
        return mParkManager.getParkDetectionSettings();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mIsStarted) {
            loadDatabase();

            mParkManager = new ParkDetectionManager(this);

            connectToGooglePlayServices();

            mIsStarted = true;
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

        mParkManager.startParkDetection(this, mClient);

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
        // TODO - hmmm
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

    /**
     * Called when it is determined we are parked. This will only be called once per detected park
     * session.
     */
    @Override
    public void onPark() {
        if (checkLocationPermissions()) {
            try {
                mLocationManager.removeUpdates(this);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        mPotentialParkedLocations.add(mLocation);

        mIsDriving = false;
        mIsParked = true;
        handleParkDetected();
    }

    /**
     * Called when it is determined we are driving. This will only be called once per detected
     * driving session.
     */
    @Override
    public void onDriving() {
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000,
                    5f, this);
        } catch (SecurityException e) {
            // TODO: What happens here.
        }

        mPotentialParkedLocations.clear();

        mIsDriving = true;
        mIsParked = false;
    }

    @Override
    public void onParkPossible() {
        mPotentialParkedLocations.add(mLocation);
    }

    private boolean checkLocationPermissions() {
        boolean coarsePermission = false;
        boolean finePermission = false;
        /*if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSION_ACCESS_COURSE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSION_ACCESS_COURSE_LOCATION);
        }*/
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            SweeperSDApplication.needsCoarsePermission = true;
        } else {
            SweeperSDApplication.needsCoarsePermission = false;
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            SweeperSDApplication.needsFinePermission = true;
        } else {
            SweeperSDApplication.needsFinePermission = false;
        }
        return (coarsePermission && finePermission);
    }

    private void connectToGooglePlayServices() {
        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this)
                == ConnectionResult.SUCCESS) {
            mClient = new GoogleApiClient.Builder(this)
                    .addApi(ActivityRecognition.API)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addConnectionCallbacks(mParkManager)
                    .addOnConnectionFailedListener(this)
                    .build();
            mClient.connect();

            changeConnectionStatus(GooglePlayConnectionStatus.CONNECTING);
        } else {
            changeConnectionStatus(GooglePlayConnectionStatus.FAILED);
        }
    }

    private void loadDatabase() {
        mLimits = LimitManager.loadLimits(this);

        mPostedLimits = LimitManager.getPostedLimits();
        Log.d(TAG, "mPostedLimits size " + mPostedLimits.size());

        new Thread(){
            @Override
            public void run() {
                for (Limit l : mPostedLimits) {
                    String startAddress = l.getRange()[0] + " " + l.getStreet() + ", San Diego, CA";
                    LatLng start = getLocationFromAddress(startAddress);
                    String endAddress = l.getRange()[1] + " " + l.getStreet() + ", San Diego, CA";
                    LatLng end = getLocationFromAddress(endAddress);
                    l.setStartLatLng(start);
                    l.setEndLatLng(end);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {

                    }
                }
            }
        }.start();
        Log.d(TAG, "finished loading database");
    }

    private void handleParkDetected() {
        long redzoneLimit = Long.parseLong(
                PreferenceManager.getDefaultSharedPreferences(this).getString(
                        SettingsActivity.PREF_KEY_REDZONE_WARNING_TIME, "64800000"));
        Log.d(TAG, "redzone limit time " + redzoneLimit);
        List<Limit> potentialParkingLimits = new ArrayList<>();

        for (Location location : mPotentialParkedLocations) {

            List<Address> addressesForLimit = getAddressesForLocation(location);
            Log.d(TAG, "addressForLimit size " + addressesForLimit.size());

            Limit limitForLocation = findLimitForAddresses(addressesForLimit);
            if (limitForLocation != null) {
                potentialParkingLimits.add(limitForLocation);
            }
        }
        Log.d(TAG, "potentialParkingLimits size " + potentialParkingLimits.size());
        sendParkedNotification();

        long timeUntilSweepingMs = Long.MAX_VALUE;
        for (Limit l : potentialParkingLimits) {
            List<GregorianCalendar> days = getSweepingDaysForLimit(l);
            GregorianCalendar today = new GregorianCalendar();

            for (GregorianCalendar c : days) {
                long msTilSweeping = c.getTime().getTime() - today.getTime().getTime();
                Log.d(TAG, "msTilSweeping: " + msTilSweeping);
                if (msTilSweeping < redzoneLimit) {
                    timeUntilSweepingMs = Math.min(msTilSweeping, timeUntilSweepingMs);
                }
            }
        }
        Log.d(TAG, "timeUntilSweepingMs " + timeUntilSweepingMs);
        if (timeUntilSweepingMs != Long.MAX_VALUE) {
            sendParkedInRedZoneNotification(timeUntilSweepingMs);
        }
    }

    private List<GregorianCalendar> getSweepingDaysForLimit(Limit l) {
        List<GregorianCalendar> results = new ArrayList<>();
        for (String schedule : l.getSchedules()) {
            String timeString = LimitManager.getTimeString(schedule);
            if (timeString == null) {
                Log.e(TAG, "Parse Error on " + l.getStreet() + " :: " + l.getLimit()
                        + " :: " + l.getSchedules());
            } else {
                String[] parsings = timeString.split("-");
                int startTime = LimitManager.convertTimeStringToHour(parsings[0]);
                int endTime = LimitManager.convertTimeStringToHour(parsings[1]);
                if (startTime > -1 && endTime > -1) {
                    for (int i = 0; i < l.getSchedules().size(); i++) {
                        results.addAll(LimitManager.getSweepingDates(startTime, endTime,
                                l.getSchedules().get(i)));
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
            Log.d(TAG, "findLimitForAddress: " + a);
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
        Log.d(TAG, "houseNumber: " + houseNumber + " - Street: " + street);
        Limit result = null;
        for (Limit l : mPostedLimits) {
            if (l.getStreet().toLowerCase().contains(street)) {
                if (houseNumber >= l.getRange()[0] && houseNumber <= l.getRange()[1]) {
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

    private LatLng getLocationFromAddress(String strAddress) {
        Geocoder coder = new Geocoder(this);
        List<Address> address;
        LatLng result = null;

        try {
            address = coder.getFromLocationName(strAddress, 5);
            if (address != null && !address.isEmpty()) {
                Address location = address.get(0);
                location.getLatitude();
                location.getLongitude();

                result = new LatLng(location.getLatitude(), location.getLongitude());
            } else {
                Log.d(TAG, "No location for: " + strAddress);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    private void sendParkedNotification() {
        // TODO: hard coded strings, Notification builder cleanup and nicer
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        boolean parkNotificationEnabled = settings.getBoolean(
                SettingsActivity.PREF_KEY_RECEIVE_PARK_NOTIFICATIONS, false);
        Log.d(TAG, "Sending parked notification? " + parkNotificationEnabled);
        if (parkNotificationEnabled && !mPotentialParkedLocations.isEmpty()) {
            String message = "You just parked!";
            Intent notificationIntent = new Intent(this, MapsActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            notificationIntent.putExtra("location", mPotentialParkedLocations.get(
                    mPotentialParkedLocations.size() - 1));

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
        if (!mPotentialParkedLocations.isEmpty()) {
            String message = "You just parked in a street sweeping zone!";
            Intent notificationIntent = new Intent(this, MapsActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            notificationIntent.putExtra("location", mPotentialParkedLocations.get(
                    mPotentialParkedLocations.size() - 1));

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
        if (!mPotentialParkedLocations.isEmpty()) {
            long hoursUntilParking = msTilSweeping / 3600000;
            long leftOverMinutes = (msTilSweeping % 3600000) / 60000;
            long daysUntilSweeping = hoursUntilParking / 24;
            long leftOverHours = hoursUntilParking % 24;
            String message = "Street Sweeping in " + daysUntilSweeping + " days, "
                    + leftOverHours + " hours, and " + leftOverMinutes + " minutes.";
            Intent notificationIntent = new Intent(this, MapsActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            notificationIntent.putExtra("location", mPotentialParkedLocations.get(
                    mPotentialParkedLocations.size() - 1));

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
            if (limit.getSchedules().contains(day)) {
                if (limit.getSchedules().contains(first)) {
                    Log.d(TAG, "first " + day);
                }
                if (limit.getSchedules().contains(second)) {
                    Log.d(TAG, "second " + day);
                }
                if (limit.getSchedules().contains(third)) {
                    Log.d(TAG, "third " + day);
                }
                if (limit.getSchedules().contains(fourth)) {
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
}
