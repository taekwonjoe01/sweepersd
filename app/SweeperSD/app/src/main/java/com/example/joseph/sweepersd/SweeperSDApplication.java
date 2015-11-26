package com.example.joseph.sweepersd;

import android.app.Application;
import android.location.Location;

import com.google.android.gms.location.DetectedActivity;

/**
 * Created by joseph on 11/25/15.
 */
public class SweeperSDApplication extends Application {
    private static Location mParkedLocation;
    private static DetectedActivity mDetectedActivity;

    public static Location getParkedLocation() {
        return mParkedLocation;
    }

    public static synchronized void setParkedLocation(Location location) {
        mParkedLocation = location;
    }

    public static DetectedActivity getmDetectedActivity() {
        return mDetectedActivity;
    }

    public static synchronized void setDetectedActivity(DetectedActivity activity) {
        mDetectedActivity = activity;
    }
}
