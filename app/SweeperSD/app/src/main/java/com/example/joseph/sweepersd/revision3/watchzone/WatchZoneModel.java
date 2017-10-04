package com.example.joseph.sweepersd.revision3.watchzone;

import java.util.List;


public class WatchZoneModel {
    private final WatchZone mWatchZone;
    private final List<WatchZonePoint> mWatchZonePoints;
    private final WatchZoneStatus mStatus;

    public enum WatchZoneStatus {
        INCOMPLETE,
        OUT_OF_DATE,
        VALID
    }

    WatchZoneModel(WatchZone watchZone, List<WatchZonePoint> watchZonePoints,
                   WatchZoneStatus status) {
        mWatchZone = watchZone;
        mWatchZonePoints = watchZonePoints;
        mStatus = status;
    }

    public WatchZone getWatchZone() {
        return mWatchZone;
    }

    public List<WatchZonePoint> getWatchZonePoints() {
        return mWatchZonePoints;
    }

    public WatchZoneStatus getStatus() {
        return mStatus;
    }

    public boolean areSame(WatchZoneModel compareTo) {
        boolean result = true;
        if (!this.mWatchZone.areSame(compareTo.getWatchZone())) {
            result = false;
        } else if (this.mStatus != compareTo.getStatus()) {
            result = false;
        } else if (this.mWatchZonePoints.size() != compareTo.getWatchZonePoints().size()){
            result = false;
        } else {
            int index = 0;
            List<WatchZonePoint> otherPoints = compareTo.getWatchZonePoints();
            for (WatchZonePoint p : mWatchZonePoints) {
                WatchZonePoint otherP = otherPoints.get(index);
                if (otherP == null) {
                    result = false;
                    break;
                } else if (!p.areSame(otherP)) {
                    result = false;
                }
                index++;
            }
        }

        return result;
    }
}
