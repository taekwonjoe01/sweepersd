package com.example.joseph.sweepersd.watchzone.model;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.joseph.sweepersd.limit.OnDeviceLimitProviderService;
import com.example.joseph.sweepersd.utils.BooleanPreferenceLiveData;
import com.example.joseph.sweepersd.utils.ChangeSet;
import com.example.joseph.sweepersd.utils.LocationUtils;
import com.example.joseph.sweepersd.utils.LongPreferenceLiveData;
import com.example.joseph.sweepersd.utils.Preferences;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class WatchZoneModelUpdater extends LiveData<Map<Long, Integer>> implements
        WatchZoneUpdater.AddressProvider,
        WatchZoneUpdater.WatchZonePointSaveDelegate {
    private static final String TAG = WatchZoneModelUpdater.class.getSimpleName();
    private static WatchZoneModelUpdater sInstance;

    private final Context mApplicationContext;
    private final HandlerThread mThread;
    private final Handler mHandler;
    private final BooleanPreferenceLiveData mLimitsLoadedLiveData;
    private final LongPreferenceLiveData mExplorerUidLiveData;
    private final Map<Long, WatchZoneContainer> mUpdatingWatchZones;

    private LiveData<List<WatchZoneModel>> mModelLiveData;
    private WatchZoneModelsObserver mModelsObserver;
    private Observer<List<WatchZoneModel>> mTempObserver;
    private Observer<Boolean> mLimitsLoadedObserver;
    private Observer<Long> mExplorerUidObserver;
    private boolean mLimitsLoaded;
    private Map<Long, WatchZoneModel> mWatchZones;

    private WatchZoneModelUpdater(Context context) {
        mApplicationContext = context.getApplicationContext();
        mThread = new HandlerThread("WatchZoneModelUpdater-thread");
        mThread.start();
        mHandler = new Handler(mThread.getLooper());
        mUpdatingWatchZones = new HashMap<>();

        mLimitsLoadedLiveData = new BooleanPreferenceLiveData(mApplicationContext, Preferences.PREFERENCE_ON_DEVICE_LIMITS_LOADED);

        mExplorerUidLiveData = new LongPreferenceLiveData(mApplicationContext,
                Preferences.PREFERENCE_WATCH_ZONE_EXPLORER_UID);
        mLimitsLoaded = false;
    }

    public static synchronized WatchZoneModelUpdater getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new WatchZoneModelUpdater(context);
        }

        return sInstance;
    }

    public synchronized void delete() {
        if (sInstance != null) {
            onInactive();
            final CountDownLatch latch = new CountDownLatch(1);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    latch.countDown();
                }
            });

            // Wait for the cancels to complete.
            try {
                latch.await();
            } catch (InterruptedException e) {

            }

            mHandler.removeCallbacksAndMessages(null);
            mThread.quit();

            sInstance = null;
        }
    }

    @Override
    protected synchronized void onActive() {
        Log.d(TAG, "AppUpdateJob onActive");
        mModelsObserver = new WatchZoneModelsObserver(false, new WatchZoneModelsObserver.WatchZoneModelsChangedCallback() {
            @Override
            public void onModelsChanged(Map<Long, WatchZoneModel> data, ChangeSet changeSet) {
                mWatchZones = data;
                if (mLimitsLoaded) {
                    invalidate(data, changeSet);
                }
            }

            @Override
            public void onDataLoaded(Map<Long, WatchZoneModel> data) {
                mWatchZones = data;
                if (mLimitsLoaded) {
                    invalidate(data, null);
                }
            }

            @Override
            public void onDataInvalid() {
                // This should never happen.
            }
        });
        mLimitsLoadedObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean limitsLoaded) {
                if (limitsLoaded && !mLimitsLoaded && mWatchZones != null) {
                    ChangeSet changeSet = new ChangeSet();
                    for (Long uid : mWatchZones.keySet()) {
                        changeSet.changedUids.add(uid);
                    }
                    invalidate(mWatchZones, changeSet);
                } else if (!limitsLoaded) {
                    Intent msgIntent = new Intent(mApplicationContext, OnDeviceLimitProviderService.class);
                    mApplicationContext.startService(msgIntent);
                }
                mLimitsLoaded = limitsLoaded;
            }
        };
        mExplorerUidObserver = new Observer<Long>() {
            @Override
            public void onChanged(@Nullable Long aLong) {
                //invalidate(WatchZoneModelRepository.getInstance(mApplicationContext));
            }
        };
        mLimitsLoadedLiveData.observeForever(mLimitsLoadedObserver);
        mModelLiveData = WatchZoneModelRepository.getInstance(mApplicationContext).getWatchZoneModelsLiveData();
        mModelLiveData.observeForever(mModelsObserver);
        mTempObserver = new Observer<List<WatchZoneModel>>() {
            @Override
            public void onChanged(@Nullable List<WatchZoneModel> watchZoneModels) {
                if (watchZoneModels == null) {
                    Log.e(TAG, "AppUpdateJob onChanged null");
                }
                if (watchZoneModels.isEmpty()) {
                    Log.e(TAG, "AppUpdateJob onChanged empty");
                }
                if (!watchZoneModels.isEmpty()) {
                    Log.e(TAG, "AppUpdateJob onChanged size " + watchZoneModels.size());
                }
            }
        };
        mModelLiveData.observeForever(mTempObserver);
        mExplorerUidLiveData.observeForever(mExplorerUidObserver);

        // This is done to signal that this is busy, don't think it's done yet!
        setValue(null);
    }

    @Override
    protected synchronized void onInactive() {
        Log.d(TAG, "AppUpdateJob onInactive");
        mExplorerUidLiveData.removeObserver(mExplorerUidObserver);
        mLimitsLoadedLiveData.removeObserver(mLimitsLoadedObserver);
        mModelLiveData.removeObserver(mModelsObserver);
        mModelLiveData.removeObserver(mTempObserver);
        cancelAll();
    }

    private synchronized void cancelAll() {
        List<WatchZoneContainer> toCancel = new ArrayList<>();
        for (Long uid : mUpdatingWatchZones.keySet()) {
            WatchZoneContainer container = mUpdatingWatchZones.get(uid);
            toCancel.add(container);
        }
        for (WatchZoneContainer container : toCancel) {
            cancelAndRemoveContainer(container);
        }
    }

    private class WatchZoneContainer {
        WatchZoneModel watchZoneModel;
        WatchZone updatingWatchZone;
        WatchZoneUpdater watchZoneUpdater;
        Integer progress;
    }

    private boolean needsUpdate(WatchZoneModel model) {
        boolean result = false;
        for (WatchZonePointModel pointModel : model.points) {
            if (pointModel.point.getAddress() == null) {
                result = true;
            }
        }
        return result;
    }

    private synchronized void invalidate(Map<Long, WatchZoneModel> zoneModels, ChangeSet changeSet) {
        List<Long> toCancel = new ArrayList<>();
        List<Long> toAdd = new ArrayList<>();
        if (changeSet != null) {
            toCancel.addAll(changeSet.removedUids);
            toCancel.addAll(changeSet.changedUids);
            toAdd.addAll(changeSet.addedUids);
            toAdd.addAll(changeSet.changedUids);
        } else {
            toAdd.addAll(new ArrayList<>(zoneModels.keySet()));
        }

        for (Long uid : toCancel) {
            WatchZoneContainer container = mUpdatingWatchZones.get(uid);
            if (container != null) {
                cancelAndRemoveContainer(container);
            }
        }

        Map<Long, Integer> progressMap = new HashMap<>();
        if (!toAdd.isEmpty()) {
            for (Long uid : mUpdatingWatchZones.keySet()) {
                WatchZoneContainer container = mUpdatingWatchZones.get(uid);
                progressMap.put(container.updatingWatchZone.getUid(), container.progress);
            }
            cancelAll();
            toAdd.clear();
            for (Long uid : zoneModels.keySet()) {
                WatchZoneModel model = zoneModels.get(uid);
                if (needsUpdate(model)) {
                    toAdd.add(uid);
                }
            }
        }

        Collections.sort(toAdd, new Comparator<Long>() {
            @Override
            public int compare(Long t1, Long t2) {
                long diff = t2 - t1;
                return diff < 0 ? -1 : diff > 0 ? 1 : 0;
            }
        });

        for (Long uid : toAdd) {
            final WatchZoneModel model = zoneModels.get(uid);

            final WatchZoneContainer container = new WatchZoneContainer();
            container.watchZoneModel = model;
            container.updatingWatchZone = model.watchZone;
            container.watchZoneUpdater = null;
            if (progressMap.containsKey(model.watchZone.getUid())) {
                container.progress = progressMap.get(model.watchZone.getUid());
            } else {
                container.progress = 0;
            }

            mUpdatingWatchZones.put(uid, container);

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (WatchZoneModelUpdater.this) {
                        WatchZoneContainer containerInMap = mUpdatingWatchZones.get(container.watchZoneModel.watchZone.getUid());
                        if (container != containerInMap) {
                            return;
                        }
                    }
                    int numThreads = Runtime.getRuntime().availableProcessors() * 32;
                    List<HandlerThread> threads = new ArrayList<>();
                    List<Handler> handlers = new ArrayList<>();
                    Log.d(TAG, "Starting " + numThreads + " threads.");

                    for (int i = 0; i < numThreads; i++) {
                        HandlerThread thread = new HandlerThread("WatchZoneUpdater_" + (i + 1));
                        thread.start();
                        Handler handler = new Handler(thread.getLooper());

                        threads.add(thread);
                        handlers.add(handler);
                    }

                    container.watchZoneUpdater = new WatchZoneUpdater(mApplicationContext, container.watchZoneModel,
                            container.progress, new WatchZoneUpdater.ProgressListener() {
                        @Override
                        public void onProgress(WatchZoneUpdater.UpdateProgress progress) {
                            if (WatchZoneUpdater.UpdateProgress.Status.UPDATING ==
                                    progress.getStatus()) {
                                container.progress = progress.getProgress();
                                postUpdatedData();
                            }
                        }
                    }, handlers, WatchZoneModelUpdater.this,
                    WatchZoneModelUpdater.this);

                    boolean cancel = false;
                    synchronized (WatchZoneModelUpdater.this) {
                        WatchZoneContainer containerInMap = mUpdatingWatchZones.get(container.watchZoneModel.watchZone.getUid());
                        if (container != containerInMap) {
                            cancel = true;
                        }
                    }
                    if (!cancel) {
                        // This will block!
                        WatchZoneUpdater.UpdateProgress finalProgress = container.watchZoneUpdater.execute();

                        if (WatchZoneUpdater.UpdateProgress.Status.CANCELLED == finalProgress.getStatus()) {
                            // TODO is needed?
                        }
                    }

                    for (HandlerThread thread : threads) {
                        thread.quit();
                    }

                    synchronized (WatchZoneModelUpdater.this) {
                        WatchZoneContainer containerInMap = mUpdatingWatchZones.get(container.watchZoneModel.watchZone.getUid());
                        if (container == containerInMap) {
                            mUpdatingWatchZones.remove(container.watchZoneModel.watchZone.getUid());
                            postUpdatedData();
                        }
                    }
                }
            });
        }

        postUpdatedData();
    }

    private synchronized void cancelAndRemoveContainer(WatchZoneContainer container) {
        if (container.watchZoneUpdater != null) {
            container.watchZoneUpdater.cancel();
        }
        mUpdatingWatchZones.remove(container.watchZoneModel.watchZone.getUid());
    }

    private synchronized void postUpdatedData() {
        Map<Long, Integer> data = new HashMap<>();
        for (Long uid : mUpdatingWatchZones.keySet()) {
            WatchZoneContainer container = mUpdatingWatchZones.get(uid);
            data.put(uid, container.progress);
        }
        Log.e(TAG, "posting Value AppUpdateJob");
        postValue(data);
    }

    @Override
    public String getAddressForLatLng(LatLng latLng) {
        return LocationUtils.getAddressForLatLnt(mApplicationContext, latLng);
    }

    @Override
    public void saveWatchZonePoint(WatchZonePoint p) {
        if (mUpdatingWatchZones.containsKey(p.getWatchZoneId())) {
            WatchZoneModelRepository.getInstance(mApplicationContext).updateWatchZonePoint(p);
        }
    }
}
