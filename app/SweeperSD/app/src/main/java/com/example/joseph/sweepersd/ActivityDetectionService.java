package com.example.joseph.sweepersd;

import android.Manifest;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationServices;

import java.util.List;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class ActivityDetectionService extends IntentService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final String TAG = ActivityDetectionService.class.getSimpleName();

    private int mMaxConfidenceThreshold = 90; // TODO: make this adjustable
    private int mMinConfidenceThreshold = 40;

    private GoogleApiClient mGoogleApiClient;

    public ActivityDetectionService() {
        super("ActivityDetectionService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            if (ActivityRecognitionResult.hasResult(intent)) {
                ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

                handleActivity(result);
            }
        }
    }

    private void handleActivity(ActivityRecognitionResult result) {
        Log.d(TAG, "handling Activity...");
        List<DetectedActivity> activities = result.getProbableActivities();
        int vehicleConfidence = 0;
        int footConfidence = 0;
        int bicycleConfidence = 0;
        int walkingConfidence = 0;
        int stillConfidence = 0;
        int tiltingConfidence = 0;
        int runningConfidence = 0;
        int unknownConfidence = 0;
        for (DetectedActivity activity : activities) {
            int type = activity.getType();
            switch (type) {
                case DetectedActivity.IN_VEHICLE:
                    vehicleConfidence = activity.getConfidence();
                    break;
                case DetectedActivity.ON_BICYCLE:
                    bicycleConfidence = activity.getConfidence();
                    break;
                case DetectedActivity.ON_FOOT:
                    footConfidence = activity.getConfidence();
                    break;
                case DetectedActivity.WALKING:
                    walkingConfidence = activity.getConfidence();
                    break;
                case DetectedActivity.STILL:
                    stillConfidence = activity.getConfidence();
                    break;
                case DetectedActivity.TILTING:
                    tiltingConfidence = activity.getConfidence();
                    break;
                case DetectedActivity.RUNNING:
                    runningConfidence = activity.getConfidence();
                    break;
                case DetectedActivity.UNKNOWN:
                    unknownConfidence = activity.getConfidence();
                    break;
            }
        }
        SweeperSDApplication.setBicycleConfidence(bicycleConfidence);
        SweeperSDApplication.setVehicleConfidence(vehicleConfidence);
        SweeperSDApplication.setFootConfidence(footConfidence);
        SweeperSDApplication.setWalkingConfidence(walkingConfidence);
        SweeperSDApplication.setStillConfidence(stillConfidence);
        SweeperSDApplication.setTiltingConfidence(tiltingConfidence);
        SweeperSDApplication.setRunningConfidence(runningConfidence);
        SweeperSDApplication.setUnknownConfidence(unknownConfidence);


        long currentTime = System.currentTimeMillis();
        /*long timeSinceDriving = currentTime - SweeperSDApplication.getDrivingTimestamp();
        if (timeSinceDriving < 60000L &&
                vehicleConfidence < mMinConfidenceThreshold) {
            long timeSinceLastPark = currentTime - SweeperSDApplication.getParkedTimestamp();
            if (timeSinceLastPark > 60000L) {
                handleParkDetected();
            }
        }*/
        if (vehicleConfidence >= mMaxConfidenceThreshold) {
            SweeperSDApplication.setDrivingTimestamp(currentTime);
        }


        if (SweeperSDApplication.getVehicleConfidence() > 85) {
            SweeperSDApplication.setIsDriving(true);
            SweeperSDApplication.setParkingDetected(false);
        } else if (SweeperSDApplication.getVehicleConfidence() < 20 &&
                SweeperSDApplication.getFootConfidence() > 45) {
            if (SweeperSDApplication.isDriving()) {
                SweeperSDApplication.setParkingDetected(true);
                handleParkDetected();
            }
            SweeperSDApplication.setIsDriving(false);
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

    private void handleParkDetected() {
        SweeperSDApplication.setParkingDetected(true);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
        /*if (!mGoogleApiClient.isConnecting() || !mGoogleApiClient.isConnected()) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            mGoogleApiClient.connect();
        } else if (mGoogleApiClient.isConnected()) {
            findParkedLocation();
        }*/
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");
        findParkedLocation();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed");
    }

    private LocationManager mLocationManager;
    private void findParkedLocation() {
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
                    0, this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        SweeperSDApplication.setParkedLocation(LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient), System.currentTimeMillis());
        if (SweeperSDApplication.getParkedLocation() != null) {
            Intent notificationIntent = new Intent(this, MapsActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            notificationIntent.putExtra("location", SweeperSDApplication.getParkedLocation());

            PendingIntent intent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);


            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setSmallIcon(android.R.drawable.ic_notification_overlay)
                    .setContentTitle("lambda")
                    .setContentText("You just parked!")
                    .setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 })
                    .setContentIntent(intent);

            NotificationManager notificationManager = (NotificationManager)
                    getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(0, builder.build());
        }
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
}
