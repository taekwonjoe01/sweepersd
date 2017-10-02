package com.example.joseph.sweepersd.revision3.watchzone;

import java.util.List;

/**
 * Created by joseph on 10/1/17.
 */

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
}
