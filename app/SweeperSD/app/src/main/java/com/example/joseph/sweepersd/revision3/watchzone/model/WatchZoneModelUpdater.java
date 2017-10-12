package com.example.joseph.sweepersd.revision3.watchzone.model;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.joseph.sweepersd.revision3.limit.Limit;
import com.example.joseph.sweepersd.revision3.limit.LimitRepository;
import com.example.joseph.sweepersd.revision3.utils.LocationUtils;
import com.example.joseph.sweepersd.revision3.utils.Preferences;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collections;
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
    private final Observer<WatchZoneModelRepository> mRepositoryObserver;
    private final Observer<List<Limit>> mLimitObserver;
    private final Map<Long, WatchZoneContainer> mUpdatingWatchZones;

    private LiveData<List<Limit>> mLimits;


    private WatchZoneModelUpdater(Context context) {
        mApplicationContext = context.getApplicationContext();
        mThread = new HandlerThread("WatchZoneModelUpdater-thread");
        mThread.start();
        mHandler = new Handler(mThread.getLooper());
        mUpdatingWatchZones = new HashMap<>();
        mRepositoryObserver = new Observer<WatchZoneModelRepository>() {
            @Override
            public void onChanged(@Nullable WatchZoneModelRepository repository) {
                invalidate(repository);
            }
        };
        mLimitObserver = new Observer<List<Limit>>() {
            @Override
            public void onChanged(@Nullable List<Limit> limits) {
                invalidate(WatchZoneModelRepository.getInstance(mApplicationContext));
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
        mLimits = LimitRepository.getInstance(mApplicationContext).getPostedLimitsLiveData();
        mLimits.observeForever(mLimitObserver);
        WatchZoneModelRepository.getInstance(mApplicationContext).observeForever(mRepositoryObserver);
    }

    @Override
    protected synchronized void onInactive() {
        mLimits.removeObserver(mLimitObserver);
        WatchZoneModelRepository.getInstance(mApplicationContext).removeObserver(mRepositoryObserver);
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
        WatchZoneUpdater watchZoneUpdater;
        Integer progress;
    }

    private boolean needsRefresh(WatchZoneModel first, WatchZoneModel second) {
        return first.getWatchZone().getCenterLatitude() != second.getWatchZone().getCenterLatitude() ||
                first.getWatchZone().getCenterLongitude() != second.getWatchZone().getCenterLongitude() ||
                first.getWatchZone().getRadius() != second.getWatchZone().getRadius();
    }

    private synchronized void scheduleWatchZoneNotifications(List<WatchZoneModel> watchZoneModels) {
        for (WatchZoneModel model : watchZoneModels) {
            if (model.getStatus() == WatchZoneModel.Status.VALID) {
                WatchZoneUtils.scheduleWatchZoneNotification(mApplicationContext, model);
            } else if (model.getStatus() != WatchZoneModel.Status.LOADING) {
                WatchZoneUtils.unscheduleWatchZoneNotification(mApplicationContext, model);
            }
        }
    }

    private synchronized void invalidate(WatchZoneModelRepository repository) {
        if (mLimits.getValue() == null) {
            return;
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
        if (!preferences.getBoolean(Preferences.PREFERENCE_ON_DEVICE_LIMITS_LOADED, false)) {
            return;
        }

        scheduleWatchZoneNotifications(repository.getWatchZoneModels());

        List<WatchZoneModel> modelsThatNeedUpdate = new ArrayList<>();
        for (WatchZoneModel model : repository.getWatchZoneModels()) {
            if (model.getStatus() != WatchZoneModel.Status.VALID &&
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

        Collections.reverse(newModelsToUpdate);

        for (final WatchZoneModel model : newModelsToUpdate) {
            final Long uid = model.getWatchZone().getUid();

            final WatchZoneContainer container = new WatchZoneContainer();
            container.watchZoneModel = model;
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
                            //Log.e("Joey", "Updater thread cancelled for " + container.watchZoneModel.getWatchZoneUid());
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
    }

    private synchronized void cancelAndRemoveContainer(WatchZoneContainer container) {
        if (container.watchZoneUpdater != null) {
            container.watchZoneUpdater.cancel();
        }
        mUpdatingWatchZones.remove(container.watchZoneModel.getWatchZoneUid());
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
