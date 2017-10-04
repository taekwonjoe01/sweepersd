package com.example.joseph.sweepersd.revision3.watchzone;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.example.joseph.sweepersd.revision3.utils.LocationUtils;
import com.example.joseph.sweepersd.revision3.utils.Preferences;
import com.example.joseph.sweepersd.revision3.limit.Limit;
import com.example.joseph.sweepersd.revision3.limit.LimitRepository;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class WatchZoneModelUpdater extends LiveData<Map<Long, Integer>> implements
        WatchZoneUpdater.AddressProvider,
        WatchZoneUpdater.WatchZonePointSaveDelegate {
    private static WatchZoneModelUpdater sInstance;

    private final Context mApplicationContext;
    private final HandlerThread mThread;
    private final Handler mHandler;
    private final Observer<List<WatchZoneModel>> mWatchZoneObserver;
    private final Observer<List<Limit>> mLimitObserver;

    private Map<Long, WatchZoneContainer> mUpdatingWatchZones;
    private LiveData<List<WatchZoneModel>> mWatchZoneModels;
    private LiveData<List<Limit>> mLimits;


    private WatchZoneModelUpdater(Context context) {
        mApplicationContext = context.getApplicationContext();
        mThread = new HandlerThread("WatchZoneModelUpdater-thread");
        mThread.start();
        mHandler = new Handler(mThread.getLooper());
        mWatchZoneObserver = new Observer<List<WatchZoneModel>>() {
            @Override
            public void onChanged(@Nullable List<WatchZoneModel> watchZoneModels) {
                invalidate(watchZoneModels);
            }
        };
        mLimitObserver = new Observer<List<Limit>>() {
            @Override
            public void onChanged(@Nullable List<Limit> limits) {
                invalidate(mWatchZoneModels.getValue());
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
    protected void onActive() {
        mUpdatingWatchZones = new HashMap<>();
        postUpdatedData();

        mWatchZoneModels = WatchZoneRepository.getInstance(mApplicationContext)
                .getAllWatchZoneModelsLiveData();
        mWatchZoneModels.observeForever(mWatchZoneObserver);
        mLimits = LimitRepository.getInstance(mApplicationContext).getPostedLimitsLiveData();
        mLimits.observeForever(mLimitObserver);
    }

    @Override
    protected synchronized void onInactive() {
        mLimits.removeObserver(mLimitObserver);
        mWatchZoneModels.removeObserver(mWatchZoneObserver);
        for (Long uid : mUpdatingWatchZones.keySet()) {
            WatchZoneContainer container = mUpdatingWatchZones.get(uid);
            if (container.watchZoneUpdater != null) {
                container.watchZoneUpdater.cancel();
            }
        }
        mUpdatingWatchZones.clear();
        postUpdatedData();
    }

    private class WatchZoneContainer {
        WatchZoneModel watchZoneModel;
        WatchZoneUpdater watchZoneUpdater;
        Integer progress;
    }

    private boolean needsRefresh(WatchZoneModel first, WatchZoneModel second) {
        return first.getWatchZone().getCenterLatitude() != second.getWatchZone().getCenterLatitude() ||
                first.getWatchZone().getCenterLongitude() != second.getWatchZone().getCenterLongitude() ||
                first.getWatchZone().getRadius() != second.getWatchZone().getRadius();
    }

    private synchronized void invalidate(List<WatchZoneModel> models) {
        if (models == null) {
            return;
        }

        if (mLimits.getValue() == null) {
            return;
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
        if (!preferences.getBoolean(Preferences.PREFERENCE_ON_DEVICE_LIMITS_LOADED, false)) {
            return;
        }

        List<WatchZoneModel> modelsThatNeedUpdate = new ArrayList<>();
        for (WatchZoneModel model : models) {
            if (model.getStatus() != WatchZoneModel.WatchZoneStatus.VALID) {
                modelsThatNeedUpdate.add(model);
            }
        }

        List<WatchZoneModel> newModelsToUpdate = new ArrayList<>();
        List<Long> uidsThatNoLongerExist = new ArrayList<>(mUpdatingWatchZones.keySet());
        for (WatchZoneModel model : modelsThatNeedUpdate) {
            Long uid = model.getWatchZone().getUid();
            if (!mUpdatingWatchZones.containsKey(uid)) {
                newModelsToUpdate.add(model);
            } else {
                WatchZoneContainer container = mUpdatingWatchZones.get(uid);
                if (needsRefresh(container.watchZoneModel, model)) {
                    // Add it to the cancel and remove list...
                    uidsThatNoLongerExist.add(uid);
                    // Then add it back to the to-update list.
                    newModelsToUpdate.add(model);
                }

            }
            uidsThatNoLongerExist.remove(uid);
        }

        for (Long uid : uidsThatNoLongerExist) {
            WatchZoneUpdater updater = mUpdatingWatchZones.get(uid).watchZoneUpdater;
            if (updater != null) {
                updater.cancel();
            }
            mUpdatingWatchZones.remove(uid);
        }

        for (final WatchZoneModel model : newModelsToUpdate) {
            final Long uid = model.getWatchZone().getUid();

            WatchZoneContainer container = new WatchZoneContainer();
            container.watchZoneModel = model;
            container.watchZoneUpdater = null;
            container.progress = 0;

            mUpdatingWatchZones.put(uid, container);

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    WatchZoneContainer cont = null;
                    synchronized (WatchZoneModelUpdater.this) {
                        cont = mUpdatingWatchZones.get(uid);
                    }

                    if (cont != null) {
                        int numThreads = Runtime.getRuntime().availableProcessors() * 2;
                        List<HandlerThread> threads = new ArrayList<>();
                        List<Handler> handlers = new ArrayList<>();

                        for (int i = 0; i < numThreads; i++) {
                            HandlerThread thread = new HandlerThread("WatchZoneUpdater_" + (i + 1));
                            thread.start();
                            Handler handler = new Handler(thread.getLooper());

                            threads.add(thread);
                            handlers.add(handler);
                        }

                        cont.watchZoneUpdater = new WatchZoneUpdater(cont.watchZoneModel,
                                new WatchZoneUpdater.ProgressListener() {
                                    @Override
                                    public void onProgress(WatchZoneUpdater.UpdateProgress progress) {
                                        if (WatchZoneUpdater.UpdateProgress.Status.UPDATING ==
                                                progress.getStatus()) {
                                            WatchZoneContainer c = null;
                                            synchronized (WatchZoneModelUpdater.this) {
                                                c = mUpdatingWatchZones.get(uid);
                                                if (c != null) {
                                                    c.progress = progress.getProgress();
                                                    postUpdatedData();
                                                }
                                            }
                                        }
                                    }
                                }, handlers, WatchZoneModelUpdater.this,
                                mLimits.getValue(),
                                WatchZoneModelUpdater.this);

                        // This will block!
                        WatchZoneUpdater.UpdateProgress finalProgress = cont.watchZoneUpdater.execute();

                        if (WatchZoneUpdater.UpdateProgress.Status.CORRUPT == finalProgress.getStatus()) {
                            // TODO is needed?
                        }

                        for (HandlerThread thread : threads) {
                            thread.quit();
                        }

                        synchronized (WatchZoneModelUpdater.this) {
                            mUpdatingWatchZones.remove(cont.watchZoneModel.getWatchZone().getUid());
                        }
                        postUpdatedData();
                    }
                }
            });
        }

        postUpdatedData();
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
