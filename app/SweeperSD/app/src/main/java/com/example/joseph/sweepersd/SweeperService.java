package com.example.joseph.sweepersd;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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

public class SweeperService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener{
    private static final String TAG = SweeperService.class.getSimpleName();

    private GoogleApiClient mClient;
    private LocationManager mLocationManager;

    private boolean mIsConnected = false;
    private boolean mIsStarted = false;
    private boolean mIsDriving = false;
    private boolean mIsParked = false;
    private boolean mHandledPark = false;

    private Location mLocation;
    private long mLocationTimestamp;

    public SweeperService() {
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mIsStarted) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ActivityDetectionService.ACTION_ACTIVITY_UPDATE);
            registerReceiver(receiver, filter);

            if (!mIsConnected) {
                if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this)
                        == ConnectionResult.SUCCESS) {
                    mClient = new GoogleApiClient.Builder(this)
                            .addApi(ActivityRecognition.API)
                            .addApi(LocationServices.API)
                            .addConnectionCallbacks(this)
                            .addOnConnectionFailedListener(this)
                            .build();
                    mClient.connect();
                }
            }
            // TODO: What if google play services is not available?

            mIsStarted = true;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");
        mIsConnected = true;
        Intent intent = new Intent(this, ActivityDetectionService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mClient, 0,
                pendingIntent);

        mLocation = LocationServices.FusedLocationApi.getLastLocation(
                mClient);

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
                    5f, this);
        } catch (SecurityException e) {

        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed");
        mIsConnected = false;
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended");
        mIsConnected = false;
    }



    @Override
    public void onLocationChanged(Location location) {
        mLocation = location;
    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void handleParkDetected() {
        SweeperSDApplication.setParkedLocation(mLocation, System.currentTimeMillis());
        if (mLocation != null) {
            String message = "You just parked!";
            if ((System.currentTimeMillis() - mLocation.getTime()) > 60000) {
                message += " Cannot find location!";
            }
            Intent notificationIntent = new Intent(this, MapsActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            notificationIntent.putExtra("location", SweeperSDApplication.getParkedLocation());

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

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ActivityDetectionService.ACTION_ACTIVITY_UPDATE.equals(action)) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    int vehicleConfidence = extras.getInt(
                            ActivityDetectionService.CONFIDENCE_VEHICLE);


                    if (vehicleConfidence > 85) {
                        mIsDriving = true;
                        mIsParked = false;
                    } else if (vehicleConfidence < 20) {
                        if (mIsDriving) {
                            mIsParked = true;
                            handleParkDetected();
                        }
                        mIsDriving = false;
                    }
                }
            }
        }
    };
}
