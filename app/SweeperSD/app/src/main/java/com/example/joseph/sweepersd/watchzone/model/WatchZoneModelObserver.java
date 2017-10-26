package com.example.joseph.sweepersd.watchzone.model;

import android.util.Log;

public class WatchZoneModelObserver extends WatchZoneBaseObserver<WatchZoneModel> {
    private final Long mWatchZoneUid;
    private final WatchZoneModelChangedCallback mCallback;

    protected WatchZoneModel mWatchZoneModel;

    public interface WatchZoneModelChangedCallback extends WatchZoneBaseObserverCallback<WatchZoneModel> {
        void onWatchZoneModelChanged(WatchZoneModel model);
    }

    public WatchZoneModelObserver(Long watchZoneUid, WatchZoneModelChangedCallback callback) {
        super(callback);
        mWatchZoneUid = watchZoneUid;
        mCallback = callback;
    }

    @Override
    boolean isValid(WatchZoneModelRepository watchZoneModelRepository) {
        return watchZoneModelRepository.watchZoneExists(mWatchZoneUid);
    }

    @Override
    WatchZoneModel getDataFromRepo(WatchZoneModelRepository watchZoneModelRepository) {
        WatchZoneModel model = watchZoneModelRepository.getWatchZoneModel(mWatchZoneUid);
        if (model != null) {
            if (model.getStatus() != WatchZoneModel.Status.LOADING) {
                if (mWatchZoneModel == null) {
                    mWatchZoneModel = model;
                }
                return model;
            }
        }
        return null;
    }

    @Override
    void onRepositoryChanged(final WatchZoneModel watchZoneModel) {
        mCallback.onWatchZoneModelChanged(mWatchZoneModel);
    }

    public WatchZoneModel getWatchZoneModel() {
        return mWatchZoneModel;
    }
}
