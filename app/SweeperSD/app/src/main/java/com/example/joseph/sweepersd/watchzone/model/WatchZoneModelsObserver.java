package com.example.joseph.sweepersd.watchzone.model;

import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;

import java.util.ArrayList;
import java.util.List;

public class WatchZoneModelsObserver extends WatchZoneBaseObserver<List<WatchZoneModel>> {
    private final WatchZoneModelsChangedCallback mCallback;
    private final List<Long> mWatchZoneUids;

    protected List<WatchZoneModel> mWatchZoneModels;

    public interface WatchZoneModelsChangedCallback extends WatchZoneBaseObserverCallback<List<WatchZoneModel>> {
        void onWatchZonePointAdded(int index);
        void onWatchZonePointRemoved(int index);
        void onWatchZonePointUpdated(int index);
    }

    public WatchZoneModelsObserver(List<Long> watchZoneUids, WatchZoneModelsChangedCallback callback) {
        super(callback);
        mCallback = callback;
        mWatchZoneUids = watchZoneUids;
    }

    @Override
    boolean isValid(WatchZoneModelRepository watchZoneModelRepository) {
        boolean valid = true;
        for (Long uid : mWatchZoneUids) {
            if (!watchZoneModelRepository.watchZoneExists(uid)) {
                valid = false;
            }
        }
        return valid;
    }

    @Override
    List<WatchZoneModel> getDataFromRepo(WatchZoneModelRepository watchZoneModelRepository) {
        List<WatchZoneModel> results = new ArrayList<>();
        for (Long uid : mWatchZoneUids) {
            WatchZoneModel model = watchZoneModelRepository.getWatchZoneModel(uid);
            if (model != null) {
                if (model.getStatus() != WatchZoneModel.Status.LOADING) {
                    results.add(model);
                }
            }
        }
        if (results.size() != mWatchZoneUids.size()) {
            results = null;
        } else {
            if (mWatchZoneModels == null) {
                mWatchZoneModels = results;
            }
        }
        return results;
    }

    @Override
    void onRepositoryChanged(final List<WatchZoneModel> watchZoneModels) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return mWatchZoneModels.size();
            }

            @Override
            public int getNewListSize() {
                return watchZoneModels.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return mWatchZoneModels.get(oldItemPosition).getWatchZoneUid() ==
                        watchZoneModels.get(newItemPosition).getWatchZoneUid();
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return !mWatchZoneModels.get(oldItemPosition).isChanged(
                        watchZoneModels.get(newItemPosition));
            }
        }, false);
        result.dispatchUpdatesTo(new ListUpdateCallback() {
            @Override
            public void onInserted(int position, int count) {
                for (int i = 0; i < count; i++) {
                    int index = position + i;
                    mWatchZoneModels.add(index, watchZoneModels.get(index));
                    mCallback.onWatchZonePointAdded(index);
                }
            }

            @Override
            public void onRemoved(int position, int count) {
                for (int i = 0; i < count; i++) {
                    int index = position;
                    mCallback.onWatchZonePointRemoved(index);
                    mWatchZoneModels.remove(index);
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

    public List<WatchZoneModel> getWatchZoneModels() {
        return mWatchZoneModels;
    }
}
