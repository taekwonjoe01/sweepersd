package com.example.joseph.sweepersd;

import android.app.Application;
import android.location.Location;

/**
 * Created by joseph on 11/25/15.
 */
public class SweeperSDApplication extends Application {
    private static boolean mIsDriving;

    public static synchronized void setIsDriving(boolean driving) {
        mIsDriving = driving;
    }

    public static boolean isDriving() {
        return mIsDriving;
    }

    private static int mInVehicleConfidence;
    private static int mOnFootConfidence;
    private static int mStillConfidence;
    private static int mUnknownConfidence;
    private static int mOnBicycleConfidence;
    private static int mWalkingConfidence;
    private static int mRunningConfidence;
    private static int mTiltingConfidence;
    public static synchronized void setVehicleConfidence(int confidence) {
        mInVehicleConfidence = confidence;
    }
    public static synchronized void setFootConfidence(int confidence) {
        mOnFootConfidence = confidence;
    }
    public static synchronized void setStillConfidence(int confidence) {
        mStillConfidence = confidence;
    }
    public static synchronized void setUnknownConfidence(int confidence) {
        mUnknownConfidence = confidence;
    }
    public static synchronized void setBicycleConfidence(int confidence) {
        mOnBicycleConfidence = confidence;
    }
    public static synchronized void setWalkingConfidence(int confidence) {
        mWalkingConfidence = confidence;
    }
    public static synchronized void setRunningConfidence(int confidence) {
        mRunningConfidence = confidence;
    }
    public static synchronized void setTiltingConfidence(int confidence) {
        mTiltingConfidence = confidence;
    }

    public static int getVehicleConfidence() {
        return mInVehicleConfidence;
    }
    public static int getFootConfidence() {
        return mOnFootConfidence;
    }
    public static int getStillConfidence() {
        return mStillConfidence;
    }
    public static int getUnknownConfidence() {
        return mUnknownConfidence;
    }
    public static int getBicycleConfidence() {
        return mOnBicycleConfidence;
    }
    public static int getWalkingConfidence() {
        return mWalkingConfidence;
    }
    public static int getRunningConfidence() {
        return mRunningConfidence;
    }
    public static int getTiltingConfidence() {
        return mTiltingConfidence;
    }

    private static boolean mIsParkingDetected = false;

    public static synchronized void setParkingDetected(boolean detected) {
        mIsParkingDetected = detected;
    }

    public static boolean isParkingDetected() {
        return mIsParkingDetected;
    }




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
