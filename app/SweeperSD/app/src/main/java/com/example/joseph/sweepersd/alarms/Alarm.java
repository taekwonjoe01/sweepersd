package com.example.joseph.sweepersd.alarms;

import com.example.joseph.sweepersd.SweepingAddress;
import com.example.joseph.sweepersd.limits.Limit;
import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;
import java.util.List;

/**
 * Data model for an Alarm.
 */
public class Alarm {
    private final long mCreatedTimestamp;
    private final long mLastUpdatedTimestamp;
    private final LatLng mCenter;
    private final int mRadius;

    private HashMap<Integer, Limit> mUniqueLimits;
    private List<SweepingAddress> mSweepingAddresses;

    public Alarm(long createdTimestamp, long lastUpdatedTimestamp,  LatLng center, int radius,
                 List<SweepingAddress> sweepingAddresses) {
        mCreatedTimestamp = createdTimestamp;
        mLastUpdatedTimestamp = lastUpdatedTimestamp;
        mCenter = center;
        mRadius = radius;
        setSweepingAddresses(sweepingAddresses);
    }

    public long getCreatedTimestamp() {
        return mCreatedTimestamp;
    }

    public long getLastUpdatedTimestamp() {
        return mLastUpdatedTimestamp;
    }

    public LatLng getCenter() {
        return mCenter;
    }

    public int getRadius() {
        return mRadius;
    }

    public List<SweepingAddress> getSweepingPositions() {
        return mSweepingAddresses;
    }

    public void setSweepingAddresses(List<SweepingAddress> sweepingAddresses) {
        mSweepingAddresses = sweepingAddresses;

        mUniqueLimits = new HashMap<>();
        for (SweepingAddress address : mSweepingAddresses) {
            Limit l = address.getLimit();
            mUniqueLimits.put(l.getId(), l);
        }
    }
}
