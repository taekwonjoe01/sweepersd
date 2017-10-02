package com.example.joseph.sweepersd.revision3.watchzone;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.example.joseph.sweepersd.revision3.AppDatabase;
import com.example.joseph.sweepersd.revision3.LocationUtils;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WatchZoneRepository {
    private static final String TAG = WatchZoneRepository.class.getSimpleName();
    private static final int WATCH_ZONE_NOT_UPDATING = -1;

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















    public synchronized LiveData<List<WatchZone>> getWatchZonesLiveData() {
        return mWatchZonesLiveData;
    }

    public synchronized List<WatchZone> getWatchZones() {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mContext).watchZoneDao();

        return watchZoneDao.getAllWatchZones();
    }

    public synchronized LiveData<WatchZone> getWatchZoneLiveData(long uid) {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mContext).watchZoneDao();

        return watchZoneDao.getWatchZoneLiveData(uid);
    }

    public synchronized WatchZone getWatchZone(long uid) {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mContext).watchZoneDao();

        return watchZoneDao.getWatchZone(uid);
    }

    public synchronized List<WatchZonePoint> getWatchZonePoints(WatchZone watchZone) {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mContext).watchZoneDao();

        return watchZoneDao.getWatchZonePoints(watchZone.getUid());
    }

    public synchronized WatchZonePoint getWatchZonePoint(long uid) {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mContext).watchZoneDao();

        return watchZoneDao.getWatchZonePoint(uid);
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

                watchZoneDao.insertWatchZonePoints(points);

                Intent msgIntent = new Intent(mContext, WatchZoneUpdateServiceTODO.class);
                msgIntent.putExtra(WatchZoneUpdateServiceTODO.PARAM_WATCH_ZONE_ID, result /*uid*/);
                msgIntent.putExtra(WatchZoneUpdateServiceTODO.PARAM_INTENT_TRIGGER_TIME, System.currentTimeMillis());
                msgIntent.putExtra(WatchZoneUpdateServiceTODO.PARAM_FULL_REFRESH, true);
                mContext.startService(msgIntent);
            }
        }
        long endTime = SystemClock.elapsedRealtime();
        Log.d(TAG, "create Watch Zone with radius " + radius + " took "
                + (endTime - startTime) + "ms.");
        return result;
    }

    public synchronized int updateWatchZone(WatchZone watchZone, String label, double centerLatitude,
                                            double centerLongitude, int radius) {
        boolean invalidateWatchZonePoints = false;
        if (centerLatitude != watchZone.getCenterLatitude()
                || centerLongitude != watchZone.getCenterLongitude()
                || radius != watchZone.getRadius()) {
            invalidateWatchZonePoints = true;
        }
        watchZone.setLabel(label);
        watchZone.setCenterLatitude(centerLatitude);
        watchZone.setCenterLongitude(centerLongitude);
        watchZone.setRadius(radius);
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mContext).watchZoneDao();
        int result = watchZoneDao.updateWatchZone(watchZone);
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

            Intent msgIntent = new Intent(mContext, WatchZoneUpdateServiceTODO.class);
            msgIntent.putExtra(WatchZoneUpdateServiceTODO.PARAM_WATCH_ZONE_ID, watchZone.getUid());
            msgIntent.putExtra(WatchZoneUpdateServiceTODO.PARAM_INTENT_TRIGGER_TIME, System.currentTimeMillis());
            msgIntent.putExtra(WatchZoneUpdateServiceTODO.PARAM_FULL_REFRESH, true);
            mContext.startService(msgIntent);


            /*JobScheduler jobScheduler =
                    (JobScheduler) mContext.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            JobInfo.Builder jobInfo = new JobInfo.Builder(1, WatchZoneUpdateTaskTODO.class);
            PersistableBundle bundle = new PersistableBundle();
            bundle.putLong(WatchZoneUpdateTaskTODO.PARAM_WATCH_ZONE_ID, watchZone.getUid());
            jobInfo.setExtras(bundle);
            int jobId =  WATCH_ZONE_UPDATE_JOB_BASE_ID + watchZone.getUid();
            jobScheduler.schedule(new JobInfo.Builder(LOAD_ARTWORK_JOB_ID,
                    new ComponentName(this, DownloadArtworkJobService.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .build());*/
        }
        return result;
    }

    public synchronized long deleteWatchZone(WatchZone watchZone) {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mContext).watchZoneDao();
        int result = watchZoneDao.deleteWatchZone(watchZone);

        return result;
    }

    synchronized long insertWatchZonePoints(List<WatchZonePoint> watchZonePoints) {
        return 0L;
    }

    synchronized int updateWatchZonePoints(List<WatchZonePoint> watchZonePoints) {
        return 0;
    }

    synchronized long deleteWatchZonePoints(List<WatchZonePoint> watchZonePoints) {
        return 0;
    }

    synchronized int updateWatchZonePoint(WatchZonePoint watchZonePoint) {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mContext).watchZoneDao();
        int result = watchZoneDao.updateWatchZonePoint(watchZonePoint);

        return result;
    }

    synchronized int updateWatchZone(WatchZone watchZone) {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mContext).watchZoneDao();
        int result = watchZoneDao.updateWatchZone(watchZone);

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
                    }
                }
            } else {
                WatchZoneContainer container = new WatchZoneContainer();
                container.watchZoneModel = new MutableLiveData<>();
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
}
