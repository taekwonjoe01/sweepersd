package com.example.joseph.sweepersd.model.watchzone;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Data model for an WatchZone.
 */
public class WatchZone {
    private final long mCreatedTimestamp;
    private final long mLastUpdatedTimestamp;
    private final String mLabel;
    private final LatLng mCenter;
    private final int mRadius;

    private List<SweepingAddress> mSweepingAddresses;

    // TODO make package protected.
    public WatchZone(long createdTimestamp, long lastUpdatedTimestamp, String label, LatLng center,
                     int radius, List<SweepingAddress> sweepingAddresses) {
        mCreatedTimestamp = createdTimestamp;
        mLastUpdatedTimestamp = lastUpdatedTimestamp;
        mCenter = center;
        mLabel = label;
        mRadius = radius;
        setSweepingAddresses(sweepingAddresses);
    }
    // TODO make package protected.
    public WatchZone(WatchZone other) {
        this.mCreatedTimestamp = other.mCreatedTimestamp;
        this.mLastUpdatedTimestamp = other.mLastUpdatedTimestamp;
        this.mCenter = other.mCenter;
        this.mLabel = other.mLabel;
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

    public String getLabel() {
        return mLabel;
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
    }
}
