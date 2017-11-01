package com.example.joseph.sweepersd.watchzone.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WatchZoneLimitsObserver extends WatchZoneBaseObserver<Map<Long, WatchZoneLimitModel>> {
    final Long mWatchZoneUid;
    private final WatchZoneLimitsChangedCallback mCallback;

    protected Map<Long, WatchZoneLimitModel> mLimitModels;

    public interface WatchZoneLimitsChangedCallback extends WatchZoneBaseObserverCallback<Map<Long, WatchZoneLimitModel>> {
        void onLimitsChanged(Map<Long, WatchZoneLimitModel> data, ChangeSet changeSet);
    }

    public WatchZoneLimitsObserver(Long watchZoneUid, WatchZoneLimitsChangedCallback callback) {
        super(callback);
        mWatchZoneUid = watchZoneUid;
        mCallback = callback;
    }

    @Override
    boolean isValid(WatchZoneModelRepository watchZoneModelRepository) {
        return watchZoneModelRepository.watchZoneExists(mWatchZoneUid);
    }

    @Override
    Map<Long, WatchZoneLimitModel> getDataFromRepo(WatchZoneModelRepository watchZoneModelRepository) {
        WatchZoneModel model = watchZoneModelRepository.getWatchZoneModel(mWatchZoneUid);
        if (model != null) {
            Map<Long, WatchZoneLimitModel> limitModels = new HashMap<>(model.getWatchZoneLimitModelMap());
            boolean ready = true;
            for (Long uid : limitModels.keySet()) {
                WatchZoneLimitModel limitModel = limitModels.get(uid);
                if (limitModel == null) {
                    ready = false;
                    break;

                } else {
                    if (limitModel.getLimitSchedulesModel() == null ||
                            limitModel.getLimitSchedulesModel().getScheduleMap() == null) {
                        ready = false;
                        break;
                    }
                }
            }
            if (ready) {
                if (mLimitModels == null) {
                    mLimitModels = limitModels;
                }
                return limitModels;
            } else if (mLimitModels != null) {
                return mLimitModels;
            }
        }
        return null;
    }

    @Override
    void onRepositoryChanged(final Map<Long, WatchZoneLimitModel> limitModels) {
        ChangeSet changeSet = new ChangeSet();
        changeSet.addedLimits = new ArrayList<>();
        changeSet.changedLimits = new ArrayList<>();
        changeSet.removedLimits = new ArrayList<>(mLimitModels.keySet());
        for (Long uid : limitModels.keySet()) {
            changeSet.removedLimits.remove(uid);
            if (!mLimitModels.containsKey(uid)) {
                changeSet.addedLimits.add(uid);
            } else {
                WatchZoneLimitModel curModel = mLimitModels.get(uid);
                WatchZoneLimitModel newModel = limitModels.get(uid);
                if (curModel.isChanged(newModel)) {
                    changeSet.changedLimits.add(uid);
                }
            }
        }
        mLimitModels = limitModels;

        mCallback.onLimitsChanged(mLimitModels, changeSet);
    }

    public Map<Long, WatchZoneLimitModel> getLimitModels() {
        return mLimitModels;
    }
}
