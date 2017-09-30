package com.example.joseph.sweepersd.revision3.limit;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import com.example.joseph.sweepersd.revision3.AppDatabase;
import com.example.joseph.sweepersd.revision3.Preferences;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LimitRepository {
    private static LimitRepository sInstance;
    private Context mContext;

    private final LiveData<List<Limit>> mCachedLimits;
    private final Map<Long, LiveData<List<LimitSchedule>>> mCachedLimitSchedules;

    private LimitRepository(Context context) {
        mContext = context;
        mCachedLimits = loadLimitsFromDb();
        mCachedLimitSchedules = new HashMap<>();
    }

    public synchronized static LimitRepository getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new LimitRepository(context);
        }
        return sInstance;
    }

    public synchronized LiveData<List<Limit>> getLimits() {
        boolean limitsLoaded = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(
                        Preferences.PREFERENCE_ON_DEVICE_LIMITS_LOADED, false);
        if (!limitsLoaded) {
            Intent msgIntent = new Intent(mContext, OnDeviceLimitProviderService.class);
            mContext.startService(msgIntent);
        }

        return mCachedLimits;
    }

    public synchronized LiveData<List<LimitSchedule>> getLimitSchedules(Limit limit) {
        LiveData<List<LimitSchedule>> results = null;

        if (!mCachedLimitSchedules.containsKey(limit.getUid())) {
            mCachedLimitSchedules.put(limit.getUid(), loadLimitSchedulesFromDb(limit));
        }
        results = mCachedLimitSchedules.get(limit.getUid());

        return results;
    }

    public List<Limit> getLimitsForStreet(String street) {
        LimitDao limitDao = AppDatabase.getInstance(mContext).limitDao();

        return limitDao.getAllByStreet(street);
    }

    private LiveData<List<Limit>> loadLimitsFromDb() {
        LimitDao limitDao = AppDatabase.getInstance(mContext).limitDao();

        return limitDao.getAllLimitsLiveData();
    }

    private LiveData<List<LimitSchedule>> loadLimitSchedulesFromDb(Limit limit) {
        LimitDao limitDao = AppDatabase.getInstance(mContext).limitDao();

        return limitDao.getLimitSchedulesLiveData(limit.getUid());
    }
}
