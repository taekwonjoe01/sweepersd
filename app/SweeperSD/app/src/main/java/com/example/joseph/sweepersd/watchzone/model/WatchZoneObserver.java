package com.example.joseph.sweepersd.watchzone.model;

public class WatchZoneObserver extends WatchZoneBaseObserver<WatchZone> {
    final Long mWatchZoneUid;
    private final WatchZoneChangedCallback mCallback;

    protected WatchZone mWatchZone;

    public interface WatchZoneChangedCallback extends WatchZoneBaseObserverCallback<WatchZone> {
        void onWatchZoneChanged(WatchZone watchZone);
    }

    public WatchZoneObserver(Long watchZoneUid, WatchZoneChangedCallback callback) {
        super(callback);
        mWatchZoneUid = watchZoneUid;
        mCallback = callback;
    }

    @Override
    boolean isValid(WatchZoneModelRepository watchZoneModelRepository) {
        return watchZoneModelRepository.watchZoneExists(mWatchZoneUid);
    }

    @Override
    WatchZone getDataFromRepo(WatchZoneModelRepository watchZoneModelRepository) {
        WatchZoneModel model = watchZoneModelRepository.getWatchZoneModel(mWatchZoneUid);
        if (model != null) {
            WatchZone watchZone = model.getWatchZone();
            if (watchZone != null) {
                if (mWatchZone == null) {
                    mWatchZone = watchZone;
                }
                return watchZone;
            }
        }
        return null;
    }

    @Override
    void onRepositoryChanged(WatchZone watchZone) {
        if (mWatchZone.isChanged(watchZone)) {
            mWatchZone = watchZone;
            mCallback.onWatchZoneChanged(mWatchZone);
        }
    }

    public WatchZone getWatchZone() {
        return mWatchZone;
    }
}
