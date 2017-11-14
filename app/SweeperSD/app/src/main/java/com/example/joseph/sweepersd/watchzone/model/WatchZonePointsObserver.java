package com.example.joseph.sweepersd.watchzone.model;

import com.example.joseph.sweepersd.utils.BaseObserver;
import com.example.joseph.sweepersd.utils.ChangeSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WatchZonePointsObserver extends BaseObserver<Map<Long, WatchZonePointModel>, WatchZoneModel> {
    private final Long mWatchZoneUid;
    private final WatchZonePointsChangedCallback mCallback;

    protected Map<Long, WatchZonePointModel> mWatchZonePoints;

    public interface WatchZonePointsChangedCallback extends BaseObserverCallback<Map<Long, WatchZonePointModel>> {
        void onWatchZonePointsChanged(Map<Long, WatchZonePointModel> watchZonePointMap, ChangeSet changeSet);
    }

    public WatchZonePointsObserver(Long watchZoneUid, WatchZonePointsChangedCallback callback) {
        super(callback);
        mWatchZoneUid = watchZoneUid;
        mCallback = callback;
    }

    @Override
    public boolean isValid(WatchZoneModel watchZoneModel) {
        return watchZoneModel != null;
    }

    @Override
    public Map<Long, WatchZonePointModel> getData(WatchZoneModel watchZoneModel) {
        HashMap<Long, WatchZonePointModel> results = new HashMap<>();
        for (WatchZonePointModel model : watchZoneModel.points) {
            if (model.point.getAddress() != null) {
                results.put(model.point.getUid(), model);
            }
        }
        if (mWatchZonePoints == null) {
            mWatchZonePoints = results;
        }
        return results;
    }

    @Override
    public void onPossibleChangeDetected(Map<Long, WatchZonePointModel> watchZonePoints) {
        ChangeSet changeSet = new ChangeSet();
        changeSet.addedUids = new ArrayList<>();
        changeSet.changedUids = new ArrayList<>();
        changeSet.removedUids = new ArrayList<>(mWatchZonePoints.keySet());
        for (Long uid : watchZonePoints.keySet()) {
            changeSet.removedUids.remove(uid);
            if (!mWatchZonePoints.containsKey(uid)) {
                changeSet.addedUids.add(uid);
            } else {
                WatchZonePointModel curPoint = mWatchZonePoints.get(uid);
                WatchZonePointModel newPoint = watchZonePoints.get(uid);
                if (curPoint.isChanged(newPoint)) {
                    changeSet.changedUids.add(uid);
                }
            }
        }
        mWatchZonePoints = watchZonePoints;

        mCallback.onWatchZonePointsChanged(mWatchZonePoints, changeSet);
    }

    public Map<Long, WatchZonePointModel> getWatchZonePoints() {
        return mWatchZonePoints;
    }
}