package com.example.joseph.sweepersd.watchzone.model;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.example.joseph.sweepersd.AppDatabase;
import com.example.joseph.sweepersd.utils.LocationUtils;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WatchZoneModelRepository {
    private static final String TAG = WatchZoneModelRepository.class.getSimpleName();

    private static WatchZoneModelRepository sInstance;

    private final Context mApplicationContext;

    private final Map<Long, LiveData<WatchZoneModel>> mWatchZoneModelsMap;
    private final LiveData<List<WatchZoneModel>> mCachedWatchZones;

    private WatchZoneModelRepository(Context context) {
        mApplicationContext = context.getApplicationContext();

        mWatchZoneModelsMap = new HashMap<>();
        mCachedWatchZones = loadWatchZonesLiveDataFromDb();
    }

    public static synchronized WatchZoneModelRepository getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new WatchZoneModelRepository(context);
        }
        return sInstance;
    }

    public synchronized long createWatchZone(String label, double centerLatitude,
                                             double centerLongitude, int radius) {
        long startTime = SystemClock.elapsedRealtime();
        long result = -1;
        if (!TextUtils.isEmpty(label)) {
            WatchZone watchZone = new WatchZone();
            watchZone.setLabel(label);
            watchZone.setCenterLatitude(centerLatitude);
            watchZone.setCenterLongitude(centerLongitude);
            watchZone.setRadius(radius);
            watchZone.setLastSweepingUpdated(0L);
            watchZone.setRemindRange(WatchZone.REMIND_RANGE_DEFAULT);
            watchZone.setRemindPolicy(WatchZone.REMIND_POLICY_DEFAULT);

            WatchZoneDao watchZoneDao = AppDatabase.getInstance(mApplicationContext).watchZoneDao();
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
            }
        }
        long endTime = SystemClock.elapsedRealtime();
        Log.d(TAG, "create Watch Zone with radius " + radius + " took "
                + (endTime - startTime) + "ms.");
        return result;
    }

    /**
     * Intended for internal package use only.
     * @param watchZonePoint
     * @return
     */
    synchronized int updateWatchZonePoint(WatchZonePoint watchZonePoint) {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mApplicationContext).watchZoneDao();
        int result = watchZoneDao.updateWatchZonePoint(watchZonePoint);

        return result;
    }

    public synchronized int updateWatchZone(Long watchZoneUid, String label,
                                            double centerLatitude, double centerLongitude,
                                            int radius, int remindRange, int remindPolicy) {
        int result = 0;
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mApplicationContext).watchZoneDao();

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
            watchZone.setRemindRange(remindRange);
            watchZone.setRemindPolicy(remindPolicy);
            result = watchZoneDao.updateWatchZone(watchZone);
            if (result > 0 && invalidateWatchZonePoints) {
                List<WatchZonePoint> oldPoints = watchZoneDao.getWatchZonePoints(watchZone.getUid());
                int numDeleted = watchZoneDao.deleteWatchZonePoints(oldPoints);

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
            }
        }

        return result;
    }

    public synchronized int deleteWatchZone(Long uid) {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mApplicationContext).watchZoneDao();

        WatchZone watchZone = watchZoneDao.getWatchZone(uid);
        int result = 0;
        if (watchZone != null) {
            result = watchZoneDao.deleteWatchZone(watchZone);
        }

        return result;
    }

    public LiveData<List<WatchZoneModel>> getZoneModelsLiveData() {
        return mCachedWatchZones;
    }

    public LiveData<WatchZoneModel> getZoneModelForUid(long uid) {
        if (!mWatchZoneModelsMap.containsKey(uid)) {
            LiveData<WatchZoneModel> model = loadWatchZoneLiveDataFromDb(uid);
            mWatchZoneModelsMap.put(uid, model);
        }

        return mWatchZoneModelsMap.get(uid);
    }

    private LiveData<List<WatchZoneModel>> loadWatchZonesLiveDataFromDb() {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mApplicationContext).watchZoneDao();

        return watchZoneDao.getAllZonesLiveData();
    }

    private LiveData<WatchZoneModel> loadWatchZoneLiveDataFromDb(long uid) {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mApplicationContext).watchZoneDao();

        return watchZoneDao.getZoneLiveDataForUid(uid);
    }
}
