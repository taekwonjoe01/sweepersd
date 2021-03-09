package com.example.joseph.sweepersd.experimental.parkdetection;

import androidx.lifecycle.MutableLiveData;
import android.content.Context;

/**
 * Created by josephhutchins on 10/10/17.
 */

public class ParkDetectionManager  {
    public static final String CONFIDENCE_VEHICLE = "CONFIDENCE_VEHICLE";
    public static final String CONFIDENCE_FOOT = "CONFIDENCE_FOOT";
    public static final String CONFIDENCE_BICYCLE = "CONFIDENCE_BICYCLE";
    public static final String CONFIDENCE_WALKING = "CONFIDENCE_WALKING";
    public static final String CONFIDENCE_RUNNING = "CONFIDENCE_RUNNING";
    public static final String CONFIDENCE_TILTING = "CONFIDENCE_TILTING";
    public static final String CONFIDENCE_STILL = "CONFIDENCE_STILL";
    public static final String CONFIDENCE_UNKNOWN = "CONFIDENCE_UNKNOWN";

    private static ParkDetectionManager sInstance;

    private final Context mApplicationContext;
    private final MutableLiveData<ParkingStatus> mParkingStatusLiveData;

    private boolean mParkDetectionEnabled;
    private boolean mParkDetectionRunning;

    private int mAverageVehicleConfidence = 0;
    private int mAverageFootConfidence = 0;
    private int mAverageBicycleConfidence = 0;
    private int mAverageWalkingConfidence = 0;
    private int mAverageStillConfidence = 0;
    private int mAverageTiltingConfidence = 0;
    private int mAverageRunningConfidence = 0;
    private int mAverageUnknownConfidence = 0;

    public enum ParkingStatus {
        PARKED,
        PARKED_DECIDING,
        DRIVING,
        DRIVING_DECIDING,
        UNKNOWN,
        PARK_DETECTION_DISABLED
    }

    private ParkDetectionManager(Context context) {
        mApplicationContext = context.getApplicationContext();
        mParkingStatusLiveData = new MutableLiveData<>();
        mParkingStatusLiveData.setValue(ParkingStatus.UNKNOWN);
    }

    public static synchronized ParkDetectionManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ParkDetectionManager(context);
        }
        return sInstance;
    }

    public synchronized void delete() {
        if (sInstance != null) {
            sInstance = null;
        }
    }
    
    public synchronized void setParkDetectionEnabled(boolean enabled) {
        if (enabled && ! mParkDetectionEnabled) {
            // TODO write SharedPreferences

            // TODO set mParkDetectionEnabled

            // TODO start Park Detection
        } else if (!enabled && mParkDetectionEnabled) {
            // TODO write SharedPreferences

            // TODO set mParkDetectionEnabled

            // TODO stop Park Detection
        }
    }
    
    public synchronized int getActivityConfidence(String activity) {
        int result = -1;
        switch (activity) {
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
    
    
    synchronized void handleActivityUpdate() {
        
    }

    synchronized void handleBootCompleted() {

    }
}
