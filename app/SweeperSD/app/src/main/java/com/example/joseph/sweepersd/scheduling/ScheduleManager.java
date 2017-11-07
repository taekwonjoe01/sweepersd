package com.example.joseph.sweepersd.scheduling;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;

import com.example.joseph.sweepersd.utils.BaseObserver;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelsObserver;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ScheduleManager extends LiveData<Boolean> {
    private static ScheduleManager sInstance;

    private final Context mApplicationContext;
    private final HandlerThread mThread;
    private final Handler mHandler;
    private final AtomicInteger mTaskCount;

    private final WatchZoneModelsObserver mWatchZoneModelsObserver = new WatchZoneModelsObserver(
            true, new WatchZoneModelsObserver.WatchZoneModelsChangedCallback() {
        @Override
        public void onModelsChanged(Map<Long, WatchZoneModel> data, BaseObserver.ChangeSet changeSet) {
            mTaskCount.incrementAndGet();
            postValue(true);
            mHandler.post(new UpdateScheduleTask(data));
        }

        @Override
        public void onDataLoaded(Map<Long, WatchZoneModel> data) {
            mTaskCount.incrementAndGet();
            postValue(true);
            mHandler.post(new UpdateScheduleTask(data));
        }

        @Override
        public void onDataInvalid() {
            // This should never happen!
        }
    });

    private final Observer<Long> mLastAlarmObserver = new Observer<Long>() {
        @Override
        public void onChanged(@Nullable Long aLong) {
            if (mWatchZoneModelsObserver.isLoaded()) {
                mTaskCount.incrementAndGet();
                postValue(true);
                mHandler.post(new UpdateScheduleTask(mWatchZoneModelsObserver.getWatchZoneModels()));
            }
        }
    };

    private ScheduleManager(Context context) {
        mApplicationContext = context.getApplicationContext();
        mThread = new HandlerThread("ScheduleManager-thread");
        mThread.start();
        mHandler = new Handler(mThread.getLooper());
        mTaskCount = new AtomicInteger(0);
        postValue(false);
    }

    public static ScheduleManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ScheduleManager(context);
        }
        return sInstance;
    }

    @Override
    protected void onActive() {
        super.onActive();
        WatchZoneModelRepository.getInstance(mApplicationContext).getZoneModelsLiveData().observeForever(mWatchZoneModelsObserver);
        LastAlarm.getInstance().observeForever(mLastAlarmObserver);
        setValue(true);
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        WatchZoneModelRepository.getInstance(mApplicationContext).getZoneModelsLiveData().removeObserver(mWatchZoneModelsObserver);
        LastAlarm.getInstance().removeObserver(mLastAlarmObserver);
        mHandler.removeCallbacksAndMessages(null);
    }

    private class UpdateScheduleTask implements Runnable {
        private final Map<Long, WatchZoneModel> mModels;

        UpdateScheduleTask(Map<Long, WatchZoneModel> models) {
            mModels = models;
        }

        @Override
        public void run() {
            ScheduleUpdater scheduleUpdater = new ScheduleUpdater(mApplicationContext);
            scheduleUpdater.scheduleWatchZones(new ArrayList<>(mModels.values()));

            int numTasksRemaining = mTaskCount.decrementAndGet();
            if (numTasksRemaining == 0) {
                postValue(false);
            }
        }
    }
}
