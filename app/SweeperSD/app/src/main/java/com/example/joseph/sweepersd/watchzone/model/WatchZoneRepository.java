package com.example.joseph.sweepersd.watchzone.model;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.example.joseph.sweepersd.AppDatabase;
import com.example.joseph.sweepersd.utils.LocationUtils;
import com.example.joseph.sweepersd.watchzone.WatchZoneUpdateJob;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WatchZoneRepository extends LiveData<WatchZoneModel> {
    private static final String TAG = WatchZoneRepository.class.getSimpleName();

    private static MutableLiveData<WatchZoneRepository> sInstance = new MutableLiveData<>();

    private final Context mApplicationContext;
    private final Map<Long, LiveData<WatchZone>> mCachedWatchZoneLiveDataMap;
    private final LiveData<List<WatchZone>> mCachedWatchZoneLiveDataList;

    private WatchZoneRepository(Context context) {
        mApplicationContext = context.getApplicationContext();
        mCachedWatchZoneLiveDataMap = new HashMap<>();

        getWatchZoneUids();

        mCachedWatchZoneLiveDataList = loadWatchZonesLiveDataFromDb();
    }

    public synchronized static WatchZoneRepository getInstance(Context context) {
        if (sInstance.getValue() == null) {
            sInstance.setValue(new WatchZoneRepository(context));
        }
        return sInstance.getValue();
    }

    public static LiveData<WatchZoneRepository> getInstanceLiveData() {
        return sInstance;
    }

    /**
     * Intended to only be called by the Application when memory is needed to be trimmed.
     */
    public synchronized void delete() {
        if (sInstance.getValue() != null) {
            sInstance.setValue(null);
        }
    }

    public synchronized List<Long> getWatchZoneUids() {
        List<Long> watchZoneUids = loadWatchZoneUidsFromDb();
        for (Long uid : watchZoneUids) {
            if (!mCachedWatchZoneLiveDataMap.containsKey(uid)) {
                mCachedWatchZoneLiveDataMap.put(uid, null);
            }
        }
        return new ArrayList<>(mCachedWatchZoneLiveDataMap.keySet());
    }

    public synchronized LiveData<List<WatchZone>> getWatchZonesLiveData() {
        return mCachedWatchZoneLiveDataList;
    }

    public synchronized LiveData<WatchZone> getWatchZoneLiveData(Long watchZoneUid) {
        if (!mCachedWatchZoneLiveDataMap.containsKey(watchZoneUid) ||
                mCachedWatchZoneLiveDataMap.get(watchZoneUid) == null) {
            mCachedWatchZoneLiveDataMap.put(watchZoneUid, loadWatchZoneLiveDataFromDb(watchZoneUid));
        }

        return mCachedWatchZoneLiveDataMap.get(watchZoneUid);
    }

    public synchronized void triggerRefreshAll() {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mApplicationContext).watchZoneDao();

        List<WatchZone> zones = watchZoneDao.getAllWatchZones();
        for (WatchZone zone : zones) {
            updateWatchZone(zone.getUid(), zone.getLabel(), zone.getCenterLatitude(),
                    zone.getCenterLongitude(), zone.getRadius());
        }
    }

    public synchronized void triggerRefresh(Long uid) {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mApplicationContext).watchZoneDao();
        WatchZone zone = watchZoneDao.getWatchZone(uid);
        if (zone != null) {
            updateWatchZone(zone.getUid(), zone.getLabel(), zone.getCenterLatitude(),
                    zone.getCenterLongitude(), zone.getRadius());
        }
    }

    public synchronized int updateWatchZone(Long watchZoneUid, String label,
                                            double centerLatitude, double centerLongitude,
                                            int radius) {
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

                scheduleUpdateJob();
            }
        }
        long endTime = SystemClock.elapsedRealtime();
        Log.d(TAG, "create Watch Zone with radius " + radius + " took "
                + (endTime - startTime) + "ms.");
        return result;
    }

    public synchronized int deleteWatchZone(Long uid) {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mApplicationContext).watchZoneDao();

        WatchZone watchZone = watchZoneDao.getWatchZone(uid);
        int result = 0;
        if (watchZone != null) {
            result = watchZoneDao.deleteWatchZone(watchZone);
            Log.d("Joey", "deleteWatchZone result " + result);
        }

        return result;
    }

    public synchronized LiveData<List<WatchZonePoint>> getWatchZonePointsLiveData(Long watchZoneUid) {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mApplicationContext).watchZoneDao();

        return watchZoneDao.getWatchZonePointsLiveData(watchZoneUid);
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

    private LiveData<List<WatchZone>> loadWatchZonesLiveDataFromDb() {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mApplicationContext).watchZoneDao();

        return watchZoneDao.getAllWatchZonesLiveData();
    }

    private LiveData<WatchZone> loadWatchZoneLiveDataFromDb(Long watchZoneUid) {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mApplicationContext).watchZoneDao();

        return watchZoneDao.getWatchZoneLiveData(watchZoneUid);
    }

    private LiveData<List<WatchZonePoint>> loadWatchZonePointsLiveDataFromDb(WatchZone watchZone) {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mApplicationContext).watchZoneDao();

        return watchZoneDao.getWatchZonePointsLiveData(watchZone.getUid());
    }

    private List<Long> loadWatchZoneUidsFromDb() {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mApplicationContext).watchZoneDao();

        return watchZoneDao.getWatchZoneUids();
    }

    private void scheduleUpdateJob() {
        WatchZoneUpdateJob.scheduleAppForegroundJob(mApplicationContext);
    }
}
