package com.example.joseph.sweepersd.revision3.watchzone;

import android.arch.lifecycle.LiveData;

public class WatchZoneUpdateProgressLiveData extends LiveData<WatchZoneUpdater.UpdateProgress> {
    private final WatchZone mWatchZone;
    private final WatchZoneUpdater mWatchZoneUpdater;

    public WatchZoneUpdateProgressLiveData(WatchZone watchZone, WatchZoneUpdater watchZoneUpdater) {
        mWatchZone = watchZone;
        mWatchZoneUpdater = watchZoneUpdater;
    }

    private final WatchZoneUpdater.ProgressListener mProgressListener = new WatchZoneUpdater.ProgressListener() {
        @Override
        public void onProgress(WatchZoneUpdater.UpdateProgress progress) {
            setValue(progress);
        }
    };

    @Override
    protected void onActive() {
        mWatchZoneUpdater.registerProgressUpdates(mWatchZone, mProgressListener);
    }

    @Override
    protected void onInactive() {
        mWatchZoneUpdater.unregisterProgressUpdates(mWatchZone);
    }
}
