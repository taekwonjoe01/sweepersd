package com.example.joseph.sweepersd.watchzone.model;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.joseph.sweepersd.limit.LimitModel;
import com.example.joseph.sweepersd.limit.LimitModelObserver;
import com.example.joseph.sweepersd.limit.LimitRepository;
import com.example.joseph.sweepersd.utils.BaseObserver;
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
    private final WatchZoneModelsObserver mModelsObserver;
    private final LimitModelObserver mLimitsObserver;
    private final LongPreferenceLiveData mExplorerUidLiveData;
    private final Observer<Long> mExplorerUidObserver;
    private final Map<Long, WatchZoneContainer> mUpdatingWatchZones;

    private List<LimitModel> mLimits;
    private Map<Long, ZoneModel> mWatchZones;

    private WatchZoneModelUpdater(Context context) {
        mApplicationContext = context.getApplicationContext();
        mThread = new HandlerThread("WatchZoneModelUpdater-thread");
        mThread.start();
        mHandler = new Handler(mThread.getLooper());
        mUpdatingWatchZones = new HashMap<>();
        mModelsObserver = new WatchZoneModelsObserver(false, new WatchZoneModelsObserver.WatchZoneModelsChangedCallback() {
            @Override
            public void onModelsChanged(Map<Long, ZoneModel> data, BaseObserver.ChangeSet changeSet) {
                mWatchZones = data;
                if (mLimits != null) {
                    invalidate(data, changeSet);
                }
            }

            @Override
            public void onDataLoaded(Map<Long, ZoneModel> data) {
                mWatchZones = data;
                if (mLimits != null) {
                    invalidate(data, null);
                }
            }

            @Override
            public void onDataInvalid() {
                // This should never happen.
            }
        });
        mLimitsObserver = new LimitModelObserver(new LimitModelObserver.LimitModelCallback() {
            @Override
            public void onLimitModelsChanged(List<LimitModel> limitModels) {
                if (mWatchZones != null) {
                    // TODO refresh all
                    BaseObserver.ChangeSet changeSet = new BaseObserver.ChangeSet();
                    for (Long uid : mWatchZones.keySet()) {
                        changeSet.changedLimits.add(uid);
                    }
                    invalidate(mWatchZones, changeSet);
                }
                mLimits = limitModels;
            }

            @Override
            public void onDataLoaded(List<LimitModel> data) {
                mLimits = data;
            }

            @Override
            public void onDataInvalid() {
                mLimits = null;
            }
        });
        mExplorerUidLiveData = new LongPreferenceLiveData(mApplicationContext,
                Preferences.PREFERENCE_WATCH_ZONE_EXPLORER_UID);
        mExplorerUidObserver = new Observer<Long>() {
            @Override
            public void onChanged(@Nullable Long aLong) {
                //invalidate(WatchZoneModelRepository.getInstance(mApplicationContext));
            }
        };
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
        LimitRepository.getInstance(mApplicationContext).getPostedLimitsLiveData().observeForever(mLimitsObserver);
        WatchZoneModelRepository.getInstance(mApplicationContext).getZoneModelsLiveData().observeForever(mModelsObserver);
        mExplorerUidLiveData.observeForever(mExplorerUidObserver);
    }

    @Override
    protected synchronized void onInactive() {
        mExplorerUidLiveData.removeObserver(mExplorerUidObserver);
        LimitRepository.getInstance(mApplicationContext).getPostedLimitsLiveData().removeObserver(mLimitsObserver);
        WatchZoneModelRepository.getInstance(mApplicationContext).getZoneModelsLiveData().removeObserver(mModelsObserver);
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
        ZoneModel zoneModel;
        WatchZone updatingWatchZone;
        WatchZoneUpdater watchZoneUpdater;
        Integer progress;
    }

    private boolean needsRefresh(WatchZone first, WatchZone second) {
        return first.getCenterLatitude() != second.getCenterLatitude() ||
                first.getCenterLongitude() != second.getCenterLongitude() ||
                first.getRadius() != second.getRadius();
    }

    private synchronized void invalidate(Map<Long, ZoneModel> zoneModels, BaseObserver.ChangeSet changeSet) {
        List<Long> toCancel = new ArrayList<>();
        List<Long> toAdd = new ArrayList<>();
        if (changeSet != null) {
            toCancel.addAll(changeSet.removedLimits);
            toCancel.addAll(changeSet.changedLimits);
            toAdd.addAll(changeSet.addedLimits);
            toAdd.addAll(changeSet.changedLimits);
        } else {
            toAdd.addAll(new ArrayList<>(zoneModels.keySet()));
        }

        for (Long uid : toCancel) {
            WatchZoneContainer container = mUpdatingWatchZones.get(uid);
            if (container != null) {
                cancelAndRemoveContainer(container);
            }
        }

        if (!toAdd.isEmpty()) {
            cancelAll();
            toAdd.clear();
            for (Long uid : zoneModels.keySet()) {
                toAdd.add(uid);
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
            final ZoneModel model = zoneModels.get(uid);

            final WatchZoneContainer container = new WatchZoneContainer();
            container.zoneModel = model;
            container.updatingWatchZone = model.watchZone;
            container.watchZoneUpdater = null;
            container.progress = 0;

            mUpdatingWatchZones.put(uid, container);

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (WatchZoneModelUpdater.this) {
                        WatchZoneContainer containerInMap = mUpdatingWatchZones.get(container.zoneModel.watchZone.getUid());
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

                    container.watchZoneUpdater = new WatchZoneUpdater(container.zoneModel,
                            new WatchZoneUpdater.ProgressListener() {
                                @Override
                                public void onProgress(WatchZoneUpdater.UpdateProgress progress) {
                                    if (WatchZoneUpdater.UpdateProgress.Status.UPDATING ==
                                            progress.getStatus()) {
                                        container.progress = progress.getProgress();
                                        postUpdatedData();
                                    }
                                }
                            }, handlers, WatchZoneModelUpdater.this,
                            mLimits,
                            WatchZoneModelUpdater.this);

                    boolean cancel = false;
                    synchronized (WatchZoneModelUpdater.this) {
                        WatchZoneContainer containerInMap = mUpdatingWatchZones.get(container.zoneModel.watchZone.getUid());
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
                        WatchZoneContainer containerInMap = mUpdatingWatchZones.get(container.zoneModel.watchZone.getUid());
                        if (container == containerInMap) {
                            mUpdatingWatchZones.remove(container.zoneModel.watchZone.getUid());
                            postUpdatedData();
                        }
                    }
                }
            });
        }

        postUpdatedData();
    }

    /*private synchronized void invalidate(WatchZoneModelRepository repository) {
        if (mLimits == null) {
            return;
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
        if (!preferences.getBoolean(Preferences.PREFERENCE_ON_DEVICE_LIMITS_LOADED, false)) {
            return;
        }

        List<WatchZoneModel> modelsToSchedule = new ArrayList<>(repository.getWatchZoneModels().values());
        Map<Long, WatchZoneModel> models = new HashMap<>();//mModelsObserver.getWatchZoneModels();
        for (Long uid : models.keySet()) {
            WatchZoneModel model = models.get(uid);
            if (model != null && model.getWatchZoneUid() == mExplorerUidLiveData.getValue()) {
                modelsToSchedule.remove(model);
            }
        }

        List<WatchZoneModel> modelsThatNeedUpdate = new ArrayList<>();
        for (Long uid : models.keySet()) {
            WatchZoneModel model = models.get(uid);
            if (model != null && model.getStatus() != WatchZoneModel.Status.VALID &&
                    model.getStatus() != WatchZoneModel.Status.LOADING) {
                modelsThatNeedUpdate.add(model);
            }
        }

        List<WatchZoneModel> newModelsToUpdate = new ArrayList<>();
        List<Long> uidsThatNoLongerExist = new ArrayList<>(mUpdatingWatchZones.keySet());
        for (WatchZoneModel model : modelsThatNeedUpdate) {
            Long uid = model.getWatchZone().getUid();
            if (!mUpdatingWatchZones.containsKey(uid)) {
                newModelsToUpdate.add(model);
                uidsThatNoLongerExist.remove(uid);
            } else {
                WatchZoneContainer container = mUpdatingWatchZones.get(uid);
                if (needsRefresh(container.updatingWatchZone, model.getWatchZone())) {
                    // Keep it on the cancel and remove list...
                    // Then add it back to the to-update list.
                    newModelsToUpdate.add(model);
                } else {
                    uidsThatNoLongerExist.remove(uid);
                }
            }
        }

        for (Long uid : uidsThatNoLongerExist) {
            WatchZoneContainer container = mUpdatingWatchZones.get(uid);
            cancelAndRemoveContainer(container);
        }

        if (!newModelsToUpdate.isEmpty()) {
            cancelAll();
            newModelsToUpdate.clear();
            for (WatchZoneModel model : modelsThatNeedUpdate) {
                newModelsToUpdate.add(model);
            }
        }

        Collections.sort(newModelsToUpdate, new Comparator<WatchZoneModel>() {
            @Override
            public int compare(WatchZoneModel t1, WatchZoneModel t2) {
                long diff = t2.getWatchZoneUid() - t1.getWatchZoneUid();
                return diff < 0 ? -1 : diff > 0 ? 1 : 0;
            }
        });

        for (final WatchZoneModel model : newModelsToUpdate) {
            final Long uid = model.getWatchZone().getUid();

            final WatchZoneContainer container = new WatchZoneContainer();
            container.watchZoneModel = model;
            container.updatingWatchZone = model.getWatchZone();
            container.watchZoneUpdater = null;
            container.progress = 0;

            mUpdatingWatchZones.put(uid, container);

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (WatchZoneModelUpdater.this) {
                        WatchZoneContainer containerInMap = mUpdatingWatchZones.get(container.watchZoneModel.getWatchZoneUid());
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

                    container.watchZoneUpdater = new WatchZoneUpdater(container.watchZoneModel,
                            new WatchZoneUpdater.ProgressListener() {
                                @Override
                                public void onProgress(WatchZoneUpdater.UpdateProgress progress) {
                                    if (WatchZoneUpdater.UpdateProgress.Status.UPDATING ==
                                            progress.getStatus()) {
                                        container.progress = progress.getProgress();
                                        postUpdatedData();
                                    }
                                }
                            }, handlers, WatchZoneModelUpdater.this,
                            mLimits.getValue(),
                            WatchZoneModelUpdater.this);

                    boolean cancel = false;
                    synchronized (WatchZoneModelUpdater.this) {
                        WatchZoneContainer containerInMap = mUpdatingWatchZones.get(container.watchZoneModel.getWatchZoneUid());
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
                        WatchZoneContainer containerInMap = mUpdatingWatchZones.get(container.watchZoneModel.getWatchZoneUid());
                        if (container == containerInMap) {
                            mUpdatingWatchZones.remove(container.watchZoneModel.getWatchZoneUid());
                            postUpdatedData();
                        }
                    }
                }
            });
        }

        postUpdatedData();
    }*/

    private synchronized void cancelAndRemoveContainer(WatchZoneContainer container) {
        if (container.watchZoneUpdater != null) {
            container.watchZoneUpdater.cancel();
        }
        mUpdatingWatchZones.remove(container.zoneModel.watchZone.getUid());
    }

    private synchronized void postUpdatedData() {
        Map<Long, Integer> data = new HashMap<>();
        for (Long uid : mUpdatingWatchZones.keySet()) {
            WatchZoneContainer container = mUpdatingWatchZones.get(uid);
            data.put(uid, container.progress);
        }
        postValue(data);
    }

    @Override
    public String getAddressForLatLng(LatLng latLng) {
        return LocationUtils.getAddressForLatLnt(mApplicationContext, latLng);
    }

    @Override
    public void saveWatchZonePoint(WatchZonePoint p) {
        if (mUpdatingWatchZones.containsKey(p.getWatchZoneId())) {
            WatchZoneRepository.getInstance(mApplicationContext).updateWatchZonePoint(p);
        }
    }
}
