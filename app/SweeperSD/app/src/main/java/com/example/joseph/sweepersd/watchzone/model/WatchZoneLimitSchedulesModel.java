package com.example.joseph.sweepersd.watchzone.model;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;

import com.example.joseph.sweepersd.limit.LimitRepository;
import com.example.joseph.sweepersd.limit.LimitSchedule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WatchZoneLimitSchedulesModel extends LiveData<WatchZoneLimitSchedulesModel> {
    private final Context mApplicationContext;
    private final Handler mHandler;
    private final Long mLimitUid;

    private Map<Long, LimitSchedule> mLimitSchedulesMap;

    private WatchZoneModel.ModelStatus mStatus;

    private Observer<List<LimitSchedule>> mDatabaseObserver = new Observer<List<LimitSchedule>>() {
        @Override
        public void onChanged(@Nullable final List<LimitSchedule> limitSchedules) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (WatchZoneLimitSchedulesModel.this) {
                        if (limitSchedules == null) {
                            // Invalid values for this LiveData. Notify observers that this data is invalid.
                            mStatus = WatchZoneModel.ModelStatus.INVALID;
                            postValue(WatchZoneLimitSchedulesModel.this);
                        } else {
                            if (mLimitSchedulesMap == null) {
                                mLimitSchedulesMap = new HashMap<>();
                            }
                            mLimitSchedulesMap.clear();
                            for (LimitSchedule ls : limitSchedules) {
                                mLimitSchedulesMap.put(ls.getUid(), ls);
                            }

                            mStatus = WatchZoneModel.ModelStatus.LOADED;
                            postValue(WatchZoneLimitSchedulesModel.this);
                        }
                    }
                }
            });
        }
    };

    public WatchZoneLimitSchedulesModel(Context context, Handler handler, Long limitUid) {
        mApplicationContext = context.getApplicationContext();
        mHandler = handler;
        mLimitUid = limitUid;

        mStatus = WatchZoneModel.ModelStatus.LOADING;
        setValue(this);
    }

    public synchronized Long getLimitUid() {
        return mLimitUid;
    }

    public synchronized LimitSchedule getLimitScheduleForLimitScheduleUid(Long limitScheduleUid) {
        return getStatus() == WatchZoneModel.ModelStatus.INVALID ? null : mLimitSchedulesMap.get(limitScheduleUid);
    }

    public synchronized Map<Long, LimitSchedule> getScheduleMap() {
        return getStatus() == WatchZoneModel.ModelStatus.INVALID ? null : mLimitSchedulesMap;
    }

    public synchronized WatchZoneModel.ModelStatus getStatus() {
        return mStatus;
    }

    public synchronized boolean isChanged(WatchZoneLimitSchedulesModel compareTo) {
        boolean result = false;

        if (this.mLimitUid == compareTo.getLimitUid()) {
            if (this.getScheduleMap() == null && compareTo.getScheduleMap() != null) {
                return true;
            } else if (this.getScheduleMap() != null && compareTo.getScheduleMap() == null) {
                return true;
            } else if (this.getScheduleMap() != null && compareTo.getScheduleMap() != null &&
                    this.getScheduleMap().size() != compareTo.getScheduleMap().size()) {
                return true;
            } else if (this.getScheduleMap() != null && compareTo.getScheduleMap() != null){
                Map<Long, LimitSchedule> otherList = compareTo.getScheduleMap();
                for (Long uid : mLimitSchedulesMap.keySet()) {
                    if (!otherList.containsKey(uid)) {
                        return true;
                    }
                }
                for (Long uid : otherList.keySet()) {
                    if (!mLimitSchedulesMap.containsKey(uid)) {
                        return true;
                    }
                }
                for (Long uid : mLimitSchedulesMap.keySet()) {
                    LimitSchedule mLs = mLimitSchedulesMap.get(uid);
                    LimitSchedule ls = otherList.get(uid);
                    if (mLs.isChanged(ls)) {
                        return true;
                    }
                }
            }
        }
        return result;
    }

    @Override
    protected synchronized void onActive() {
        super.onActive();
        LimitRepository.getInstance(mApplicationContext).getLimitSchedulesLiveData(mLimitUid)
                .observeForever(mDatabaseObserver);
    }

    @Override
    protected synchronized void onInactive() {
        super.onInactive();
        LimitRepository.getInstance(mApplicationContext).getLimitSchedulesLiveData(mLimitUid)
                .removeObserver(mDatabaseObserver);
    }
}
