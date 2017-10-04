package com.example.joseph.sweepersd.revision3.watchzone;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.example.joseph.sweepersd.revision3.AppDatabase;
import com.example.joseph.sweepersd.revision3.utils.LocationUtils;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WatchZoneRepository {
    private static final String TAG = WatchZoneRepository.class.getSimpleName();

    private static WatchZoneRepository sInstance;
    private Context mContext;

    private final Handler mHandler;
    private final HandlerThread mThread;

    private final MutableLiveData<List<WatchZoneModel>> mCachedWatchZoneModels;
    private final Map<Long, WatchZoneContainer> mWatchZoneContainerMap;

    private final LiveData<List<WatchZone>> mWatchZonesLiveData;
    private final Observer<List<WatchZone>> mWatchZoneLiveDataObserver = new Observer<List<WatchZone>>() {
        @Override
        public void onChanged(@Nullable final List<WatchZone> watchZones) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    invalidateWatchZones(watchZones);
                }
            });
        }
    };
    private final Observer<WatchZoneModel> mWatchZoneModelObserver = new Observer<WatchZoneModel>() {
        @Override
        public void onChanged(@Nullable WatchZoneModel watchZoneModel) {
            updateLiveDataList();
        }
    };

    private class WatchZoneContainer {
        LiveData<List<WatchZonePoint>> watchZonePoints;
        Observer<List<WatchZonePoint>> pointsObserver;
        MutableLiveData<WatchZoneModel> watchZoneModel;
    }

    private WatchZoneRepository(Context context) {
        mContext = context.getApplicationContext();

        mThread = new HandlerThread("WatchZoneRepository_thread");
        mThread.start();
        mHandler = new Handler(mThread.getLooper());

        mWatchZonesLiveData = loadWatchZonesFromDb();
        mWatchZoneContainerMap = new HashMap<>();
        mCachedWatchZoneModels = new MutableLiveData<>();

        mWatchZonesLiveData.observeForever(mWatchZoneLiveDataObserver);
    }

    public synchronized static WatchZoneRepository getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new WatchZoneRepository(context);
        }
        return sInstance;
    }

    /**
     * Intended to only be called by the Application when memory is needed to be trimmed.
     */
    public synchronized void delete() {
        if (sInstance != null) {
            mWatchZonesLiveData.removeObserver(mWatchZoneLiveDataObserver);
            mHandler.removeCallbacksAndMessages(null);
            mThread.quit();
            sInstance = null;
        }
    }

    public synchronized LiveData<WatchZoneModel> getWatchZoneModelLiveData(Long uid) {
        return mWatchZoneContainerMap.get(uid).watchZoneModel;
    }

    public synchronized LiveData<List<WatchZoneModel>> getAllWatchZoneModelsLiveData() {
        return mCachedWatchZoneModels;
    }

    public synchronized void triggerRefreshAll() {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mContext).watchZoneDao();

        List<WatchZone> zones = watchZoneDao.getAllWatchZones();
        for (WatchZone zone : zones) {
            updateWatchZoneModel(zone.getUid(), zone.getLabel(), zone.getCenterLatitude(),
                    zone.getCenterLongitude(), zone.getRadius());
        }
    }

    public synchronized void triggerRefresh(Long uid) {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mContext).watchZoneDao();
        WatchZone zone = watchZoneDao.getWatchZone(uid);
        if (zone != null) {
            updateWatchZoneModel(zone.getUid(), zone.getLabel(), zone.getCenterLatitude(),
                    zone.getCenterLongitude(), zone.getRadius());
        }
    }

    public synchronized int updateWatchZoneModel(Long watchZoneUid, String label,
                                                 double centerLatitude, double centerLongitude,
                                                 int radius) {
        int result = 0;
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mContext).watchZoneDao();

        boolean invalidateWatchZonePoints = false;
        WatchZone watchZone = watchZoneDao.getWatchZone(watchZoneUid);
        if (watchZone != null) {
            if (centerLatitude != watchZone.getCenterLatitude()
                    || centerLongitude != watchZone.getCenterLongitude()
                    || radius != watchZone.getRadius()) {
                invalidateWatchZonePoints = true;
            }
            watchZone.setLabel(label);
            watchZone.setCenterLatitude(centerLatitude);
            watchZone.setCenterLongitude(centerLongitude);
            watchZone.setRadius(radius);
            result = watchZoneDao.updateWatchZone(watchZone);
            if (result > 0 && invalidateWatchZonePoints) {
                List<WatchZonePoint> oldPoints = watchZoneDao.getWatchZonePoints(watchZone.getUid());
                watchZoneDao.deleteWatchZonePoints(oldPoints);

                List<LatLng> latLngs = LocationUtils.getLatLngsInRadius(
                        new LatLng(watchZone.getCenterLatitude(), watchZone.getCenterLongitude()),
                        watchZone.getRadius());
                List<WatchZonePoint> points = new ArrayList<>();
                for (LatLng latLng : latLngs) {
                    WatchZonePoint point = new WatchZonePoint();
                    point.setLimitId(0L);
                    point.setAddress(null);
                    point.setWatchZoneUpdatedTimestampMs(0L);
                    point.setWatchZoneId(watchZone.getUid());
                    point.setLatitude(latLng.latitude);
                    point.setLongitude(latLng.longitude);
                    points.add(point);
                }

                watchZoneDao.insertWatchZonePoints(points);

                scheduleUpdateJob();
            }
        }

        return result;
    }

    public synchronized long createWatchZone(String label, double centerLatitude,
                                              double centerLongitude, int radius) {
        long startTime = SystemClock.elapsedRealtime();
        long result = 0;
        if (!TextUtils.isEmpty(label)) {
            WatchZone watchZone = new WatchZone();
            watchZone.setLabel(label);
            watchZone.setCenterLatitude(centerLatitude);
            watchZone.setCenterLongitude(centerLongitude);
            watchZone.setRadius(radius);
            watchZone.setLastSweepingUpdated(0L);

            WatchZoneDao watchZoneDao = AppDatabase.getInstance(mContext).watchZoneDao();
            result = watchZoneDao.insertWatchZone(watchZone);

            if (result > 0) {
                List<LatLng> latLngs = LocationUtils.getLatLngsInRadius(
                        new LatLng(watchZone.getCenterLatitude(), watchZone.getCenterLongitude()),
                        watchZone.getRadius());
                List<WatchZonePoint> points = new ArrayList<>();
                for (LatLng latLng : latLngs) {
                    WatchZonePoint point = new WatchZonePoint();
                    point.setLimitId(0L);
                    point.setAddress(null);
                    point.setWatchZoneUpdatedTimestampMs(0L);
                    point.setWatchZoneId(result);
                    point.setLatitude(latLng.latitude);
                    point.setLongitude(latLng.longitude);
                    points.add(point);
                }

                long[] uids = watchZoneDao.insertWatchZonePoints(points);

                scheduleUpdateJob();
            }
        }
        long endTime = SystemClock.elapsedRealtime();
        Log.d(TAG, "create Watch Zone with radius " + radius + " took "
                + (endTime - startTime) + "ms.");
        return result;
    }

    public synchronized int deleteWatchZone(Long uid) {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mContext).watchZoneDao();

        WatchZone watchZone = watchZoneDao.getWatchZone(uid);
        int result = 0;
        if (watchZone != null) {
            result = watchZoneDao.deleteWatchZone(watchZone);
        }

        return result;
    }

    /**
     * Intended for internal package use only.
     * @param watchZoneUid
     * @return
     */
    synchronized List<WatchZonePoint> getWatchZonePoints(Long watchZoneUid) {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mContext).watchZoneDao();

        return watchZoneDao.getWatchZonePoints(watchZoneUid);
    }

    /**
     * Intended for internal package use only.
     * @param watchZonePoint
     * @return
     */
    synchronized int updateWatchZonePoint(WatchZonePoint watchZonePoint) {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mContext).watchZoneDao();
        int result = watchZoneDao.updateWatchZonePoint(watchZonePoint);

        return result;
    }

    private LiveData<List<WatchZone>> loadWatchZonesFromDb() {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mContext).watchZoneDao();

        return watchZoneDao.getAllWatchZonesLiveData();
    }

    private LiveData<List<WatchZonePoint>> loadWatchZonePointsFromDb(WatchZone watchZone) {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mContext).watchZoneDao();

        return watchZoneDao.getWatchZonePointsLiveData(watchZone.getUid());
    }

    private void invalidateWatchZones(List<WatchZone> watchZones) {
        // Clean up old Observer Containers
        Set<Long> oldUids = mWatchZoneContainerMap.keySet();
        for (WatchZone watchZone : watchZones) {
            Long uid = watchZone.getUid();
            oldUids.remove(uid);
        }
        for (Long deletedUid : oldUids) {
            WatchZoneContainer container = mWatchZoneContainerMap.get(deletedUid);

            container.watchZonePoints.removeObserver(container.pointsObserver);
            // Set it to null so Observers know it's gone.
            container.watchZoneModel.postValue(null);
            container.watchZoneModel.removeObserver(mWatchZoneModelObserver);
            mWatchZoneContainerMap.remove(deletedUid);
        }

        // Setup new Observer Containers
        for (final WatchZone watchZone : watchZones) {
            if (mWatchZoneContainerMap.containsKey(watchZone.getUid())) {
                WatchZoneContainer container = mWatchZoneContainerMap.get(watchZone.getUid());
                WatchZoneModel model = container.watchZoneModel.getValue();
                if (model != null) {
                    WatchZone oldZone = model.getWatchZone();
                    boolean changed = oldZone.getCenterLatitude() != watchZone.getCenterLatitude() ||
                            oldZone.getCenterLongitude() != watchZone.getCenterLongitude() ||
                            oldZone.getRadius() != watchZone.getRadius();
                    if (changed) {
                        container.watchZonePoints.removeObserver(container.pointsObserver);
                        container.watchZonePoints = loadWatchZonePointsFromDb(watchZone);
                        container.watchZonePoints.observeForever(container.pointsObserver);
                    }
                }
            } else {
                WatchZoneContainer container = new WatchZoneContainer();
                container.watchZoneModel = new MutableLiveData<>();
                container.watchZoneModel.observeForever(mWatchZoneModelObserver);
                container.watchZonePoints = loadWatchZonePointsFromDb(watchZone);
                container.pointsObserver = new Observer<List<WatchZonePoint>>() {
                    @Override
                    public void onChanged(@Nullable final List<WatchZonePoint> watchZonePoints) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                invalidateWatchZoneModel(watchZone, watchZonePoints);
                            }
                        });
                    }
                };
                container.watchZonePoints.observeForever(container.pointsObserver);
                mWatchZoneContainerMap.put(watchZone.getUid(), container);
            }

            updateLiveDataList();
        }
    }

    private void updateLiveDataList() {
        List<WatchZoneModel> models = new ArrayList<>();
        for (Long uid : mWatchZoneContainerMap.keySet()) {
            WatchZoneContainer container = mWatchZoneContainerMap.get(uid);
            WatchZoneModel model = container.watchZoneModel.getValue();
            if (model != null) {
                models.add(model);
            }
        }
        mCachedWatchZoneModels.postValue(models);
    }

    private void invalidateWatchZoneModel(WatchZone watchZone, List<WatchZonePoint> watchZonePoints) {
        if (watchZonePoints == null || watchZonePoints.isEmpty()) {
            Log.w(TAG, "INVALID WatchZone Model detected! Uid: " + watchZone.getUid() + "\nIgnoring");
            return;
        }
        WatchZoneModel.WatchZoneStatus status = null;

        boolean isCreated = true;
        boolean isUpToDate = true;

        for (WatchZonePoint point : watchZonePoints) {
            if (point.getAddress() == null) {
                isCreated = false;
                isUpToDate = false;
            } else {
                long timestamp = point.getWatchZoneUpdatedTimestampMs();
                long elapsedTime = System.currentTimeMillis() - timestamp;
                if (elapsedTime > WatchZonePointUpdater.WATCH_ZONE_UP_TO_DATE_TIME_MS) {
                    isUpToDate = false;
                }
            }
        }

        if (!isCreated) {
            status = WatchZoneModel.WatchZoneStatus.INCOMPLETE;
        } else if (!isUpToDate) {
            status = WatchZoneModel.WatchZoneStatus.OUT_OF_DATE;
        } else {
            status = WatchZoneModel.WatchZoneStatus.VALID;


        }

        WatchZoneModel model = new WatchZoneModel(watchZone, watchZonePoints,
                status);
        WatchZoneContainer container = mWatchZoneContainerMap.get(watchZone.getUid());
        if (container != null) {
            container.watchZoneModel.postValue(model);
        }
    }

    private void scheduleUpdateJob() {
        WatchZoneUpdateJob.scheduleAppForegroundJob(mContext);
    }
}
