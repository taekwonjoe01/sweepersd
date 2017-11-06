package com.example.joseph.sweepersd.watchzone.model;

import com.example.joseph.sweepersd.utils.BaseObserver;

public class WatchZoneModelObserver extends BaseObserver<WatchZone, WatchZoneModel> {
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
    public boolean isValid(WatchZoneModel watchZoneModel) {
        return watchZoneModel != null;
    }

    @Override
    public WatchZone getData(WatchZoneModel watchZoneModel) {
        if (mWatchZone == null) {
            mWatchZone = watchZoneModel.watchZone;
        }
        return watchZoneModel.watchZone;
    }

    @Override
    public void onPossibleChangeDetected(WatchZone watchZone) {
        boolean isChanged = mWatchZone.isChanged(false, watchZone);
        mWatchZone = watchZone;
        if (isChanged) {
            mCallback.onWatchZoneModelChanged(mWatchZone);
        }
    }

    public WatchZone getWatchZone() {
        return mWatchZone;
    }
}
