package com.example.joseph.sweepersd.alert.geofence;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import com.example.joseph.sweepersd.AppDatabase;

import java.util.List;

public class WatchZoneFenceRepository {
    private static MutableLiveData<WatchZoneFenceRepository> sInstance = new MutableLiveData<>();
    private final Context mApplicationContext;

    private final LiveData<List<WatchZoneFence>> mCachedFences;

    private WatchZoneFenceRepository(Context context) {
        mApplicationContext = context;
        mCachedFences = loadFencesLiveDataFromDb();
    }

    public synchronized static WatchZoneFenceRepository getInstance(Context context) {
        if (sInstance.getValue() == null) {
            sInstance.setValue(new WatchZoneFenceRepository(context));
        }
        return sInstance.getValue();
    }

    public synchronized LiveData<List<WatchZoneFence>> getFencesLiveData() {
        return mCachedFences;
    }

    private LiveData<List<WatchZoneFence>> loadFencesLiveDataFromDb() {
        WatchZoneFenceDao fenceDao = AppDatabase.getInstance(mApplicationContext).watchZoneFenceDao();

        return fenceDao.getAllGeofencesLiveData();
    }
}
