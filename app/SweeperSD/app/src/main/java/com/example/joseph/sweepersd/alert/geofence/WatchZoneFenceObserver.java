package com.example.joseph.sweepersd.alert.geofence;

import com.example.joseph.sweepersd.utils.BaseObserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WatchZoneFenceObserver extends BaseObserver<Map<Long, WatchZoneFence>, List<WatchZoneFence>> {
    private final WatchZoneFencesChangedCallback mCallback;

    protected Map<Long, WatchZoneFence> mWatchZoneFences;

    public interface WatchZoneFencesChangedCallback extends BaseObserverCallback<Map<Long, WatchZoneFence>> {
        void onFencesChanges(Map<Long, WatchZoneFence> data, ChangeSet changeSet);
    }

    public WatchZoneFenceObserver(WatchZoneFencesChangedCallback callback) {
        super(callback);
        mCallback = callback;
    }

    @Override
    public boolean isValid(List<WatchZoneFence> fences) {
        return fences != null;
    }

    @Override
    public void onPossibleChangeDetected(Map<Long, WatchZoneFence> fences) {
        ChangeSet changeSet = new ChangeSet();
        changeSet.addedUids = new ArrayList<>();
        changeSet.changedUids = new ArrayList<>();
        changeSet.removedUids = new ArrayList<>(mWatchZoneFences.keySet());
        for (Long watchZoneUid : fences.keySet()) {
            changeSet.removedUids.remove(watchZoneUid);
            if (!mWatchZoneFences.containsKey(watchZoneUid)) {
                changeSet.addedUids.add(watchZoneUid);
            } else {
                WatchZoneFence oldFence = mWatchZoneFences.get(watchZoneUid);
                WatchZoneFence newFence = fences.get(watchZoneUid);
                if (oldFence.isChanged(newFence)) {
                    changeSet.changedUids.add(watchZoneUid);
                }
            }
        }
        mWatchZoneFences = fences;

        if (!changeSet.changedUids.isEmpty() || !changeSet.removedUids.isEmpty()
                || !changeSet.addedUids.isEmpty()) {
            mCallback.onFencesChanges(mWatchZoneFences, changeSet);
        }
    }

    @Override
    public Map<Long, WatchZoneFence> getData(List<WatchZoneFence> data) {
        HashMap<Long, WatchZoneFence> results = new HashMap<>();
        for (WatchZoneFence fence : data) {
            results.put(fence.getWatchZoneId(), fence);
        }
        if (mWatchZoneFences == null) {
            mWatchZoneFences = results;
        }
        return results;
    }

    public Map<Long, WatchZoneFence> getWatchZoneFences() {
        return mWatchZoneFences;
    }
}
