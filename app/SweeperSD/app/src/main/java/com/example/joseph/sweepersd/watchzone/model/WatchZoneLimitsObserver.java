package com.example.joseph.sweepersd.watchzone.model;

import com.example.joseph.sweepersd.limit.LimitModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WatchZoneLimitsObserver extends WatchZoneBaseObserver<Map<Long, LimitModel>, ZoneModel> {
    final Long mWatchZoneUid;
    private final WatchZoneLimitsChangedCallback mCallback;

    protected Map<Long, LimitModel> mLimitModels;

    public interface WatchZoneLimitsChangedCallback extends WatchZoneBaseObserverCallback<Map<Long, LimitModel>> {
        void onLimitsChanged(Map<Long, LimitModel> data, ChangeSet changeSet);
    }

    public WatchZoneLimitsObserver(Long watchZoneUid, WatchZoneLimitsChangedCallback callback) {
        super(callback);
        mWatchZoneUid = watchZoneUid;
        mCallback = callback;
    }

    @Override
    boolean isValid(ZoneModel zoneModel) {
        return zoneModel != null;
    }

    @Override
    Map<Long, LimitModel> getData(ZoneModel zoneModel) {
        HashMap<Long, LimitModel> results = new HashMap<>();
        for (PointModel point : zoneModel.points) {
            List<LimitModel> models = point.limitModels;
            if (models != null && !models.isEmpty()) {
                for (LimitModel model : models) {
                    if (!results.containsKey(model.limit.getUid())) {
                        results.put(model.limit.getUid(), model);
                    }
                }
            }
        }
        if (mLimitModels == null) {
            mLimitModels = results;
        }
        return results;
    }

    @Override
    void onPossibleChangeDetected(Map<Long, LimitModel> limitModels) {
        ChangeSet changeSet = new ChangeSet();
        changeSet.addedLimits = new ArrayList<>();
        changeSet.changedLimits = new ArrayList<>();
        changeSet.removedLimits = new ArrayList<>(mLimitModels.keySet());
        for (Long uid : limitModels.keySet()) {
            changeSet.removedLimits.remove(uid);
            if (!mLimitModels.containsKey(uid)) {
                changeSet.addedLimits.add(uid);
            } else {
                LimitModel curModel = mLimitModels.get(uid);
                LimitModel newModel = limitModels.get(uid);
                if (curModel.isChanged(newModel)) {
                    changeSet.changedLimits.add(uid);
                }
            }
        }
        mLimitModels = limitModels;

        mCallback.onLimitsChanged(mLimitModels, changeSet);
    }

    public Map<Long, LimitModel> getLimitModels() {
        return mLimitModels;
    }
}
