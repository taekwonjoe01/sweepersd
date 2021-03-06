package com.example.joseph.sweepersd.scheduling;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.annotation.Nullable;

import com.example.joseph.sweepersd.utils.ChangeSet;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelsObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ScheduleManager extends LiveData<Boolean> {
    private static final String TAG = ScheduleManager.class.getSimpleName();
    private static ScheduleManager sInstance;

    private final Context mApplicationContext;
    private final HandlerThread mThread;
    private final Handler mHandler;
    private final AtomicInteger mTaskCount;

    private LiveData<List<WatchZoneModel>> mModelLiveData;
    private WatchZoneModelsObserver mWatchZoneModelsObserver;
    private Observer<Long> mLastAlarmObserver;

    private ScheduleManager(Context context) {
        mApplicationContext = context.getApplicationContext();
        mThread = new HandlerThread("ScheduleManager-thread");
        mThread.start();
        mHandler = new Handler(mThread.getLooper());
        mTaskCount = new AtomicInteger(0);
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
        mWatchZoneModelsObserver = new WatchZoneModelsObserver(
                true, new WatchZoneModelsObserver.WatchZoneModelsChangedCallback() {
            @Override
            public void onModelsChanged(Map<Long, WatchZoneModel> data, ChangeSet changeSet) {
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
        mLastAlarmObserver = new Observer<Long>() {
            @Override
            public void onChanged(@Nullable Long aLong) {
                if (mWatchZoneModelsObserver.isLoaded()) {
                    mTaskCount.incrementAndGet();
                    postValue(true);
                    mHandler.post(new UpdateScheduleTask(mWatchZoneModelsObserver.getWatchZoneModels()));
                }
            }
        };
        mModelLiveData = WatchZoneModelRepository.getInstance(mApplicationContext).getWatchZoneModelsLiveData();
        mModelLiveData.observeForever(mWatchZoneModelsObserver);
        LastAlarm.getInstance().observeForever(mLastAlarmObserver);
        setValue(true);
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        mModelLiveData.removeObserver(mWatchZoneModelsObserver);
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
