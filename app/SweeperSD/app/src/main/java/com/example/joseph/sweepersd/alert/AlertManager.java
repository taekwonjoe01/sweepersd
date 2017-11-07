package com.example.joseph.sweepersd.alert;


import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import com.example.joseph.sweepersd.alert.geofence.WatchZoneFence;
import com.example.joseph.sweepersd.alert.geofence.WatchZoneFenceObserver;
import com.example.joseph.sweepersd.alert.geofence.WatchZoneFenceRepository;
import com.example.joseph.sweepersd.utils.BaseObserver;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelsObserver;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AlertManager extends LiveData<Boolean> {
    private static AlertManager sInstance;

    private final Context mApplicationContext;
    private final HandlerThread mThread;
    private final Handler mHandler;
    private final AtomicInteger mTaskCount;

    private final WatchZoneModelsObserver mWatchZoneModelsObserver = new WatchZoneModelsObserver(
            true, new WatchZoneModelsObserver.WatchZoneModelsChangedCallback() {
        @Override
        public void onModelsChanged(Map<Long, WatchZoneModel> data, BaseObserver.ChangeSet changeSet) {
            if (mFenceObserver.isLoaded()) {
                mTaskCount.incrementAndGet();
                postValue(true);
                mHandler.post(new UpdateAlertsTask(data, mFenceObserver.getWatchZoneFences()));
            }
        }

        @Override
        public void onDataLoaded(Map<Long, WatchZoneModel> data) {
            if (mFenceObserver.isLoaded()) {
                mTaskCount.incrementAndGet();
                postValue(true);
                mHandler.post(new UpdateAlertsTask(data, mFenceObserver.getWatchZoneFences()));
            }
        }

        @Override
        public void onDataInvalid() {
            // This should never happen!
        }
    });

    private final WatchZoneFenceObserver mFenceObserver = new WatchZoneFenceObserver(
            new WatchZoneFenceObserver.WatchZoneFencesChangedCallback() {
        @Override
        public void onFencesChanges(Map<Long, WatchZoneFence> data, BaseObserver.ChangeSet changeSet) {
            if (mWatchZoneModelsObserver.isLoaded()) {
                mTaskCount.incrementAndGet();
                postValue(true);
                mHandler.post(new UpdateAlertsTask(mWatchZoneModelsObserver.getWatchZoneModels(), data));
            }
        }

        @Override
        public void onDataLoaded(Map<Long, WatchZoneFence> data) {
            if (mWatchZoneModelsObserver.isLoaded()) {
                mTaskCount.incrementAndGet();
                postValue(true);
                mHandler.post(new UpdateAlertsTask(mWatchZoneModelsObserver.getWatchZoneModels(), data));
            }
        }

        @Override
        public void onDataInvalid() {
            // This should never happen!
        }
    });

    private AlertManager(Context context) {
        mApplicationContext = context.getApplicationContext();
        mThread = new HandlerThread("AlertManager-thread");
        mThread.start();
        mHandler = new Handler(mThread.getLooper());
        mTaskCount = new AtomicInteger(0);
        postValue(false);
    }

    public static AlertManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AlertManager(context);
        }
        return sInstance;
    }

    @Override
    protected void onActive() {
        super.onActive();
        WatchZoneModelRepository.getInstance(mApplicationContext).getZoneModelsLiveData().observeForever(mWatchZoneModelsObserver);
        WatchZoneFenceRepository.getInstance(mApplicationContext).getFencesLiveData().observeForever(mFenceObserver);
        setValue(true);
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        WatchZoneModelRepository.getInstance(mApplicationContext).getZoneModelsLiveData().removeObserver(mWatchZoneModelsObserver);
        WatchZoneFenceRepository.getInstance(mApplicationContext).getFencesLiveData().removeObserver(mFenceObserver);
        mHandler.removeCallbacksAndMessages(null);
    }

    private class UpdateAlertsTask implements Runnable {
        private final Map<Long, WatchZoneModel> mModels;
        private final Map<Long, WatchZoneFence> mFences;

        UpdateAlertsTask(Map<Long, WatchZoneModel> models, Map<Long, WatchZoneFence> fences) {
            mModels = models;
            mFences = fences;
        }

        @Override
        public void run() {
            AlertUpdater alertUpdater = new AlertUpdater(mApplicationContext);
            alertUpdater.updateAlertNotification(new ArrayList<>(mModels.values()),
                    new ArrayList<>(mFences.values()));

            int numTasksRemaining = mTaskCount.decrementAndGet();
            if (numTasksRemaining == 0) {
                postValue(false);
            }
        }
    }
}
