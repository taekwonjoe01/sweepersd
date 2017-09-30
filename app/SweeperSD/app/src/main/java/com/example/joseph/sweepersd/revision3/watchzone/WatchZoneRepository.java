package com.example.joseph.sweepersd.revision3.watchzone;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.example.joseph.sweepersd.revision3.AppDatabase;
import com.example.joseph.sweepersd.revision3.LocationUtils;
import com.google.android.gms.maps.model.LatLng;

import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WatchZoneRepository {
    private static final String TAG = WatchZoneRepository.class.getSimpleName();
    public static final int WATCH_ZONE_UPDATE_JOB_BASE_ID = 1000;

    private static WatchZoneRepository sInstance;
    private Context mContext;

    private final LiveData<List<WatchZone>> mCachedWatchZones;
    private final Map<Long, LiveData<List<WatchZonePoint>>> mCachedWatchZonePoints;

    private WatchZoneRepository(Context context) {
        mContext = context;
        mCachedWatchZones = loadWatchZonesFromDb();
        mCachedWatchZonePoints = new HashMap<>();
    }

    public synchronized static WatchZoneRepository getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new WatchZoneRepository(context);
        }
        return sInstance;
    }

    public synchronized LiveData<List<WatchZone>> getWatchZonesLiveData() {
        return mCachedWatchZones;
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

    public synchronized LiveData<List<WatchZonePoint>> getWatchZonePointsLiveData(WatchZone watchZone) {
        LiveData<List<WatchZonePoint>> results = null;

        if (!mCachedWatchZonePoints.containsKey(watchZone.getUid())) {
            mCachedWatchZonePoints.put(watchZone.getUid(), loadWatchZonePointsFromDb(watchZone));
        }
        results = mCachedWatchZonePoints.get(watchZone.getUid());

        return results;
    }

    public synchronized List<WatchZonePoint> getWatchZonePoints(WatchZone watchZone) {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mContext).watchZoneDao();

        return watchZoneDao.getWatchZonePoints(watchZone.getUid());
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

                Intent msgIntent = new Intent(mContext, WatchZoneUpdateService.class);
                msgIntent.putExtra(WatchZoneUpdateService.PARAM_WATCH_ZONE_ID, result /*uid*/);
                msgIntent.putExtra(WatchZoneUpdateService.PARAM_INTENT_TRIGGER_TIME, System.currentTimeMillis());
                msgIntent.putExtra(WatchZoneUpdateService.PARAM_FULL_REFRESH, true);
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

            Intent msgIntent = new Intent(mContext, WatchZoneUpdateService.class);
            msgIntent.putExtra(WatchZoneUpdateService.PARAM_WATCH_ZONE_ID, watchZone.getUid());
            msgIntent.putExtra(WatchZoneUpdateService.PARAM_INTENT_TRIGGER_TIME, System.currentTimeMillis());
            msgIntent.putExtra(WatchZoneUpdateService.PARAM_FULL_REFRESH, true);
            mContext.startService(msgIntent);


            JobScheduler jobScheduler =
                    (JobScheduler) mContext.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            JobInfo.Builder jobInfo = new JobInfo.Builder(1, WatchZoneUpdateTask.class);
            PersistableBundle bundle = new PersistableBundle();
            bundle.putLong(WatchZoneUpdateTask.PARAM_WATCH_ZONE_ID, watchZone.getUid());
            jobInfo.setExtras(bundle);
            int jobId =  WATCH_ZONE_UPDATE_JOB_BASE_ID + watchZone.getUid();
            jobScheduler.schedule(new JobInfo.Builder(LOAD_ARTWORK_JOB_ID,
                    new ComponentName(this, DownloadArtworkJobService.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .build());
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
}
