package com.example.joseph.sweepersd.alarms;

import com.example.joseph.sweepersd.SweepingPosition;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * Data model for an Alarm.
 */
public class Alarm {
    private final long mTimestamp;
    private final LatLng mCenter;
    private final int mRadius;
    private List<SweepingPosition> mSweepingPositions;

    public Alarm(long timestamp, LatLng center, int radius,
                 List<SweepingPosition> sweepingPositions) {
        mTimestamp = timestamp;
        mCenter = center;
        mRadius = radius;
        mSweepingPositions = sweepingPositions;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public LatLng getCenter() {
        return mCenter;
    }

    public int getRadius() {
        return mRadius;
    }

    public List<SweepingPosition> getSweepingPositions() {
        return mSweepingPositions;
    }

    public void setSweepingPositions(List<SweepingPosition> sweepingPositions) {
        mSweepingPositions = sweepingPositions;
    }
}
