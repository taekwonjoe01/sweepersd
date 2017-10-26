package com.example.joseph.sweepersd.limit;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
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
    private static MutableLiveData<LimitRepository> sInstance = new MutableLiveData<>();
    private Context mContext;

    private final LiveData<List<Limit>> mCachedPostedLimitsLiveData;
    private final Map<Long, LiveData<List<LimitSchedule>>> mCachedLimitSchedulesLiveData;
    private final Map<Long, LiveData<Limit>> mCachedLimitsLiveData;

    private final BooleanPreferenceLiveData mLimitsLoadedPreferenceLiveData;
    private final Observer<Boolean> mLimitsLoadedObserver =
            new Observer<Boolean>() {
        @Override
        public void onChanged(@Nullable Boolean limitsLoaded) {
            if (!limitsLoaded) {
                Intent msgIntent = new Intent(mContext, OnDeviceLimitProviderService.class);
                mContext.startService(msgIntent);
            }
        }
    };
    private final BooleanPreferenceLiveData mLimitsValidatedPrefrenceLiveData;
    private final Observer<Boolean> mLimitsValidatedObserver =
            new Observer<Boolean>() {
        @Override
        public void onChanged(@Nullable Boolean limitsValidated) {
            Boolean limitsLoaded = mLimitsLoadedPreferenceLiveData.getValue();
            if (limitsLoaded != null && limitsLoaded) {
                if (!limitsValidated) {
                    AddressValidatorJob.scheduleJob(mContext);
                }
                AddressValidatorJob.scheduleMonthlyJob(mContext);
            }
        }
    };

    private LimitRepository(Context context) {
        mContext = context;
        mCachedPostedLimitsLiveData = loadPostedLimitsLiveDataFromDb();
        mCachedLimitSchedulesLiveData = new HashMap<>();
        mCachedLimitsLiveData = new HashMap<>();
        mLimitsLoadedPreferenceLiveData = new BooleanPreferenceLiveData(mContext, Preferences.PREFERENCE_ON_DEVICE_LIMITS_LOADED);
        mLimitsLoadedPreferenceLiveData.observeForever(mLimitsLoadedObserver);
        mLimitsValidatedPrefrenceLiveData = new BooleanPreferenceLiveData(mContext, Preferences.PREFERENCE_ON_DEVICE_LIMITS_VALIDATED);
        mLimitsValidatedPrefrenceLiveData.observeForever(mLimitsValidatedObserver);
    }

    public synchronized static LimitRepository getInstance(Context context) {
        if (sInstance.getValue() == null) {
            sInstance.setValue(new LimitRepository(context));
        }
        return sInstance.getValue();
    }

    public static LiveData<LimitRepository> getInstanceLiveData() {
        return sInstance;
    }

    public synchronized void delete() {
        if (sInstance.getValue() != null) {
            mLimitsLoadedPreferenceLiveData.removeObserver(mLimitsLoadedObserver);
            mLimitsValidatedPrefrenceLiveData.removeObserver(mLimitsValidatedObserver);
            sInstance.setValue(null);
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
