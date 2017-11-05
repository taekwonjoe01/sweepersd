package com.example.joseph.sweepersd.watchzone.model;

import com.example.joseph.sweepersd.utils.BaseObserver;

public class WatchZoneModelObserver extends BaseObserver<WatchZone, ZoneModel> {
    private final Long mWatchZoneUid;
    private final WatchZoneModelChangedCallback mCallback;

    protected WatchZone mWatchZone;

    public interface WatchZoneModelChangedCallback extends BaseObserverCallback<WatchZone> {
        void onWatchZoneModelChanged(WatchZone watchZone);
    }

    public WatchZoneModelObserver(Long watchZoneUid, WatchZoneModelChangedCallback callback) {
        super(callback);
        mWatchZoneUid = watchZoneUid;
        mCallback = callback;
    }

    @Override
    public boolean isValid(ZoneModel zoneModel) {
        return zoneModel != null;
    }

    @Override
    public WatchZone getData(ZoneModel zoneModel) {
        if (mWatchZone == null) {
            mWatchZone = zoneModel.watchZone;
        }
        return zoneModel.watchZone;
    }

    @Override
    public void onPossibleChangeDetected(WatchZone watchZone) {
        boolean isChanged = mWatchZone.isChanged(watchZone);
        mWatchZone = watchZone;
        if (isChanged) {
            mCallback.onWatchZoneModelChanged(mWatchZone);
        }
    }

    public WatchZone getWatchZone() {
        return mWatchZone;
    }
}
