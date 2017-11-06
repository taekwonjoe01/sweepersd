package com.example.joseph.sweepersd.watchzone.model;

import com.example.joseph.sweepersd.utils.BaseObserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WatchZoneModelsObserver extends BaseObserver<Map<Long, WatchZoneModel>, List<WatchZoneModel>> {
    private final WatchZoneModelsChangedCallback mCallback;
    private final boolean mDetectDeepChanges;

    protected Map<Long, WatchZoneModel> mWatchZoneModels;

    public interface WatchZoneModelsChangedCallback extends BaseObserverCallback<Map<Long, WatchZoneModel>> {
        void onModelsChanged(Map<Long, WatchZoneModel> data, ChangeSet changeSet);
    }

    public WatchZoneModelsObserver(boolean detectDeepChanges, WatchZoneModelsChangedCallback callback) {
        super(callback);
        mCallback = callback;
        mDetectDeepChanges = detectDeepChanges;
    }

    @Override
    public boolean isValid(List<WatchZoneModel> data) {
        return data != null;
    }

    @Override
    public void onPossibleChangeDetected(Map<Long, WatchZoneModel> watchZoneModels) {
        ChangeSet changeSet = new ChangeSet();
        changeSet.addedLimits = new ArrayList<>();
        changeSet.changedLimits = new ArrayList<>();
        changeSet.removedLimits = new ArrayList<>(mWatchZoneModels.keySet());
        for (Long uid : watchZoneModels.keySet()) {
            changeSet.removedLimits.remove(uid);
            if (!mWatchZoneModels.containsKey(uid)) {
                changeSet.addedLimits.add(uid);
            } else {
                WatchZoneModel oldModel = mWatchZoneModels.get(uid);
                WatchZoneModel newModel = watchZoneModels.get(uid);
                if (mDetectDeepChanges) {
                    Boolean isChanged = oldModel.isChanged(newModel);
                    if (isChanged) {
                        changeSet.changedLimits.add(uid);
                    }
                } else {
                    WatchZone oldZone = oldModel.watchZone;
                    WatchZone newZone = newModel.watchZone;
                    if (oldZone.isChanged(true, newZone)) {
                        changeSet.changedLimits.add(uid);
                    }
                }
            }
        }
        mWatchZoneModels = watchZoneModels;

        if (!changeSet.changedLimits.isEmpty() || !changeSet.removedLimits.isEmpty()
                || !changeSet.addedLimits.isEmpty()) {
            mCallback.onModelsChanged(mWatchZoneModels, changeSet);
        }
    }

    @Override
    public Map<Long, WatchZoneModel> getData(List<WatchZoneModel> data) {
        HashMap<Long, WatchZoneModel> results = new HashMap<>();
        for (WatchZoneModel model : data) {
            results.put(model.watchZone.getUid(), model);
        }
        if (mWatchZoneModels == null) {
            mWatchZoneModels = results;
        }
        return results;
    }

    public Map<Long, WatchZoneModel> getWatchZoneModels() {
        return mWatchZoneModels;
    }
}
