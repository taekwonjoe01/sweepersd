package com.example.joseph.sweepersd.revision3.watchzone;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WatchZonePointsModel extends LiveData<WatchZonePointsModel> implements
        ListUpdateCallback {
    private final Context mApplicationContext;
    private final Handler mHandler;
    private final Long mWatchZoneUid;

    private Map<Long, WatchZonePoint> mWatchZonePointsMap;

    private List<WatchZonePoint> mCurrentList;
    private List<WatchZonePoint> mChangeToList;

    private Observer<List<WatchZonePoint>> mDatabaseObserver = new Observer<List<WatchZonePoint>>() {
        @Override
        public void onChanged(@Nullable final List<WatchZonePoint> watchZonePoints) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (WatchZonePointsModel.this) {
                        if (watchZonePoints == null || watchZonePoints.isEmpty()) {
                            // Invalid values for this LiveData. Notify observers that this data is invalid.
                            postValue(null);
                        } else {
                            if (mWatchZonePointsMap == null) {
                                mWatchZonePointsMap = new HashMap<>();
                            }
                            if (mCurrentList == null) {
                                mCurrentList = new ArrayList<>();
                            }
                            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                                @Override
                                public int getOldListSize() {
                                    return mWatchZonePointsMap == null ? 0 : mCurrentList.size();
                                }

                                @Override
                                public int getNewListSize() {
                                    return watchZonePoints.size();
                                }

                                @Override
                                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                                    return mCurrentList.get(oldItemPosition).getUid()
                                            == watchZonePoints.get(newItemPosition).getUid();
                                }

                                @Override
                                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                                    return !mCurrentList.get(oldItemPosition).isChanged(watchZonePoints.get(newItemPosition));
                                }
                            }, false);
                            mChangeToList = watchZonePoints;
                            result.dispatchUpdatesTo(WatchZonePointsModel.this);
                            mCurrentList = watchZonePoints;
                        }
                    }
                }
            });
        }
    };

    public WatchZonePointsModel(Context context, Handler handler, Long watchZoneUid) {
        mApplicationContext = context.getApplicationContext();
        mHandler = handler;
        mWatchZoneUid = watchZoneUid;
    }

    public synchronized Long getWatchZoneUid() {
        return mWatchZoneUid;
    }

    public synchronized WatchZonePoint getWatchZonePointForWatchZonePointUid(Long watchZonePointUid) {
        return getValue() == null ? null : mWatchZonePointsMap.get(watchZonePointUid);
    }

    public synchronized List<WatchZonePoint> getWatchZonePointsList() {
        return getValue() == null ? null : mCurrentList;
    }

    public synchronized boolean isChanged(WatchZonePointsModel compareTo) {
        boolean result = false;

        if (this.mWatchZoneUid == compareTo.getWatchZoneUid()) {
            if (this.getWatchZonePointsList().size() != compareTo.getWatchZonePointsList().size()) {
                result = true;
            } else {
                int index = 0;
                List<WatchZonePoint> otherList = compareTo.getWatchZonePointsList();
                for (WatchZonePoint p : getWatchZonePointsList()) {
                    WatchZonePoint op = otherList.get(index);
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
        WatchZoneRepository.getInstance(mApplicationContext).getWatchZonePointsLiveData(mWatchZoneUid)
                .observeForever(mDatabaseObserver);
    }

    @Override
    protected synchronized void onInactive() {
        super.onInactive();
        WatchZoneRepository.getInstance(mApplicationContext).getWatchZonePointsLiveData(mWatchZoneUid)
                .removeObserver(mDatabaseObserver);
    }

    @Override
    public void onChanged(int position, int count, Object payload) {
        Map<Long, WatchZonePoint> points = mWatchZonePointsMap;
        for (int i = 0; i < count; i++) {
            WatchZonePoint changedSchedule = mChangeToList.get(i + position);
            points.put(changedSchedule.getUid(), changedSchedule);
        }
        postValue(this);
    }

    @Override
    public void onInserted(int position, int count) {
        Map<Long, WatchZonePoint> points = mWatchZonePointsMap;
        for (int i = 0; i < count; i++) {
            WatchZonePoint insertedSchedule = mChangeToList.get(i + position);
            points.put(insertedSchedule.getUid(), insertedSchedule);
        }
        postValue(this);
    }

    @Override
    public void onMoved(int fromPosition, int toPosition) {
        // Detect moves is false, so do nothing.
    }

    @Override
    public void onRemoved(int position, int count) {
        Map<Long, WatchZonePoint> points = mWatchZonePointsMap;
        for (int i = 0; i < count; i++) {
            WatchZonePoint removedSchedule = mCurrentList.get(i + position);
            points.remove(removedSchedule.getUid());
        }
        postValue(this);
    }
}
