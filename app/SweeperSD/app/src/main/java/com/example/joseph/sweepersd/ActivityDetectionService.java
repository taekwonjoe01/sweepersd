package com.example.joseph.sweepersd;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class ActivityDetectionService extends IntentService {
    private static final String TAG = ActivityDetectionService.class.getSimpleName();
    public static final String ACTION_ACTIVITY_UPDATE =
            "com.example.joseph.sweepersd.ACTION_ACTIVITY_UPDATE";
    public static final String CONFIDENCE_VEHICLE = "CONFIDENCE_VEHICLE";
    public static final String CONFIDENCE_FOOT = "CONFIDENCE_FOOT";
    public static final String CONFIDENCE_BICYCLE = "CONFIDENCE_BICYCLE";
    public static final String CONFIDENCE_WALKING = "CONFIDENCE_WALKING";
    public static final String CONFIDENCE_RUNNING = "CONFIDENCE_RUNNING";
    public static final String CONFIDENCE_TILTING = "CONFIDENCE_TILTING";
    public static final String CONFIDENCE_STILL = "CONFIDENCE_STILL";
    public static final String CONFIDENCE_UNKNOWN = "CONFIDENCE_UNKNOWN";

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

        Bundle bundle = new Bundle();
        bundle.putInt(CONFIDENCE_BICYCLE, bicycleConfidence);
        bundle.putInt(CONFIDENCE_VEHICLE, vehicleConfidence);
        bundle.putInt(CONFIDENCE_WALKING, walkingConfidence);
        bundle.putInt(CONFIDENCE_FOOT, footConfidence);
        bundle.putInt(CONFIDENCE_RUNNING, runningConfidence);
        bundle.putInt(CONFIDENCE_STILL, stillConfidence);
        bundle.putInt(CONFIDENCE_UNKNOWN, unknownConfidence);
        bundle.putInt(CONFIDENCE_TILTING, tiltingConfidence);

        Intent intent = new Intent(ACTION_ACTIVITY_UPDATE);
        intent.putExtras(bundle);
        sendBroadcast(intent);


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

    private void handleParkDetected() {
        SweeperSDApplication.setParkingDetected(true);
    }
}
