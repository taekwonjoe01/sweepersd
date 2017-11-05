package com.example.joseph.sweepersd.watchzone.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WatchZoneModelsObserver extends WatchZoneBaseObserver<Map<Long, ZoneModel>, List<ZoneModel>> {
    private final WatchZoneModelsChangedCallback mCallback;

    protected Map<Long, ZoneModel> mWatchZoneModels;

    public interface WatchZoneModelsChangedCallback extends WatchZoneBaseObserverCallback<Map<Long, ZoneModel>> {
        void onModelsChanged(Map<Long, ZoneModel> data, ChangeSet changeSet);
    }

    public WatchZoneModelsObserver(WatchZoneModelsChangedCallback callback) {
        super(callback);
        mCallback = callback;
    }

    @Override
    boolean isValid(List<ZoneModel> data) {
        return data != null;
    }

    @Override
    void onPossibleChangeDetected(Map<Long, ZoneModel> watchZoneModels) {
        ChangeSet changeSet = new ChangeSet();
        changeSet.addedLimits = new ArrayList<>();
        changeSet.changedLimits = new ArrayList<>();
        changeSet.removedLimits = new ArrayList<>(mWatchZoneModels.keySet());
        for (Long uid : watchZoneModels.keySet()) {
            changeSet.removedLimits.remove(uid);
            if (!mWatchZoneModels.containsKey(uid)) {
                changeSet.addedLimits.add(uid);
            } else {
                ZoneModel oldModel = mWatchZoneModels.get(uid);
                ZoneModel newModel = watchZoneModels.get(uid);
                Boolean isChanged = oldModel.isChanged(newModel);
                if (isChanged) {
                    changeSet.changedLimits.add(uid);
                }
            }
        }
        mWatchZoneModels = watchZoneModels;

        mCallback.onModelsChanged(mWatchZoneModels, changeSet);
    }

    @Override
    Map<Long, ZoneModel> getData(List<ZoneModel> data) {
        HashMap<Long, ZoneModel> results = new HashMap<>();
        for (ZoneModel model : data) {
            results.put(model.watchZone.getUid(), model);
        }
        if (mWatchZoneModels == null) {
            mWatchZoneModels = results;
        }
        return results;
    }

    public Map<Long, ZoneModel> getWatchZoneModels() {
        return mWatchZoneModels;
    }
}
