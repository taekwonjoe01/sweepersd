package com.example.joseph.sweepersd.alert.geofence;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import com.example.joseph.sweepersd.utils.BaseObserver;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelsObserver;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class WatchZoneFenceManager extends LiveData<Boolean> {
    private static WatchZoneFenceManager sInstance;

    private final Context mApplicationContext;
    private final HandlerThread mThread;
    private final Handler mHandler;
    private final AtomicInteger mTaskCount;

    private final WatchZoneModelsObserver mWatchZoneModelsObserver = new WatchZoneModelsObserver(
            false, new WatchZoneModelsObserver.WatchZoneModelsChangedCallback() {
        @Override
        public void onModelsChanged(Map<Long, WatchZoneModel> data, BaseObserver.ChangeSet changeSet) {
            mTaskCount.incrementAndGet();
            postValue(true);
            mHandler.post(new UpdateGeofenceTask(data));
        }

        @Override
        public void onDataLoaded(Map<Long, WatchZoneModel> data) {
            mTaskCount.incrementAndGet();
            postValue(true);
            mHandler.post(new UpdateGeofenceTask(data));
        }

        @Override
        public void onDataInvalid() {
            // This should never happen!
        }
    });

    private WatchZoneFenceManager(Context context) {
        mApplicationContext = context.getApplicationContext();
        mThread = new HandlerThread("WatchZoneFenceManager-thread");
        mThread.start();
        mHandler = new Handler(mThread.getLooper());
        mTaskCount = new AtomicInteger(0);
        postValue(false);
    }

    public static WatchZoneFenceManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new WatchZoneFenceManager(context);
        }
        return sInstance;
    }

    @Override
    protected void onActive() {
        super.onActive();
        WatchZoneModelRepository.getInstance(mApplicationContext).getZoneModelsLiveData().observeForever(mWatchZoneModelsObserver);
        postValue(true);
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        WatchZoneModelRepository.getInstance(mApplicationContext).getZoneModelsLiveData().removeObserver(mWatchZoneModelsObserver);
    }

    private class UpdateGeofenceTask implements Runnable {
        private final Map<Long, WatchZoneModel> mModels;

        UpdateGeofenceTask(Map<Long, WatchZoneModel> models) {
            mModels = models;
        }

        @Override
        public void run() {
            WatchZoneFenceUpdater watchZoneFenceUpdater = new WatchZoneFenceUpdater(mApplicationContext);
            watchZoneFenceUpdater.updateGeofences(new ArrayList<>(mModels.values()));

            int numTasksRemaining = mTaskCount.decrementAndGet();
            if (numTasksRemaining == 0) {
                postValue(false);
            }
        }
    }
}
