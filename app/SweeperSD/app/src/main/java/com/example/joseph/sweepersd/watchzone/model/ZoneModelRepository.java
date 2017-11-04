package com.example.joseph.sweepersd.watchzone.model;

import android.arch.lifecycle.LiveData;
import android.content.Context;

import com.example.joseph.sweepersd.AppDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZoneModelRepository {
    private static final String TAG = ZoneModelRepository.class.getSimpleName();

    private static ZoneModelRepository sInstance;

    private final Context mApplicationContext;
    private final LiveData<List<ZoneModel>> mCachedWatchZones;
    private final Map<Long, LiveData<ZoneModel>> mCachedLiveData;

    private ZoneModelRepository(Context context) {
        mApplicationContext = context.getApplicationContext();
        mCachedWatchZones = loadWatchZonesLiveDataFromDb();
        mCachedLiveData = new HashMap<>();
    }

    public synchronized static ZoneModelRepository getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ZoneModelRepository(context);
        }
        return sInstance;
    }

    public static ZoneModelRepository getInstance() {
        return sInstance;
    }

    public LiveData<List<ZoneModel>> getZoneModelsLiveData() {
        return mCachedWatchZones;
    }

    public LiveData<ZoneModel> getZoneModelForUid(long uid) {
        if (!mCachedLiveData.containsKey(uid)) {
            mCachedLiveData.put(uid, loadWatchZoneLiveDataFromDb(uid));
        }

        return mCachedLiveData.get(uid);
    }

    private LiveData<List<ZoneModel>> loadWatchZonesLiveDataFromDb() {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mApplicationContext).watchZoneDao();

        return watchZoneDao.getAllZonesLiveData();
    }

    private LiveData<ZoneModel> loadWatchZoneLiveDataFromDb(long uid) {
        WatchZoneDao watchZoneDao = AppDatabase.getInstance(mApplicationContext).watchZoneDao();

        return watchZoneDao.getZoneLiveDataForUid(uid);
    }
}
