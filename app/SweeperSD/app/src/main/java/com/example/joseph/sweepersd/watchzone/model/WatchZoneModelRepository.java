package com.example.joseph.sweepersd.watchzone.model;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.joseph.sweepersd.scheduling.ScheduleJob;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WatchZoneModelRepository extends LiveData<WatchZoneModelRepository> {
    private static final String TAG = WatchZoneModelRepository.class.getSimpleName();

    private static MutableLiveData<WatchZoneModelRepository> sInstance = new MutableLiveData<>();

    private final Context mApplicationContext;
    private final Handler mHandler;
    private final HandlerThread mThread;

    private final Map<Long, WatchZoneModel> mWatchZoneModelsMap;

    private final Observer<List<WatchZone>> mWatchZoneObserver = new Observer<List<WatchZone>>() {
        @Override
        public void onChanged(@Nullable final List<WatchZone> watchZones) {
            // In this observer, we are only detecting insertions or deletions. Changes to the
            // models themselves are handled in the WatchZoneModels.
            Log.e("Joey", "onChanged");
            List<Long> existingWatchZones = WatchZoneRepository.getInstance(mApplicationContext).getWatchZoneUids();
            Set<Long> deletedWatchZones = new HashSet<>(mWatchZoneModelsMap.keySet());
            for (Long uid : existingWatchZones) {
                if (!mWatchZoneModelsMap.containsKey(uid)) {
                    mWatchZoneModelsMap.put(uid, null);
                }
                deletedWatchZones.remove(uid);
            }
            for (Long deletedUid : deletedWatchZones) {
                WatchZoneModel deletedModel = mWatchZoneModelsMap.remove(deletedUid);
                Log.e("Joey", "Deleting watch zone " + deletedUid);
                if (deletedModel != null) {
                    deletedModel.removeObserver(mWatchZoneModelObserver);
                }
            }
            if (watchZones != null) {
                for (WatchZone watchZone : watchZones) {
                    WatchZoneModel model = mWatchZoneModelsMap.get(watchZone.getUid());
                    if (model == null) {
                        model = new WatchZoneModel(mApplicationContext, mHandler, watchZone.getUid());
                        model.observeForever(mWatchZoneModelObserver);
                        mWatchZoneModelsMap.put(watchZone.getUid(), model);
                    }
                }
            }

            // This will spam call to start a scheduleJob. It should only run if 30 seconds pass
            // since the last call to schedule the scheduleJob.
            ScheduleJob.scheduleJob(mApplicationContext);

            postValue(WatchZoneModelRepository.this);
        }
    };

    private final Observer<WatchZoneModel> mWatchZoneModelObserver = new Observer<WatchZoneModel>() {
        @Override
        public void onChanged(@Nullable WatchZoneModel watchZoneModel) {
            // This will spam call to start a scheduleJob. It should only run if 30 seconds pass
            // since the last call to schedule the scheduleJob.
            ScheduleJob.scheduleJob(mApplicationContext);

            // WatchZoneModel changes trigger this repository to issue a change.
            postValue(WatchZoneModelRepository.this);
        }
    };

    private WatchZoneModelRepository(Context context) {
        mApplicationContext = context.getApplicationContext();
        mThread = new HandlerThread("WatchZoneModelRepositoryUpdateThread");
        mThread.start();
        mHandler = new Handler(/*mThread.getLooper()*/Looper.getMainLooper());

        mWatchZoneModelsMap = new HashMap<>();

        List<Long> watchZoneUids = WatchZoneRepository.getInstance(mApplicationContext).getWatchZoneUids();
        for (Long uid : watchZoneUids) {
            mWatchZoneModelsMap.put(uid, null);
        }

        WatchZoneRepository.getInstance(mApplicationContext).getWatchZonesLiveData()
                .observeForever(mWatchZoneObserver);
        setValue(this);
    }

    public static synchronized WatchZoneModelRepository getInstance(Context context) {
        if (sInstance.getValue() == null) {
            sInstance.setValue(new WatchZoneModelRepository(context));
        }
        return sInstance.getValue();
    }

    public synchronized long createWatchZone(String label, double centerLatitude,
                                             double centerLongitude, int radius) {
        long newWatchZoneUid = WatchZoneRepository.getInstance(mApplicationContext).createWatchZone(label,
                centerLatitude, centerLongitude, radius);
        if (newWatchZoneUid >= 0L) {
            mWatchZoneModelsMap.put(newWatchZoneUid, null);
        }
        return newWatchZoneUid;
    }

    public static LiveData<WatchZoneModelRepository> getInstanceLiveData() {
        return sInstance;
    }

    public synchronized void delete() {
        if (sInstance.getValue() != null) {
            // TODO Remove observers
            WatchZoneRepository.getInstance(mApplicationContext).getWatchZonesLiveData()
                .removeObserver(mWatchZoneObserver);
            for (Long watchZoneUid : mWatchZoneModelsMap.keySet()) {
                WatchZoneModel model = mWatchZoneModelsMap.get(watchZoneUid);
                if (model != null) {
                    model.removeObserver(mWatchZoneModelObserver);
                }
            }

            mThread.quit();

            sInstance.setValue(null);
        }
    }

    public synchronized Map<Long, WatchZoneModel> getWatchZoneModels() {
        return mWatchZoneModelsMap;
    }

    public synchronized WatchZoneModel getWatchZoneModel(long watchZoneUid) {
        return mWatchZoneModelsMap.get(watchZoneUid);
    }

    public synchronized boolean watchZoneExists(long watchZoneUid) {
        boolean result = mWatchZoneModelsMap.containsKey(watchZoneUid);
        if (!result) {
            List<Long> watchZoneUids = WatchZoneRepository.getInstance(mApplicationContext).getWatchZoneUids();
            for (Long uid : watchZoneUids) {
                if (!mWatchZoneModelsMap.containsKey(uid)) {
                    mWatchZoneModelsMap.put(uid, null);
                }
            }
        }
        result = mWatchZoneModelsMap.containsKey(watchZoneUid);
        return result;
    }

    @Override
    protected void onActive() {
        super.onActive();
        // Do nothing, because we're a singleton and want to exist until delete is called.
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        // Do nothing, because we're a singleton and want to exist until delete is called.
    }
}
