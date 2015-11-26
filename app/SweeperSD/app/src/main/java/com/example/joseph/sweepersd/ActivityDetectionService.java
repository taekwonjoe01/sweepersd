package com.example.joseph.sweepersd;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationServices;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class ActivityDetectionService extends IntentService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = ActivityDetectionService.class.getSimpleName();

    private int mConfidenceThreshold = 100; // TODO: make this adjustable

    private GoogleApiClient mGoogleApiClient;

    public ActivityDetectionService() {
        super("ActivityDetectionService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            if (ActivityRecognitionResult.hasResult(intent)) {
                ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
                DetectedActivity detectedActivity = result.getMostProbableActivity();

                handleActivity(detectedActivity);
            }
        }
    }

    private void handleActivity(DetectedActivity activity) {
        Log.d(TAG, "Activity detected: " + getActivityString(activity.getType()) + " conf:"
                + activity.getConfidence());
        if (activity.getConfidence() >= mConfidenceThreshold) {
            Log.d(TAG, "Activity confidence meets threshold.");
            if (activity.getType() == DetectedActivity.ON_FOOT &&
                    SweeperSDApplication.getmDetectedActivity().getType() ==
                            DetectedActivity.IN_VEHICLE) {
                handleParkDetected();
            }
            /*if (SweeperSDApplication.getmDetectedActivity() != null &&
                    activity.getType() != SweeperSDApplication.getmDetectedActivity().getType()) {
                Log.d(TAG, "handleParkDetected");
                handleParkDetected();
            }*/
            SweeperSDApplication.setDetectedActivity(activity);
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
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");
        SweeperSDApplication.setParkedLocation(LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient));
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
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed");
    }
}
