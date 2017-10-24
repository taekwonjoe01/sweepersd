package com.example.joseph.sweepersd.watchzone.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WatchZoneModelsObserver extends WatchZoneBaseObserver<Map<Long, WatchZoneModel>> {
    private final WatchZoneModelsChangedCallback mCallback;

    protected Map<Long, WatchZoneModel> mWatchZoneModels;

    public interface WatchZoneModelsChangedCallback extends WatchZoneBaseObserverCallback<Map<Long, WatchZoneModel>> {
        void onModelsChanged(Map<Long, WatchZoneModel> data, ChangeSet changeSet);
    }

    public WatchZoneModelsObserver(WatchZoneModelsChangedCallback callback) {
        super(callback);
        mCallback = callback;
    }

    @Override
    boolean isValid(WatchZoneModelRepository watchZoneModelRepository) {
        return true;
    }

    @Override
    Map<Long, WatchZoneModel> getDataFromRepo(WatchZoneModelRepository watchZoneModelRepository) {
        Map<Long, WatchZoneModel> modelMap = watchZoneModelRepository.getWatchZoneModels();
        boolean ready = true;
        for (Long uid : modelMap.keySet()) {
            WatchZoneModel model = watchZoneModelRepository.getWatchZoneModel(uid);
            if (model == null) {
                ready = false;
                break;
            } else {
                if (model.getStatus() == WatchZoneModel.Status.LOADING) {
                    ready = false;
                    break;
                }
            }
        }
        if (ready) {
            if (mWatchZoneModels == null) {
                mWatchZoneModels = modelMap;
            }
            return modelMap;
        } else {
            return null;
        }
    }

    @Override
    void onRepositoryChanged(final Map<Long, WatchZoneModel> watchZoneModels) {
        ChangeSet changeSet = new ChangeSet();
        changeSet.addedLimits = new ArrayList<>();
        changeSet.changedLimits = new ArrayList<>();
        changeSet.removedLimits = new ArrayList<>(mWatchZoneModels.keySet());
        for (Long uid : watchZoneModels.keySet()) {
            changeSet.removedLimits.remove(uid);
            if (!mWatchZoneModels.containsKey(uid)) {
                changeSet.addedLimits.add(uid);
            } else {
                WatchZoneModel curModel = mWatchZoneModels.get(uid);
                WatchZoneModel newModel = watchZoneModels.get(uid);
                if (curModel.isChanged(newModel)) {
                    changeSet.changedLimits.add(uid);
                }
            }
        }
        mWatchZoneModels = watchZoneModels;

        mCallback.onModelsChanged(mWatchZoneModels, changeSet);
    }

    public Map<Long, WatchZoneModel> getWatchZoneModels() {
        return mWatchZoneModels;
    }
}
