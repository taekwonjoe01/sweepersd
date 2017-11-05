package com.example.joseph.sweepersd.watchzone.model;

import com.example.joseph.sweepersd.utils.BaseObserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WatchZonePointsObserver extends BaseObserver<Map<Long, PointModel>, ZoneModel> {
    private final Long mWatchZoneUid;
    private final WatchZonePointsChangedCallback mCallback;

    protected Map<Long, PointModel> mWatchZonePoints;

    public interface WatchZonePointsChangedCallback extends BaseObserverCallback<Map<Long, PointModel>> {
        void onWatchZonePointsChanged(Map<Long, PointModel> watchZonePointMap, ChangeSet changeSet);
    }

    public WatchZonePointsObserver(Long watchZoneUid, WatchZonePointsChangedCallback callback) {
        super(callback);
        mWatchZoneUid = watchZoneUid;
        mCallback = callback;
    }

    @Override
    public boolean isValid(ZoneModel zoneModel) {
        return zoneModel != null;
    }

    @Override
    public Map<Long, PointModel> getData(ZoneModel zoneModel) {
        HashMap<Long, PointModel> results = new HashMap<>();
        for (PointModel model : zoneModel.points) {
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
    public void onPossibleChangeDetected(Map<Long, PointModel> watchZonePoints) {
        ChangeSet changeSet = new ChangeSet();
        changeSet.addedLimits = new ArrayList<>();
        changeSet.changedLimits = new ArrayList<>();
        changeSet.removedLimits = new ArrayList<>(mWatchZonePoints.keySet());
        for (Long uid : watchZonePoints.keySet()) {
            changeSet.removedLimits.remove(uid);
            if (!mWatchZonePoints.containsKey(uid)) {
                changeSet.addedLimits.add(uid);
            } else {
                PointModel curPoint = mWatchZonePoints.get(uid);
                PointModel newPoint = watchZonePoints.get(uid);
                if (curPoint.isChanged(newPoint)) {
                    changeSet.changedLimits.add(uid);
                }
            }
        }
        mWatchZonePoints = watchZonePoints;

        mCallback.onWatchZonePointsChanged(mWatchZonePoints, changeSet);
    }

    public Map<Long, PointModel> getWatchZonePoints() {
        return mWatchZonePoints;
    }
}