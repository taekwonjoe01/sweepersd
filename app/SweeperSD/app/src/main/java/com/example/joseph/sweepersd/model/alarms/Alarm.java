package com.example.joseph.sweepersd.model.alarms;

import com.example.joseph.sweepersd.model.limits.Limit;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
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

    // TODO make package protected.
    public Alarm(long createdTimestamp, long lastUpdatedTimestamp,  LatLng center, int radius,
                 List<SweepingAddress> sweepingAddresses) {
        mCreatedTimestamp = createdTimestamp;
        mLastUpdatedTimestamp = lastUpdatedTimestamp;
        mCenter = center;
        mRadius = radius;
        setSweepingAddresses(sweepingAddresses);
    }
    // TODO make package protected.
    public Alarm(Alarm other) {
        this.mCreatedTimestamp = other.mCreatedTimestamp;
        this.mLastUpdatedTimestamp = other.mLastUpdatedTimestamp;
        this.mCenter = other.mCenter;
        this.mRadius = other.mRadius;
        List<SweepingAddress> addresses = new ArrayList<>();
        for (SweepingAddress address : other.getSweepingAddresses()) {
            addresses.add(new SweepingAddress(address));
        }
        setSweepingAddresses(addresses);
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

    public List<SweepingAddress> getSweepingAddresses() {
        return mSweepingAddresses;
    }

    public void setSweepingAddresses(List<SweepingAddress> sweepingAddresses) {
        if (sweepingAddresses == null) {
            mSweepingAddresses = new ArrayList<>();
        } else {
            mSweepingAddresses = sweepingAddresses;
        }

        mUniqueLimits = new HashMap<>();
        for (SweepingAddress address : mSweepingAddresses) {
            Limit l = address.getLimit();
            mUniqueLimits.put(l.getId(), l);
        }
    }
}
