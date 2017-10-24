package com.example.joseph.sweepersd.watchzone.model;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WatchZonePointsObserver extends WatchZoneBaseObserver<Map<Long, WatchZonePoint>> {
    private final Long mWatchZoneUid;
    private final WatchZonePointsChangedCallback mCallback;

    protected Map<Long, WatchZonePoint> mWatchZonePoints;

    public interface WatchZonePointsChangedCallback extends WatchZoneBaseObserverCallback<Map<Long, WatchZonePoint>> {
        void onWatchZonePointsChanged(Map<Long, WatchZonePoint> watchZonePointMap, ChangeSet changeSet);
    }

    public WatchZonePointsObserver(Long watchZoneUid, WatchZonePointsChangedCallback callback) {
        super(callback);
        mWatchZoneUid = watchZoneUid;
        mCallback = callback;
    }

    @Override
    boolean isValid(WatchZoneModelRepository watchZoneModelRepository) {
        return watchZoneModelRepository.watchZoneExists(mWatchZoneUid);
    }

    @Override
    Map<Long, WatchZonePoint> getDataFromRepo(WatchZoneModelRepository watchZoneModelRepository) {
        WatchZoneModel model = watchZoneModelRepository.getWatchZoneModel(mWatchZoneUid);
        if (model != null) {
            WatchZonePointsModel pointsModel = model.getWatchZonePointsModel();
            if (pointsModel != null) {
                Map<Long, WatchZonePoint> watchZonePoints = pointsModel.getWatchZonePointsMap();
                if (watchZonePoints != null) {
                    Map<Long, WatchZonePoint> finishedPoints = new HashMap<>();
                    for (Long uid : watchZonePoints.keySet()) {
                        WatchZonePoint p = watchZonePoints.get(uid);
                        if (p.getAddress() != null) {
                            finishedPoints.put(uid, p);
                        }
                    }
                    if (mWatchZonePoints == null) {
                        mWatchZonePoints = finishedPoints;
                    }
                    return finishedPoints;
                }
            }
        }
        return null;
    }

    @Override
    void onRepositoryChanged(final Map<Long, WatchZonePoint> watchZonePoints) {
        ChangeSet changeSet = new ChangeSet();
        changeSet.addedLimits = new ArrayList<>();
        changeSet.changedLimits = new ArrayList<>();
        changeSet.removedLimits = new ArrayList<>(mWatchZonePoints.keySet());
        for (Long uid : watchZonePoints.keySet()) {
            changeSet.removedLimits.remove(uid);
            if (!mWatchZonePoints.containsKey(uid)) {
                changeSet.addedLimits.add(uid);
            } else {
                WatchZonePoint curPoint = mWatchZonePoints.get(uid);
                WatchZonePoint newPoint = watchZonePoints.get(uid);
                if (curPoint.isChanged(newPoint)) {
                    changeSet.changedLimits.add(uid);
                }
            }
        }
        mWatchZonePoints = watchZonePoints;

        mCallback.onWatchZonePointsChanged(mWatchZonePoints, changeSet);
    }

    public Map<Long, WatchZonePoint> getWatchZonePoints() {
        return mWatchZonePoints;
    }
}