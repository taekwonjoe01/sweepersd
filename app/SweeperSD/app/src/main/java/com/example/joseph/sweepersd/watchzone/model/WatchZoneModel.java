package com.example.joseph.sweepersd.watchzone.model;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class WatchZoneModel extends LiveData<WatchZoneModel> {
    private final Context mApplicationContext;
    private final Handler mHandler;
    private final Long mWatchZoneUid;
    private final WatchZonePointsModel mWatchZonePointsModel;
    private final Map<Long, WatchZoneLimitModel> mWatchZoneLimitModelMap;

    private WatchZone mWatchZone;
    private Status mStatus;

    public enum Status {
        LOADING,
        INVALID_NO_WATCH_ZONE,
        INVALID_NO_WATCH_ZONE_POINTS,
        INVALID_LIMIT,
        NOT_CREATED,
        OUT_OF_DATE,
        VALID
    }

    enum ModelStatus {
        LOADING,
        INVALID,
        LOADED
    }

    private Observer<WatchZone> mWatchZoneDatabaseObserver = new Observer<WatchZone>() {
        @Override
        public void onChanged(@Nullable final WatchZone watchZone) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (WatchZoneModel.this) {
                        if (watchZone == null) {
                            // Invalid value for this LiveData. Notify observers by setting self to
                            // null.
                            mStatus = Status.INVALID_NO_WATCH_ZONE;
                            postValue(WatchZoneModel.this);
                        } else {
                            boolean changed = mWatchZone == null || mWatchZone.isChanged(watchZone);
                            if (changed) {
                                clearWatchZonePointsObservers();
                                mWatchZone = watchZone;
                                mWatchZonePointsModel.observeForever(mWatchZonePointsObserver);
                            }
                        }
                    }
                }
            });
        }
    };

    private Observer<WatchZonePointsModel> mWatchZonePointsObserver = new Observer<WatchZonePointsModel>() {
        @Override
        public void onChanged(@Nullable final WatchZonePointsModel watchZonePointsModel) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (WatchZoneModel.this) {
                        ModelStatus modelStatus = watchZonePointsModel.getStatus();
                        if (modelStatus == ModelStatus.INVALID) {
                            // Invalid value for this LiveData. Notify observers by setting self to
                            // null.
                            mStatus = Status.INVALID_NO_WATCH_ZONE_POINTS;
                            postValue(WatchZoneModel.this);
                        } else if (modelStatus == ModelStatus.LOADED) {
                            updateWatchZoneLimitModels();
                            checkModelLoadingComplete();
                        }
                    }
                }
            });
        }
    };

    private synchronized void checkModelLoadingComplete() {
        if (!isWatchZoneCreated()) {
            mStatus = Status.NOT_CREATED;
            postValue(WatchZoneModel.this);
        } else {
            if (mWatchZoneLimitModelMap.isEmpty()) {
                mStatus = Status.VALID;
                postValue(WatchZoneModel.this);
            } else {
                boolean done = true;
                for (Long limitUid : mWatchZoneLimitModelMap.keySet()) {
                    WatchZoneLimitModel limitModel = mWatchZoneLimitModelMap.get(limitUid);
                    if (limitModel.getStatus() == ModelStatus.INVALID) {
                        mStatus = Status.INVALID_LIMIT;
                        postValue(WatchZoneModel.this);
                        done = false;
                        break;
                    } else if (limitModel.getStatus() == ModelStatus.LOADING) {
                        done = false;
                    }
                }
                if (done) {
                    if (needsUpdate()) {
                        mStatus = Status.OUT_OF_DATE;
                    } else {
                        mStatus = Status.VALID;
                    }
                    postValue(WatchZoneModel.this);
                }
            }
        }
    }

    private Observer<WatchZoneLimitModel> mWatchZoneLimitModelObserver = new Observer<WatchZoneLimitModel>() {
        @Override
        public void onChanged(@Nullable final WatchZoneLimitModel watchZoneLimitModel) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (WatchZoneModel.this) {
                        checkModelLoadingComplete();
                    }
                }
            });
        }
    };

    public WatchZoneModel(Context context, Handler handler, Long watchZoneUid) {
        mApplicationContext = context.getApplicationContext();
        mHandler = handler;
        mWatchZoneUid = watchZoneUid;
        mWatchZonePointsModel = new WatchZonePointsModel(mApplicationContext, mHandler, mWatchZoneUid);
        mWatchZoneLimitModelMap = new HashMap<>();
        mStatus = Status.LOADING;
        setValue(this);
    }

    public synchronized WatchZone getWatchZone() {
        return mWatchZone;
    }

    public synchronized WatchZonePointsModel getWatchZonePointsModel() {
        return mWatchZonePointsModel.getValue();
    }

    public synchronized Long getWatchZoneUid() {
        return mWatchZoneUid;
    }

    public synchronized Set<Long> getWatchZoneLimitModelUids() {
        return mWatchZoneLimitModelMap.keySet();
    }

    public synchronized WatchZoneLimitModel getWatchZoneLimitModel(Long limitUid) {
        return mWatchZoneLimitModelMap.get(limitUid).getValue();
    }

    public synchronized Status getStatus() {
        return mStatus;
    }

    public synchronized boolean isChanged(WatchZoneModel compareTo) {
        boolean result = false;

        if (this.mWatchZoneUid == compareTo.getWatchZoneUid()) {
            if (this.mStatus != compareTo.getStatus()) {
                result = true;
            } else if (this.mWatchZone == null && compareTo.getWatchZone() != null) {
                result = true;
            } else if (this.mWatchZone != null && compareTo.getWatchZone() == null) {
                result = true;
            } else if (this.mWatchZone == null && compareTo.getWatchZone() == null) {
                return false;
            } else if (this.mWatchZone.isChanged(compareTo.getWatchZone())) {
                result = true;
            } else if (mWatchZonePointsModel.isChanged(compareTo.getWatchZonePointsModel())) {
                result = true;
            } else if (this.getWatchZoneLimitModelUids().size() != compareTo.getWatchZoneLimitModelUids().size()) {
                result = true;
            } else {
                Set<Long> otherSet = compareTo.getWatchZoneLimitModelUids();
                for (Long limitUid : getWatchZoneLimitModelUids()) {
                    if (!otherSet.contains(limitUid)) {
                        result = true;
                        break;
                    }
                }
                if (!result) {
                    for (Long limitUid : getWatchZoneLimitModelUids()) {
                        WatchZoneLimitModel model = compareTo.getWatchZoneLimitModel(limitUid);
                        if (model.isChanged(getWatchZoneLimitModel(limitUid))) {
                            result = true;
                            break;
                        }
                    }
                }
            }
        }

        return result;
    }

    @Override
    protected synchronized void onActive() {
        super.onActive();
        WatchZoneRepository.getInstance(mApplicationContext).getWatchZoneLiveData(mWatchZoneUid)
                .observeForever(mWatchZoneDatabaseObserver);
        if (mWatchZone != null) {
            mWatchZonePointsModel.observeForever(mWatchZonePointsObserver);
            if (mWatchZonePointsModel.getStatus() != ModelStatus.INVALID) {
                updateWatchZoneLimitModels();
            }
        }
    }

    @Override
    protected synchronized void onInactive() {
        super.onInactive();
        WatchZoneRepository.getInstance(mApplicationContext).getWatchZoneLiveData(mWatchZoneUid)
                .removeObserver(mWatchZoneDatabaseObserver);
        clearWatchZonePointsObservers();
    }

    private void clearWatchZonePointsObservers() {
        mWatchZonePointsModel.removeObserver(mWatchZonePointsObserver);
        for (Long uid : mWatchZoneLimitModelMap.keySet()) {
            mWatchZoneLimitModelMap.get(uid).removeObserver(mWatchZoneLimitModelObserver);
        }
    }

    private boolean isWatchZoneCreated() {
        for (WatchZonePoint point : mWatchZonePointsModel.getValue().getWatchZonePointsList()) {
            if (point.getAddress() == null) {
                return false;
            }
        }
        return true;
    }

    private boolean needsUpdate() {
        boolean result = false;
        for (WatchZonePoint point : mWatchZonePointsModel.getValue().getWatchZonePointsList()) {
            long timestamp = point.getWatchZoneUpdatedTimestampMs();
            long elapsedTime = System.currentTimeMillis() - timestamp;
            if (elapsedTime > WatchZonePointUpdater.WATCH_ZONE_UP_TO_DATE_TIME_MS) {
                result = true;
            }
        }
        return result;
    }

    private void updateWatchZoneLimitModels() {
        // Get the unique LimitUid's
        List<Long> uniqueLimitUids = new ArrayList<>();
        for (WatchZonePoint p : mWatchZonePointsModel.getValue().getWatchZonePointsList()) {
            if (!uniqueLimitUids.contains(p.getLimitId()) &&
                    p.getLimitId() != 0L) {
                uniqueLimitUids.add(p.getLimitId());
            }
        }
        Set<Long> existingLimitUids = mWatchZoneLimitModelMap.keySet();
        List<Long> limitsToRemove = new ArrayList<>();
        for (Long existingUid : existingLimitUids) {
            if (!uniqueLimitUids.contains(existingUid)) {
                limitsToRemove.add(existingUid);
            }
        }

        for (Long toRemoveUid : limitsToRemove) {
            WatchZoneLimitModel model = mWatchZoneLimitModelMap.get(toRemoveUid);
            model.removeObserver(mWatchZoneLimitModelObserver);
            mWatchZoneLimitModelMap.remove(toRemoveUid);
        }

        for (Long limitId : uniqueLimitUids) {
            if (!mWatchZoneLimitModelMap.containsKey(limitId)) {
                WatchZoneLimitModel model = new WatchZoneLimitModel(
                        mApplicationContext, mHandler, limitId);
                mWatchZoneLimitModelMap.put(limitId, model);
                model.observeForever(mWatchZoneLimitModelObserver);
            }
        }
    }
}
