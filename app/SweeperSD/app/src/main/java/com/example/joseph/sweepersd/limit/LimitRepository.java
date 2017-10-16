package com.example.joseph.sweepersd.limit;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.example.joseph.sweepersd.AppDatabase;
import com.example.joseph.sweepersd.utils.BooleanPreferenceLiveData;
import com.example.joseph.sweepersd.utils.Preferences;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LimitRepository {
    private static LimitRepository sInstance;
    private Context mContext;

    private final LiveData<List<Limit>> mCachedPostedLimitsLiveData;
    private final Map<Long, LiveData<List<LimitSchedule>>> mCachedLimitSchedulesLiveData;
    private final Map<Long, LiveData<Limit>> mCachedLimitsLiveData;

    private final BooleanPreferenceLiveData mSharedPreferenceLiveData;
    private final Observer<Boolean> mSharedPreferencesObserver =
            new Observer<Boolean>() {
        @Override
        public void onChanged(@Nullable Boolean limitsLoaded) {
            if (!limitsLoaded) {
                Intent msgIntent = new Intent(mContext, OnDeviceLimitProviderService.class);
                mContext.startService(msgIntent);
            }
        }
    };

    private LimitRepository(Context context) {
        mContext = context;
        mCachedPostedLimitsLiveData = loadPostedLimitsLiveDataFromDb();
        mCachedLimitSchedulesLiveData = new HashMap<>();
        mCachedLimitsLiveData = new HashMap<>();
        mSharedPreferenceLiveData = new BooleanPreferenceLiveData(mContext, Preferences.PREFERENCE_ON_DEVICE_LIMITS_LOADED);
        mSharedPreferenceLiveData.observeForever(mSharedPreferencesObserver);
    }

    public synchronized static LimitRepository getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new LimitRepository(context);
        }
        return sInstance;
    }

    public synchronized void delete() {
        if (sInstance != null) {
            mSharedPreferenceLiveData.removeObserver(mSharedPreferencesObserver);
            sInstance = null;
        }
    }

    public synchronized LiveData<List<Limit>> getPostedLimitsLiveData() {
        return mCachedPostedLimitsLiveData;
    }

    public synchronized LiveData<Limit> getLimitLiveData(Long limitUid) {
        if (!mCachedLimitsLiveData.containsKey(limitUid)) {
            mCachedLimitsLiveData.put(limitUid, loadLimitLiveDataFromDb(limitUid));
        }

        return mCachedLimitsLiveData.get(limitUid);
    }

    public synchronized LiveData<List<LimitSchedule>> getLimitSchedulesLiveData(Long limitUid) {
        if (!mCachedLimitSchedulesLiveData.containsKey(limitUid)) {
            mCachedLimitSchedulesLiveData.put(limitUid, loadLimitSchedulesLiveDataFromDb(limitUid));
        }

        return mCachedLimitSchedulesLiveData.get(limitUid);
    }

    public List<Limit> getLimitsForStreet(String street) {
        LimitDao limitDao = AppDatabase.getInstance(mContext).limitDao();

        return limitDao.getAllByStreet(street);
    }

    private LiveData<List<Limit>> loadPostedLimitsLiveDataFromDb() {
        LimitDao limitDao = AppDatabase.getInstance(mContext).limitDao();

        return limitDao.getAllPostedLimitsLiveData();
    }

    private LiveData<List<LimitSchedule>> loadLimitSchedulesLiveDataFromDb(Long limitUid) {
        LimitDao limitDao = AppDatabase.getInstance(mContext).limitDao();

        return limitDao.getLimitSchedulesLiveData(limitUid);
    }

    private LiveData<Limit> loadLimitLiveDataFromDb(Long limitUid) {
        LimitDao limitDao = AppDatabase.getInstance(mContext).limitDao();

        return limitDao.getLimitLiveData(limitUid);
    }
}