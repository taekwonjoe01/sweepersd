package com.example.joseph.sweepersd.watchzone.model;

import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;

import java.util.ArrayList;
import java.util.List;

public class WatchZonePointsObserver extends WatchZoneBaseObserver<List<WatchZonePoint>> {
    private final Long mWatchZoneUid;
    private final WatchZonePointsChangedCallback mCallback;

    protected List<WatchZonePoint> mWatchZonePoints;

    public interface WatchZonePointsChangedCallback extends WatchZoneBaseObserverCallback<List<WatchZonePoint>> {
        void onWatchZonePointAdded(int index);
        void onWatchZonePointRemoved(int index);
        void onWatchZonePointUpdated(int index);
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
    List<WatchZonePoint> getDataFromRepo(WatchZoneModelRepository watchZoneModelRepository) {
        WatchZoneModel model = watchZoneModelRepository.getWatchZoneModel(mWatchZoneUid);
        if (model != null) {
            WatchZonePointsModel pointsModel = model.getWatchZonePointsModel();
            if (pointsModel != null) {
                List<WatchZonePoint> watchZonePoints = pointsModel.getWatchZonePointsList();
                if (watchZonePoints != null) {
                    final List<WatchZonePoint> finishedPoints = new ArrayList<>();
                    for (WatchZonePoint p : watchZonePoints) {
                        if (p.getAddress() != null) {
                            finishedPoints.add(p);
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
    void onRepositoryChanged(final List<WatchZonePoint> watchZonePoints) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return mWatchZonePoints.size();
            }

            @Override
            public int getNewListSize() {
                return watchZonePoints.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return mWatchZonePoints.get(oldItemPosition).getUid() ==
                        watchZonePoints.get(newItemPosition).getUid();
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return !mWatchZonePoints.get(oldItemPosition).isChanged(
                        watchZonePoints.get(newItemPosition));
            }
        }, false);
        result.dispatchUpdatesTo(new ListUpdateCallback() {
            @Override
            public void onInserted(int position, int count) {
                for (int i = 0; i < count; i++) {
                    int index = position + i;
                    mWatchZonePoints.add(index, watchZonePoints.get(index));
                    mCallback.onWatchZonePointAdded(index);
                }
            }

            @Override
            public void onRemoved(int position, int count) {
                for (int i = 0; i < count; i++) {
                    int index = position;
                    mCallback.onWatchZonePointRemoved(index);
                    mWatchZonePoints.remove(index);
                }
            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {
                // Do nothing.
            }

            @Override
            public void onChanged(int position, int count, Object payload) {
                for (int i = 0; i < count; i++) {
                    int index = position + i;
                    mCallback.onWatchZonePointUpdated(index);
                }
            }
        });
    }

    public List<WatchZonePoint> getWatchZonePoints() {
        return mWatchZonePoints;
    }
}