package com.example.joseph.sweepersd.watchzone.model;

import com.example.joseph.sweepersd.utils.BaseObserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WatchZoneModelsObserver extends BaseObserver<Map<Long, ZoneModel>, List<ZoneModel>> {
    private final WatchZoneModelsChangedCallback mCallback;
    private final boolean mDetectDeepChanges;

    protected Map<Long, ZoneModel> mWatchZoneModels;

    public interface WatchZoneModelsChangedCallback extends BaseObserverCallback<Map<Long, ZoneModel>> {
        void onModelsChanged(Map<Long, ZoneModel> data, ChangeSet changeSet);
    }

    public WatchZoneModelsObserver(boolean detectDeepChanges, WatchZoneModelsChangedCallback callback) {
        super(callback);
        mCallback = callback;
        mDetectDeepChanges = detectDeepChanges;
    }

    @Override
    public boolean isValid(List<ZoneModel> data) {
        return data != null;
    }

    @Override
    public void onPossibleChangeDetected(Map<Long, ZoneModel> watchZoneModels) {
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
                if (mDetectDeepChanges) {
                    Boolean isChanged = oldModel.isChanged(newModel);
                    if (isChanged) {
                        changeSet.changedLimits.add(uid);
                    }
                } else {
                    WatchZone oldZone = oldModel.watchZone;
                    WatchZone newZone = newModel.watchZone;
                    if (oldZone.isChanged(newZone)) {
                        changeSet.changedLimits.add(uid);
                    }
                }
            }
        }
        mWatchZoneModels = watchZoneModels;

        mCallback.onModelsChanged(mWatchZoneModels, changeSet);
    }

    @Override
    public Map<Long, ZoneModel> getData(List<ZoneModel> data) {
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
