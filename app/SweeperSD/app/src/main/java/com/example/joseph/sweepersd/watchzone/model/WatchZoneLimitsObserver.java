package com.example.joseph.sweepersd.watchzone.model;

import com.example.joseph.sweepersd.limit.LimitModel;
import com.example.joseph.sweepersd.utils.BaseObserver;
import com.example.joseph.sweepersd.utils.ChangeSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WatchZoneLimitsObserver extends BaseObserver<Map<Long, LimitModel>, WatchZoneModel> {
    final Long mWatchZoneUid;
    private final WatchZoneLimitsChangedCallback mCallback;

    protected Map<Long, LimitModel> mLimitModels;

    public interface WatchZoneLimitsChangedCallback extends BaseObserverCallback<Map<Long, LimitModel>> {
        void onLimitsChanged(Map<Long, LimitModel> data, ChangeSet changeSet);
    }

    public WatchZoneLimitsObserver(Long watchZoneUid, WatchZoneLimitsChangedCallback callback) {
        super(callback);
        mWatchZoneUid = watchZoneUid;
        mCallback = callback;
    }

    @Override
    public boolean isValid(WatchZoneModel watchZoneModel) {
        return watchZoneModel != null;
    }

    @Override
    public Map<Long, LimitModel> getData(WatchZoneModel watchZoneModel) {
        HashMap<Long, LimitModel> results = new HashMap<>();
        for (WatchZonePointModel point : watchZoneModel.points) {
            List<WatchZonePointLimitModel> wzpModels = point.pointLimitModels;
            if (wzpModels != null && !wzpModels.isEmpty()) {
                for (WatchZonePointLimitModel wzpModel : wzpModels) {
                    List<LimitModel> models = wzpModel.limitModels;
                    if (models != null && !models.isEmpty()) {
                        for (LimitModel model : models) {
                            if (!results.containsKey(model.limit.getUid())) {
                                results.put(model.limit.getUid(), model);
                            }
                        }
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
    public void onPossibleChangeDetected(Map<Long, LimitModel> limitModels) {
        ChangeSet changeSet = new ChangeSet();
        changeSet.addedUids = new ArrayList<>();
        changeSet.changedUids = new ArrayList<>();
        changeSet.removedUids = new ArrayList<>(mLimitModels.keySet());
        for (Long uid : limitModels.keySet()) {
            changeSet.removedUids.remove(uid);
            if (!mLimitModels.containsKey(uid)) {
                changeSet.addedUids.add(uid);
            } else {
                LimitModel curModel = mLimitModels.get(uid);
                LimitModel newModel = limitModels.get(uid);
                if (curModel.isChanged(newModel)) {
                    changeSet.changedUids.add(uid);
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
