package com.example.joseph.sweepersd.watchzone.model;

import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by joseph on 10/18/17.
 */

public class WatchZoneLimitsObserver extends WatchZoneBaseObserver<List<WatchZoneLimitModel>> {
    final Long mWatchZoneUid;
    private final WatchZoneLimitsChangedCallback mCallback;

    protected List<WatchZoneLimitModel> mLimitModels;

    public interface WatchZoneLimitsChangedCallback extends WatchZoneBaseObserverCallback<List<WatchZoneLimitModel>> {
        void onLimitModelAdded(int index);
        void onLimitModelRemoved(int index);
        void onLimitModelUpdated(int index);
    }

    public WatchZoneLimitsObserver(Long watchZoneUid, WatchZoneLimitsChangedCallback callback) {
        super(callback);
        mWatchZoneUid = watchZoneUid;
        mCallback = callback;
    }

    @Override
    boolean isValid(WatchZoneModelRepository watchZoneModelRepository) {
        return watchZoneModelRepository.watchZoneExists(mWatchZoneUid);
    }

    @Override
    List<WatchZoneLimitModel> getDataFromRepo(WatchZoneModelRepository watchZoneModelRepository) {
        WatchZoneModel model = watchZoneModelRepository.getWatchZoneModel(mWatchZoneUid);
        if (model != null) {
            List<Long> limitUids = new ArrayList<>(model.getWatchZoneLimitModelUids());
            Collections.sort(limitUids);
            List<WatchZoneLimitModel> limitModels = new ArrayList<>();
            for (Long uid : limitUids) {
                WatchZoneLimitModel limitModel = model.getWatchZoneLimitModel(uid);
                if (limitModel != null) {
                    if (limitModel.getLimitSchedulesModel() != null &&
                            limitModel.getLimitSchedulesModel().getScheduleList() != null) {
                        limitModels.add(limitModel);
                    }
                }
            }
            if (mLimitModels == null) {
                mLimitModels = limitModels;
            }
            return limitModels;
        }
        return null;
    }

    @Override
    void onRepositoryChanged(final List<WatchZoneLimitModel> limitModels) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return mLimitModels.size();
            }

            @Override
            public int getNewListSize() {
                return limitModels.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return mLimitModels.get(oldItemPosition).getLimitUid() ==
                        limitModels.get(newItemPosition).getLimitUid();
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return !mLimitModels.get(oldItemPosition).isChanged(
                        limitModels.get(newItemPosition));
            }
        }, true);
        result.dispatchUpdatesTo(new ListUpdateCallback() {
            @Override
            public void onInserted(int position, int count) {
                for (int i = 0; i < count; i++) {
                    int index = position + i;
                    mLimitModels.add(index, limitModels.get(index));
                    mCallback.onLimitModelAdded(index);
                }
            }

            @Override
            public void onRemoved(int position, int count) {
                for (int i = 0; i < count; i++) {
                    int index = position;
                    mCallback.onLimitModelRemoved(index);
                    mLimitModels.remove(index);
                }
            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {
                WatchZoneLimitModel model = mLimitModels.remove(fromPosition);
                mLimitModels.add(toPosition, model);
            }

            @Override
            public void onChanged(int position, int count, Object payload) {
                for (int i = 0; i < count; i++) {
                    int index = position + i;
                    mCallback.onLimitModelUpdated(index);
                }
            }
        });
    }

    public List<WatchZoneLimitModel> getLimitModels() {
        return mLimitModels;
    }
}
