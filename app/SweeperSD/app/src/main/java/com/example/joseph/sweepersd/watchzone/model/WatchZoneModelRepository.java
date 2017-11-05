package com.example.joseph.sweepersd.watchzone.model;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
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

public class WatchZoneModelRepository {
    private static final String TAG = WatchZoneModelRepository.class.getSimpleName();

    private static WatchZoneModelRepository sInstance;

    private final Context mApplicationContext;
    private final Handler mHandler;
    private final HandlerThread mThread;

    private final Map<Long, LiveData<ZoneModel>> mWatchZoneModelsMap;
    private final LiveData<List<ZoneModel>> mCachedWatchZones;

    private WatchZoneModelRepository(Context context) {
        mApplicationContext = context.getApplicationContext();
        mThread = new HandlerThread("WatchZoneModelRepositoryUpdateThread");
        mThread.start();
        mHandler = new Handler(/*mThread.getLooper()*/Looper.getMainLooper());

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

                scheduleUpdateJob();
            }
        }
        long endTime = SystemClock.elapsedRealtime();
        Log.d(TAG, "create Watch Zone with radius " + radius + " took "
                + (endTime - startTime) + "ms.");
        return result;
    }

    public LiveData<List<ZoneModel>> getZoneModelsLiveData() {
        return mCachedWatchZones;
    }

    public LiveData<ZoneModel> getZoneModelForUid(long uid) {
        if (!mWatchZoneModelsMap.containsKey(uid)) {
            LiveData<ZoneModel> model = loadWatchZoneLiveDataFromDb(uid);
            if (model == null) {
                Log.e("Joey", "It's null???");
            }
            mWatchZoneModelsMap.put(uid, model);
        }

        return mWatchZoneModelsMap.get(uid);
    }

    private LiveData<List<ZoneModel>> loadWatchZonesLiveDataFromDb() {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mApplicationContext).watchZoneDao();

        return watchZoneDao.getAllZonesLiveData();
    }

    private LiveData<ZoneModel> loadWatchZoneLiveDataFromDb(long uid) {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mApplicationContext).watchZoneDao();

        return watchZoneDao.getZoneLiveDataForUid(uid);
    }

    private void scheduleUpdateJob() {
        WatchZoneUpdateJob.scheduleAppForegroundJob(mApplicationContext);
    }
}
