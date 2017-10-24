package com.example.joseph.sweepersd.watchzone.model;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WatchZonePointsModel extends LiveData<WatchZonePointsModel> {
    private final Context mApplicationContext;
    private final Handler mHandler;
    private final Long mWatchZoneUid;

    private Map<Long, WatchZonePoint> mWatchZonePointsMap;

    private WatchZoneModel.ModelStatus mStatus;

    private Observer<List<WatchZonePoint>> mDatabaseObserver = new Observer<List<WatchZonePoint>>() {
        @Override
        public void onChanged(@Nullable final List<WatchZonePoint> watchZonePoints) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (WatchZonePointsModel.this) {
                        if (watchZonePoints == null) {
                            // Invalid values for this LiveData. Notify observers that this data is invalid.
                            mStatus = WatchZoneModel.ModelStatus.INVALID;
                            postValue(WatchZonePointsModel.this);
                        } else {
                            if (mWatchZonePointsMap == null) {
                                mWatchZonePointsMap = new HashMap<>();
                            }
                            mWatchZonePointsMap.clear();
                            for (WatchZonePoint p : watchZonePoints) {
                                mWatchZonePointsMap.put(p.getUid(), p);
                            }

                            mStatus = WatchZoneModel.ModelStatus.LOADED;
                            postValue(WatchZonePointsModel.this);
                        }
                    }
                }
            });
        }
    };

    public WatchZonePointsModel(Context context, Handler handler, Long watchZoneUid) {
        mApplicationContext = context.getApplicationContext();
        mHandler = handler;
        mWatchZoneUid = watchZoneUid;

        mStatus = WatchZoneModel.ModelStatus.LOADING;
        setValue(this);
    }

    public synchronized Long getWatchZoneUid() {
        return mWatchZoneUid;
    }

    public synchronized WatchZonePoint getWatchZonePointForWatchZonePointUid(Long watchZonePointUid) {
        return getStatus() == WatchZoneModel.ModelStatus.INVALID ? null : mWatchZonePointsMap.get(watchZonePointUid);
    }

    public synchronized Map<Long, WatchZonePoint> getWatchZonePointsMap() {
        return getStatus() == WatchZoneModel.ModelStatus.INVALID ? null : mWatchZonePointsMap;
    }

    public synchronized WatchZoneModel.ModelStatus getStatus() {
        return mStatus;
    }

    public synchronized boolean isChanged(WatchZonePointsModel compareTo) {
        boolean result = false;

        if (this.mWatchZoneUid == compareTo.getWatchZoneUid()) {
            if (this.getWatchZonePointsMap() == null && compareTo.getWatchZonePointsMap() == null) {
                return false;
            } else if (this.getWatchZonePointsMap() == null && compareTo.getWatchZonePointsMap() != null) {
                return true;
            } else if (this.getWatchZonePointsMap() != null && compareTo.getWatchZonePointsMap() == null) {
                return true;
            } else if (this.getWatchZonePointsMap().size() != compareTo.getWatchZonePointsMap().size()) {
                return true;
            } else {
                Map<Long, WatchZonePoint> otherList = compareTo.getWatchZonePointsMap();
                for (Long uid : mWatchZonePointsMap.keySet()) {
                    if (!otherList.containsKey(uid)) {
                        return true;
                    }
                }
                for (Long uid : otherList.keySet()) {
                    if (!mWatchZonePointsMap.containsKey(uid)) {
                        return true;
                    }
                }
                for (Long uid : mWatchZonePointsMap.keySet()) {
                    WatchZonePoint mP = mWatchZonePointsMap.get(uid);
                    WatchZonePoint p = otherList.get(uid);
                    if (mP.isChanged(p)) {
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
        WatchZoneRepository.getInstance(mApplicationContext).getWatchZonePointsLiveData(mWatchZoneUid)
                .observeForever(mDatabaseObserver);
    }

    @Override
    protected synchronized void onInactive() {
        super.onInactive();
        WatchZoneRepository.getInstance(mApplicationContext).getWatchZonePointsLiveData(mWatchZoneUid)
                .removeObserver(mDatabaseObserver);
    }
}
