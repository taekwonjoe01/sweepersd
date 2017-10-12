package com.example.joseph.sweepersd.revision3.watchzone.model;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;

import com.example.joseph.sweepersd.revision3.limit.LimitRepository;
import com.example.joseph.sweepersd.revision3.limit.LimitSchedule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WatchZoneLimitSchedulesModel extends LiveData<WatchZoneLimitSchedulesModel> implements
        ListUpdateCallback {
    private final Context mApplicationContext;
    private final Handler mHandler;
    private final Long mLimitUid;

    private Map<Long, LimitSchedule> mLimitSchedulesMap;

    private List<LimitSchedule> mCurrentList;
    private List<LimitSchedule> mChangeToList;

    private WatchZoneModel.ModelStatus mStatus;

    private Observer<List<LimitSchedule>> mDatabaseObserver = new Observer<List<LimitSchedule>>() {
        @Override
        public void onChanged(@Nullable final List<LimitSchedule> limitSchedules) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (WatchZoneLimitSchedulesModel.this) {
                        if (limitSchedules == null || limitSchedules.isEmpty()) {
                            // Invalid values for this LiveData. Notify observers that this data is invalid.
                            mStatus = WatchZoneModel.ModelStatus.INVALID;
                            postValue(WatchZoneLimitSchedulesModel.this);
                        } else {
                            if (mLimitSchedulesMap == null) {
                                mLimitSchedulesMap = new HashMap<>();
                            }
                            if (mCurrentList == null) {
                                mCurrentList = new ArrayList<>();
                            }
                            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                                @Override
                                public int getOldListSize() {
                                    return mCurrentList == null ? 0 : mCurrentList.size();
                                }

                                @Override
                                public int getNewListSize() {
                                    return limitSchedules.size();
                                }

                                @Override
                                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                                    return mCurrentList.get(oldItemPosition).getUid()
                                            == limitSchedules.get(newItemPosition).getUid();
                                }

                                @Override
                                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                                    return !mCurrentList.get(oldItemPosition).isChanged(limitSchedules.get(newItemPosition));
                                }
                            }, false);
                            mChangeToList = limitSchedules;
                            result.dispatchUpdatesTo(WatchZoneLimitSchedulesModel.this);
                            mCurrentList = limitSchedules;

                            mStatus = WatchZoneModel.ModelStatus.LOADED;
                            postValue(WatchZoneLimitSchedulesModel.this);
                        }
                    }
                }
            });
        }
    };

    public WatchZoneLimitSchedulesModel(Context context, Handler handler, Long limitUid) {
        mApplicationContext = context.getApplicationContext();
        mHandler = handler;
        mLimitUid = limitUid;

        mStatus = WatchZoneModel.ModelStatus.LOADING;
        setValue(this);
    }

    public synchronized Long getLimitUid() {
        return mLimitUid;
    }

    public synchronized LimitSchedule getLimitScheduleForLimitScheduleUid(Long limitScheduleUid) {
        return getStatus() == WatchZoneModel.ModelStatus.INVALID ? null : mLimitSchedulesMap.get(limitScheduleUid);
    }

    public synchronized List<LimitSchedule> getScheduleList() {
        return getStatus() == WatchZoneModel.ModelStatus.INVALID ? null : mCurrentList;
    }

    public synchronized WatchZoneModel.ModelStatus getStatus() {
        return mStatus;
    }

    public synchronized boolean isChanged(WatchZoneLimitSchedulesModel compareTo) {
        boolean result = false;

        if (this.mLimitUid == compareTo.getLimitUid()) {
            if (this.getScheduleList().size() != compareTo.getScheduleList().size()) {
                result = true;
            } else {
                int index = 0;
                List<LimitSchedule> otherList = compareTo.getScheduleList();
                for (LimitSchedule p : getScheduleList()) {
                    LimitSchedule op = otherList.get(index);
                    if (p.isChanged(op)) {
                        result = true;
                        break;
                    }
                    index++;
                }
            }
        }
        return result;
    }

    @Override
    protected synchronized void onActive() {
        super.onActive();
        LimitRepository.getInstance(mApplicationContext).getLimitSchedulesLiveData(mLimitUid)
                .observeForever(mDatabaseObserver);
    }

    @Override
    protected synchronized void onInactive() {
        super.onInactive();
        LimitRepository.getInstance(mApplicationContext).getLimitSchedulesLiveData(mLimitUid)
                .removeObserver(mDatabaseObserver);
    }

    @Override
    public void onChanged(int position, int count, Object payload) {
        Map<Long, LimitSchedule> schedules = mLimitSchedulesMap;
        for (int i = 0; i < count; i++) {
            LimitSchedule changedSchedule = mChangeToList.get(i + position);
            schedules.put(changedSchedule.getUid(), changedSchedule);
        }
    }

    @Override
    public void onInserted(int position, int count) {
        Map<Long, LimitSchedule> schedules = mLimitSchedulesMap;
        for (int i = 0; i < count; i++) {
            LimitSchedule insertedSchedule = mChangeToList.get(i + position);
            schedules.put(insertedSchedule.getUid(), insertedSchedule);
        }
    }

    @Override
    public void onMoved(int fromPosition, int toPosition) {
        // Detect moves is false, so do nothing.
    }

    @Override
    public void onRemoved(int position, int count) {
        Map<Long, LimitSchedule> schedules = mLimitSchedulesMap;
        for (int i = 0; i < count; i++) {
            LimitSchedule removedSchedule = mCurrentList.get(i + position);
            schedules.remove(removedSchedule.getUid());
        }
    }
}
