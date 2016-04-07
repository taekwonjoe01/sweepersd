package com.example.joseph.sweepersd;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joseph on 3/12/16.
 */
public class ParkDetectionManager implements GoogleApiClient.ConnectionCallbacks {
    private static final String TAG = ParkDetectionManager.class.getSimpleName();

    public static final String ACTIVITY_RECOGNITION_RESULT = "ACTIVITY_RECOGNITION_RESULT";
    public static final String ACTIVITY_RECOGNITION_CLEAR = "ACTIVITY_RECOGNITION_CLEAR";

    public static final String CONFIDENCE_VEHICLE = "CONFIDENCE_VEHICLE";
    public static final String CONFIDENCE_FOOT = "CONFIDENCE_FOOT";
    public static final String CONFIDENCE_BICYCLE = "CONFIDENCE_BICYCLE";
    public static final String CONFIDENCE_WALKING = "CONFIDENCE_WALKING";
    public static final String CONFIDENCE_RUNNING = "CONFIDENCE_RUNNING";
    public static final String CONFIDENCE_TILTING = "CONFIDENCE_TILTING";
    public static final String CONFIDENCE_STILL = "CONFIDENCE_STILL";
    public static final String CONFIDENCE_UNKNOWN = "CONFIDENCE_UNKNOWN";

    private volatile int mAverageVehicleConfidence = 0;
    private volatile int mAverageFootConfidence = 0;
    private volatile int mAverageBicycleConfidence = 0;
    private volatile int mAverageWalkingConfidence = 0;
    private volatile int mAverageStillConfidence = 0;
    private volatile int mAverageTiltingConfidence = 0;
    private volatile int mAverageRunningConfidence = 0;
    private volatile int mAverageUnknownConfidence = 0;

    private Context mContext;

    // This guy is our decision if we are parked or driving.

    public enum Status {
        PARKED,
        PARKED_DECIDING,
        DRIVING,
        DRIVING_DECIDING
    }
    private Status mLastStatus = Status.PARKED;
    private Status mStatus = Status.PARKED;

    // Use this to check if we've been asked to startParkDetection.
    private GoogleApiClient mApiClient = null;
    private ParkDetectionListener mListener;
    private PendingIntent mActivityRecognitionIntent;

    private List<ActivityRecognitionResult> mRecentActivityResults = new ArrayList<>();

    private ParkDetectionSettings mSettings = new ParkDetectionSettings();

    public ParkDetectionManager(Context context) {
        mContext = context;
    }

    public void startParkDetection(ParkDetectionListener listener, GoogleApiClient apiClient) {
        mListener = listener;
        mApiClient = apiClient;

        if (mApiClient.isConnected()) {
            resumeParkDetection();
        }
    }

    public void stopParkDetection() {
        pauseParkDetection();

        mListener = null;
        mApiClient = null;
    }

    public Status getStatus() {
        return mStatus;
    }

    public int getConfidence(String confidence) {
        int result = -1;
        switch (confidence) {
            case CONFIDENCE_BICYCLE:
                result = mAverageBicycleConfidence;
                break;
            case CONFIDENCE_FOOT:
                result = mAverageFootConfidence;
                break;
            case CONFIDENCE_RUNNING:
                result = mAverageRunningConfidence;
                break;
            case CONFIDENCE_WALKING:
                result = mAverageWalkingConfidence;
                break;
            case CONFIDENCE_STILL:
                result = mAverageStillConfidence;
                break;
            case CONFIDENCE_TILTING:
                result = mAverageTiltingConfidence;
                break;
            case CONFIDENCE_UNKNOWN:
                result = mAverageUnknownConfidence;
                break;
            case CONFIDENCE_VEHICLE:
                result = mAverageVehicleConfidence;
                break;
        }
        return result;
    }

    public ParkDetectionSettings getParkDetectionSettings() {
        return mSettings;
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (mApiClient != null) {
            resumeParkDetection();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (mApiClient != null) {
            pauseParkDetection();
        }
    }

    private void pauseParkDetection() {
        mContext.unregisterReceiver(mActivityDetectorReceiver);
        ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mApiClient,
                mActivityRecognitionIntent);
    }

    private void resumeParkDetection() {
        mSettings.loadSettings(PreferenceManager.getDefaultSharedPreferences(mContext));

        // Register IntentFilter for receiving activity updates from the ActivityDetection
        // IntentService.
        IntentFilter filter = new IntentFilter();
        filter.addAction(ActivityDetectionService.ACTION_ACTIVITY_UPDATE);
        mContext.registerReceiver(mActivityDetectorReceiver, filter);

        // Register the ActivityDetectionService for activity updates. That service will just
        // forward that data to the mActivityDetectorReceiver.
        Intent intent = new Intent(mContext, ActivityDetectionService.class);
        mActivityRecognitionIntent = PendingIntent.getService(mContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mApiClient,
                mSettings.ACTIVITY_UPDATE_PERIOD, mActivityRecognitionIntent);
    }

    private void handleActivityUpdate(ActivityRecognitionResult result) {
        mLastStatus = mStatus;
        mRecentActivityResults.add(0, result);

        pruneOldActivity(mRecentActivityResults, mSettings.AGE_OF_VALID_STATUS);

        int mostLikelyVehicle = 0;
        int mostLikelyNotVehicle = 0;

        int sumVehicleConfidence = 0;
        int sumFootConfidence = 0;
        int sumBicycleConfidence = 0;
        int sumWalkingConfidence = 0;
        int sumStillConfidence = 0;
        int sumTiltingConfidence = 0;
        int sumRunningConfidence = 0;
        int sumUnknownConfidence = 0;

        int totalWeightedActivities = mRecentActivityResults.size();

        if (totalWeightedActivities == 0) {
            return;
        }

        for (ActivityRecognitionResult r : mRecentActivityResults) {
            DetectedActivity detectedActivity = r.getMostProbableActivity();

            if (detectedActivity.getType() == DetectedActivity.IN_VEHICLE) {
                mostLikelyVehicle++;
            } else {
                mostLikelyNotVehicle++;
            }

            int vehicleConfidence = 0;
            int footConfidence = 0;
            int bicycleConfidence = 0;
            int walkingConfidence = 0;
            int stillConfidence = 0;
            int tiltingConfidence = 0;
            int runningConfidence = 0;
            int unknownConfidence = 0;
            for (DetectedActivity activity : r.getProbableActivities()) {
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

            sumVehicleConfidence += vehicleConfidence;
            sumFootConfidence += footConfidence;
            sumBicycleConfidence += bicycleConfidence;
            sumWalkingConfidence += walkingConfidence;
            sumStillConfidence += stillConfidence;
            sumTiltingConfidence += tiltingConfidence;
            sumRunningConfidence += runningConfidence;
            sumUnknownConfidence += unknownConfidence;
        }

        mAverageVehicleConfidence = sumVehicleConfidence / totalWeightedActivities;
        mAverageFootConfidence = sumFootConfidence / totalWeightedActivities;
        mAverageBicycleConfidence = sumBicycleConfidence / totalWeightedActivities;
        mAverageWalkingConfidence = sumWalkingConfidence / totalWeightedActivities;
        mAverageStillConfidence = sumStillConfidence / totalWeightedActivities;
        mAverageTiltingConfidence = sumTiltingConfidence / totalWeightedActivities;
        mAverageRunningConfidence = sumRunningConfidence / totalWeightedActivities;
        mAverageUnknownConfidence = sumUnknownConfidence / totalWeightedActivities;

        Log.d(TAG, "handle Activity Update. Total Activities: " + totalWeightedActivities);
        Log.d(TAG, "mAverageVehicleConfidence: " + mAverageVehicleConfidence);

        int drivingThreshold = getDrivingThreshold();
        int parkedThreshold = getParkedThreshold();

        if (mAverageVehicleConfidence >= drivingThreshold) {
            mStatus = Status.DRIVING;
        } else if (mAverageVehicleConfidence <= parkedThreshold) {
            mStatus = Status.PARKED;
        } else {
            mStatus = mStatus == Status.DRIVING ? Status.DRIVING_DECIDING :
                    mStatus == Status.PARKED ? Status.PARKED_DECIDING :
                    mStatus;
        }
    }

    private int getDrivingThreshold() {
        int result;
        switch (mStatus) {
            case DRIVING:
                result = mSettings.STATUS_DRIVING_DRIVING_THRESHOLD;
                break;
            case DRIVING_DECIDING:
                result = mSettings.STATUS_DRIVING_DECIDING_DRIVING_THRESHOLD;
                break;
            case PARKED:
                result = mSettings.STATUS_PARKED_DRIVING_THRESHOLD;
                break;
            case PARKED_DECIDING:
                result = mSettings.STATUS_PARKED_DECIDING_DRIVING_THRESHOLD;
                break;
            default:
                result = 0;
        }
        return result;
    }

    private int getParkedThreshold() {
        int result;
        switch (mStatus) {
            case DRIVING:
                result = mSettings.STATUS_DRIVING_PARKED_THRESHOLD;
                break;
            case DRIVING_DECIDING:
                result = mSettings.STATUS_DRIVING_DECIDING_PARKED_THRESHOLD;
                break;
            case PARKED:
                result = mSettings.STATUS_PARKED_PARKED_THRESHOLD;
                break;
            case PARKED_DECIDING:
                result = mSettings.STATUS_PARKED_DECIDING_PARKED_THRESHOLD;
                break;
            default:
                result = 0;
        }
        return result;
    }

    private void pruneOldActivity(List<ActivityRecognitionResult> timeOrderedResults, long age) {
        int indexToPrune = 0;
        long latestTime = System.currentTimeMillis();
        for (ActivityRecognitionResult result : timeOrderedResults) {
            if ((latestTime - result.getTime()) > age) {
                break;
            }
            indexToPrune++;
        }
        while (mRecentActivityResults.size() > indexToPrune) {
            mRecentActivityResults.remove(indexToPrune);
        }
    }

    private void notifyStatusUpdate() {
        if (mListener != null && mLastStatus != mStatus) {
            switch (mStatus) {
                case DRIVING:
                    if (mLastStatus != Status.DRIVING_DECIDING) {
                        mListener.onDriving();
                    }
                    break;
                case DRIVING_DECIDING:
                    mListener.onParkPossible();
                    break;
                case PARKED:
                    if (mLastStatus != Status.PARKED_DECIDING) {
                        mListener.onPark();
                    }
                    break;
                case PARKED_DECIDING:
                    break;
                default:
            }
        } else if (mListener != null && mStatus == Status.DRIVING_DECIDING) {
            mListener.onParkPossible();
        }
    }

    public interface ParkDetectionListener {
        void onPark();
        void onParkPossible();
        void onDriving();
    }

    public class ParkDetectionSettings {
        public int STATUS_DRIVING_DRIVING_THRESHOLD = 70;
        public int STATUS_DRIVING_PARKED_THRESHOLD = 20;

        public int STATUS_DRIVING_DECIDING_DRIVING_THRESHOLD = 70;
        public int STATUS_DRIVING_DECIDING_PARKED_THRESHOLD = 30;

        public int STATUS_PARKED_DRIVING_THRESHOLD = 100;
        public int STATUS_PARKED_PARKED_THRESHOLD = 40;

        public int STATUS_PARKED_DECIDING_DRIVING_THRESHOLD = 90;
        public int STATUS_PARKED_DECIDING_PARKED_THRESHOLD = 35;

        public int AGE_OF_VALID_STATUS = 40000;
        public int ACTIVITY_UPDATE_PERIOD = 5000;

        public void loadSettings(SharedPreferences preferences) {
            STATUS_DRIVING_DRIVING_THRESHOLD = preferences.getInt(SettingsActivity.PREF_KEY_DRIVING_DRIVING_THRESHOLD, 70);
            STATUS_DRIVING_PARKED_THRESHOLD = preferences.getInt(SettingsActivity.PREF_KEY_DRIVING_PARKED_THRESHOLD, 20);
            STATUS_DRIVING_DECIDING_DRIVING_THRESHOLD = preferences.getInt(SettingsActivity.PREF_KEY_DRIVING_DECIDING_DRIVING_THRESHOLD, 70);
            STATUS_DRIVING_DECIDING_PARKED_THRESHOLD = preferences.getInt(SettingsActivity.PREF_KEY_DRIVING_DECIDING_PARKED_THRESHOLD, 30);
            STATUS_PARKED_DRIVING_THRESHOLD = preferences.getInt(SettingsActivity.PREF_KEY_PARKED_DRIVING_THRESHOLD, 100);
            STATUS_PARKED_PARKED_THRESHOLD = preferences.getInt(SettingsActivity.PREF_KEY_PARKED_PARKED_THRESHOLD, 40);
            STATUS_PARKED_DECIDING_DRIVING_THRESHOLD = preferences.getInt(SettingsActivity.PREF_KEY_PARKED_DECIDING_DRIVING_THRESHOLD, 90);
            STATUS_PARKED_DECIDING_PARKED_THRESHOLD = preferences.getInt(SettingsActivity.PREF_KEY_PARKED_DECIDING_PARKED_THRESHOLD, 35);
            AGE_OF_VALID_STATUS = Integer.parseInt(preferences.getString(SettingsActivity.PREF_KEY_AGE_VALID, "40000"));
            ACTIVITY_UPDATE_PERIOD = Integer.parseInt(preferences.getString(SettingsActivity.PREF_KEY_ACTIVITY_UPDATE_RATE, "5000"));
        }
    }

    private final BroadcastReceiver mActivityDetectorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ActivityDetectionService.ACTION_ACTIVITY_UPDATE.equals(action)) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    ActivityRecognitionResult result =
                            extras.getParcelable(ACTIVITY_RECOGNITION_RESULT);
                    boolean clearExistingData = extras.getBoolean(ACTIVITY_RECOGNITION_CLEAR, false);
                    if (clearExistingData) {
                        mRecentActivityResults.clear();
                    }

                    Log.d(TAG, "Activity update");
                    handleActivityUpdate(result);

                    notifyStatusUpdate();
                }
            }
        }
    };
}
