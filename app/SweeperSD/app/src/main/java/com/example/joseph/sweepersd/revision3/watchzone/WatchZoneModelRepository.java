package com.example.joseph.sweepersd.revision3.watchzone;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WatchZoneModelRepository extends LiveData<WatchZoneModelRepository> implements
        ListUpdateCallback {
    private static final String TAG = WatchZoneModelRepository.class.getSimpleName();

    private static WatchZoneModelRepository sInstance;

    private final Context mApplicationContext;
    private final Handler mHandler;
    private final HandlerThread mThread;

    private final Map<Long, WatchZoneModel> mWatchZoneModelsMap;

    private List<WatchZone> mCurrentList;
    private List<WatchZone> mChangeToList;

    private final Observer<List<WatchZone>> mWatchZoneObserver = new Observer<List<WatchZone>>() {
        @Override
        public void onChanged(@Nullable final List<WatchZone> watchZones) {
            if (watchZones != null && !watchZones.isEmpty()) {
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
                        return watchZones.size();
                    }

                    @Override
                    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                        return mCurrentList.get(oldItemPosition).getUid()
                                == watchZones.get(newItemPosition).getUid();
                    }

                    @Override
                    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                        return !mCurrentList.get(oldItemPosition).isChanged(watchZones.get(newItemPosition));
                    }
                }, false);
                mChangeToList = watchZones;
                result.dispatchUpdatesTo(WatchZoneModelRepository.this);
                mCurrentList = watchZones;
            }

            postValue(WatchZoneModelRepository.this);
        }
    };

    private final Observer<WatchZoneModel> mWatchZoneModelObserver = new Observer<WatchZoneModel>() {
        @Override
        public void onChanged(@Nullable WatchZoneModel watchZoneModel) {
            postValue(WatchZoneModelRepository.this);
        }
    };

    private WatchZoneModelRepository(Context context) {
        mApplicationContext = context.getApplicationContext();
        mThread = new HandlerThread("WatchZoneModelRepositoryUpdateThread");
        mThread.start();
        mHandler = new Handler(mThread.getLooper());

        mWatchZoneModelsMap = new HashMap<>();

        WatchZoneRepository.getInstance(mApplicationContext).getWatchZonesLiveData()
                .observeForever(mWatchZoneObserver);
    }

    public static synchronized WatchZoneModelRepository getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new WatchZoneModelRepository(context);
        }
        return sInstance;
    }

    public synchronized void delete() {
        if (sInstance != null) {
            // TODO Remove observers
            WatchZoneRepository.getInstance(mApplicationContext).getWatchZonesLiveData()
                .removeObserver(mWatchZoneObserver);
            for (Long watchZoneUid : mWatchZoneModelsMap.keySet()) {
                WatchZoneModel model = mWatchZoneModelsMap.get(watchZoneUid);
                model.removeObserver(mWatchZoneModelObserver);
            }

            mThread.quit();

            sInstance = null;
        }
    }

    public synchronized List<WatchZoneModel> getWatchZoneModels() {
        if (mCurrentList == null) {
            return null;
        }
        List<WatchZoneModel> result = new ArrayList<>();
        for (WatchZone zone : mCurrentList) {
            result.add(mWatchZoneModelsMap.get(zone.getUid()));
        }
        return result;
    }

    @Override
    protected void onActive() {
        super.onActive();
        // Do nothing, because we're a singleton and want to exist until delete is called.
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        // Do nothing, because we're a singleton and want to exist until delete is called.
    }

    @Override
    public void onChanged(int position, int count, Object payload) {
        // Do nothing. If an item has changed, its WatchZoneModel will handle the updates.
    }

    @Override
    public void onInserted(int position, int count) {
        Map<Long, WatchZoneModel> models = mWatchZoneModelsMap;
        for (int i = 0; i < count; i++) {
            WatchZone insertedSchedule = mChangeToList.get(i + position);
            WatchZoneModel model = new WatchZoneModel(mApplicationContext, mHandler,
                    insertedSchedule.getUid());
            model.observeForever(mWatchZoneModelObserver);
            models.put(model.getWatchZoneUid(), model);
        }
        postValue(this);
    }

    @Override
    public void onMoved(int fromPosition, int toPosition) {
        // Detect moves is false, so do nothing.
    }

    @Override
    public void onRemoved(int position, int count) {
        Map<Long, WatchZoneModel> models = mWatchZoneModelsMap;
        for (int i = 0; i < count; i++) {
            WatchZone removedWatchZone = mCurrentList.get(i + position);
            models.remove(removedWatchZone.getUid());
        }
        postValue(this);
    }
}
