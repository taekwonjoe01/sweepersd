package com.example.joseph.sweepersd;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.location.ActivityRecognitionResult;


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
        /*List<DetectedActivity> activities = result.getProbableActivities();
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

        Bundle bundle = new Bundle();
        bundle.putInt(SweeperService.CONFIDENCE_BICYCLE, bicycleConfidence);
        bundle.putInt(SweeperService.CONFIDENCE_VEHICLE, vehicleConfidence);
        bundle.putInt(SweeperService.CONFIDENCE_WALKING, walkingConfidence);
        bundle.putInt(SweeperService.CONFIDENCE_FOOT, footConfidence);
        bundle.putInt(SweeperService.CONFIDENCE_RUNNING, runningConfidence);
        bundle.putInt(SweeperService.CONFIDENCE_STILL, stillConfidence);
        bundle.putInt(SweeperService.CONFIDENCE_UNKNOWN, unknownConfidence);
        bundle.putInt(SweeperService.CONFIDENCE_TILTING, tiltingConfidence);
        bundle.putInt(SweeperService.MOST_LIKELY_ACTIVITY,
                result.getMostProbableActivity().getType());


        Intent intent = new Intent(ACTION_ACTIVITY_UPDATE);
        intent.putExtras(bundle);
        sendBroadcast(intent);*/
        Bundle bundle = new Bundle();
        bundle.putParcelable(ParkDetectionManager.ACTIVITY_RECOGNITION_RESULT, result);

        Intent intent = new Intent(ACTION_ACTIVITY_UPDATE);
        intent.putExtras(bundle);
        sendBroadcast(intent);
    }
}
