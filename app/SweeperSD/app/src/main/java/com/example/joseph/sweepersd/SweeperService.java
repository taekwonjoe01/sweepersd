package com.example.joseph.sweepersd;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.example.joseph.sweepersd.model.alarms.SweepingAddress;
import com.example.joseph.sweepersd.model.limits.Limit;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SweeperService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener,
        ParkDetectionManager.ParkDetectionListener {
    private static final String TAG = SweeperService.class.getSimpleName();

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
    private DrivingLocationListener mDrivingLocationListener;
    private boolean mIsStarted = false;
    private ParkDetectionManager mParkManager;

    private volatile SweepingAddress mSweepingAddress;
    private volatile long mLocationTimestamp = Long.MAX_VALUE;

    private final IBinder mBinder = new SweeperBinder();

    private volatile boolean mIsDriving = false;

    private List<SweepingAddress> mPotentialParkedLocations = new ArrayList<>();

    private Handler mHandler = new Handler();

    private long mRedzoneLimit = 64800000;

    public SweeperService() {
    }

    public int getConfidence(String confidence) {
        return mParkManager.getConfidence(confidence);
    }

    public ParkDetectionManager.Status getParkDetectionStatus() {
        return mParkManager.getStatus();
    }

    public boolean isDriving() {
        return mIsDriving;
    }

    public GooglePlayConnectionStatus getConnectionStatus() {
        return mConnectionStatus;
    }

    public ParkDetectionManager.ParkDetectionSettings getParkDetectionSettings() {
        return mParkManager.getParkDetectionSettings();
    }

    public SweepingAddress getCurrentLocationDetails() {
        return mSweepingAddress;
    }

    public List<SweepingAddress> getParkedLocationDetails() {
        return mPotentialParkedLocations;
    }

    public void registerDrivingLocationListener(DrivingLocationListener listener) {
        mDrivingLocationListener = listener;
        requestLocationUpdates();
    }

    public long getRedzoneLimit() {
        return mRedzoneLimit;
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

        /*mSweepingAddress = SweepingAddress.createFromLocation(this, LocationServices.FusedLocationApi
                .getLastLocation(mClient));*/
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
        /*mSweepingAddress = SweepingAddress.createFromLocation(this, location);*/

        if (mDrivingLocationListener != null && isDriving()) {
            mDrivingLocationListener.onLocationChanged(mSweepingAddress);
        }
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

    @Override
    public boolean onUnbind(Intent intent) {
        mDrivingLocationListener = null;
        requestLocationUpdates();
        return super.onUnbind(intent);
    }

    public class SweeperBinder extends Binder {
        public SweeperService getService(SweeperServiceListener listener) {
            mListener = listener;
            return SweeperService.this;
        }
    }

    public interface SweeperServiceListener {
        void onGooglePlayConnectionStatusUpdated(GooglePlayConnectionStatus status);
        void onParked(List<SweepingAddress> results);
        void onDriving();
    }

    public interface DrivingLocationListener {
        void onLocationChanged(SweepingAddress location);
    }


    /**
     * Called when it is determined we are parked. This will only be called once per detected park
     * session.
     */
    @Override
    public void onPark() {
        mIsDriving = false;

        stopLocationUpdates();

        mPotentialParkedLocations.add(mSweepingAddress);

        handleParkingResults(mPotentialParkedLocations);

        if (mListener != null) {
            mListener.onParked(mPotentialParkedLocations);
        }
    }

    /**
     * Called when it is determined we are driving. This will only be called once per detected
     * driving session.
     */
    @Override
    public void onDriving() {
        mIsDriving = true;
        requestLocationUpdates();

        mPotentialParkedLocations.clear();


        if (mListener != null) {
            mListener.onDriving();
        }
    }

    @Override
    public void onParkPossible() {
        mPotentialParkedLocations.add(mSweepingAddress);
    }

    private void requestLocationUpdates() {
        if (mIsDriving) {
            long minTime = mDrivingLocationListener != null ? 250 : 2000;
            mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            try {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime,
                        5f, this);
            } catch (SecurityException e) {
                // TODO: What happens here.
            }
        }
    }

    private void stopLocationUpdates() {
        if (!mIsDriving) {
            try {
                mLocationManager.removeUpdates(this);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean checkLocationPermissions() {
        boolean coarsePermission = false;
        boolean finePermission = false;
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            SweeperSDApplication.needsCoarsePermission = true;
        } else {
            SweeperSDApplication.needsCoarsePermission = false;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
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
        LimitManager.loadLimits(this);
        Log.d(TAG, "mPostedLimits size " + LimitManager.getPostedLimits().size());
    }



    private void handleParkingResults(List<SweepingAddress> parkingResults) {
        /*mRedzoneLimit = SettingsUtils.getRedzoneLimit(this);
        Log.d(TAG, "redzone limit time " + mRedzoneLimit);
        List<Limit> potentialParkingLimits = new ArrayList<>();

        for (SweepingAddress location : parkingResults) {
            if (location.limit != null) {
                potentialParkingLimits.add(location.limit);
            }
        }

        // handle notifications
        Log.d(TAG, "potentialParkingLimits size " + potentialParkingLimits.size());
        NotificationPresenter.sendParkedNotificationIfEnabled(this);

        long minTime = Long.MAX_VALUE;
        for (Limit l : potentialParkingLimits) {
            List<Long> timesUntilSweeping = LocationUtils.getTimesUntilLimit(l, 31);
            for (Long time : timesUntilSweeping) {
                minTime = Math.min(time, minTime);
            }
        }
        Log.d(TAG, "timeUntilSweepingMs " + minTime);
        if (minTime < 0) {
            NotificationPresenter.sendRedzoneNotification(this);
        } else if (minTime < mRedzoneLimit) {
            NotificationPresenter.sendRedzoneWarningNotification(this);
        }*/
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
}
