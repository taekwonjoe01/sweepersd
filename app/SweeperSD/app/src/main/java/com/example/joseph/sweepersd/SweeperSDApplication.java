package com.example.joseph.sweepersd;

import android.app.Application;
import android.location.Location;

/**
 * Created by joseph on 11/25/15.
 */
public class SweeperSDApplication extends Application {
    private static Location mParkedLocation;
    private static long mParkedTimestamp = 0L;
    private static long mDrivingTimestamp = 0L;

    public static Location getParkedLocation() {
        return mParkedLocation;
    }

    public static Long getParkedTimestamp() {
        return mParkedTimestamp;
    }

    public static synchronized void setParkedLocation(Location location, long timestamp) {
        mParkedLocation = location;
        mParkedTimestamp = timestamp;
    }

    public static synchronized void setDrivingTimestamp(long timestamp) {
        mDrivingTimestamp = timestamp;
    }

    public static long getDrivingTimestamp() {
        return mDrivingTimestamp;
    }
}
