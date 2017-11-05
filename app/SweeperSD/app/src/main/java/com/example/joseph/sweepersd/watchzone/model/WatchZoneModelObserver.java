package com.example.joseph.sweepersd.watchzone.model;

public class WatchZoneModelObserver extends WatchZoneBaseObserver<WatchZone, ZoneModel> {
    private final Long mWatchZoneUid;
    private final WatchZoneModelChangedCallback mCallback;

    protected WatchZone mWatchZone;

    public interface WatchZoneModelChangedCallback extends WatchZoneBaseObserverCallback<WatchZone> {
        void onWatchZoneModelChanged(WatchZone watchZone);
    }

    public WatchZoneModelObserver(Long watchZoneUid, WatchZoneModelChangedCallback callback) {
        super(callback);
        mWatchZoneUid = watchZoneUid;
        mCallback = callback;
    }

    @Override
    boolean isValid(ZoneModel zoneModel) {
        return zoneModel != null;
    }

    @Override
    WatchZone getData(ZoneModel zoneModel) {
        if (mWatchZone == null) {
            mWatchZone = zoneModel.watchZone;
        }
        return zoneModel.watchZone;
    }

    @Override
    void onPossibleChangeDetected(WatchZone watchZone) {
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
