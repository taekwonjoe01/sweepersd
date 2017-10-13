package com.example.joseph.sweepersd.archived.model.watchzone;

import android.content.Context;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by joseph on 9/2/16.
 */
public class WatchZoneUpdateManager {
    public static final int INVALID_PROGRESS = -1;
    private static WatchZoneUpdateManager sInstance;

    private final Context mContext;
    private HashMap<Long, WatchZoneUpdater> mWatchZoneStatuses = new HashMap<>();
    private WatchZoneUpdaterFactory mWatchZoneUpdaterFactory;

    private Set<WeakReference<WatchZoneProgressListener>> mListeners = new HashSet<>();

    interface WatchZoneUpdaterFactory {
        WatchZoneUpdater createNewWatchZoneUpdater();
    }

    interface WatchZoneUpdater {
        void updateWatchZone(WatchZone watchZone, WatchZoneProgressListener listener);
        int getProgress();
    }

    public interface WatchZoneProgressListener {
        void onWatchZoneUpdateProgress(long createdTimestamp, int progress);
        void onWatchZoneUpdateComplete(long createdTimestamp);
    }

    private WatchZoneUpdateManager(Context context) {
        mContext = context;

        mWatchZoneUpdaterFactory = new ServiceWatchZoneUpdaterFactory(mContext);
    }

    /**
     * For test purposes only
     */
    void setWatchZoneUpdaterFactory(WatchZoneUpdaterFactory factory) {
        if (factory == null) {
            mWatchZoneUpdaterFactory = new ServiceWatchZoneUpdaterFactory(mContext);
        } else {
            mWatchZoneUpdaterFactory = factory;
        }
    }

    public void addListener(WatchZoneProgressListener listener) {
        if (listener != null) {
            mListeners.add(new WeakReference<>(listener));
        }
    }

    public void removeListener(WatchZoneProgressListener listener) {
        if (listener != null) {
            WeakReference<WatchZoneProgressListener> toRemove = null;
            for (WeakReference<WatchZoneProgressListener> weakRef : mListeners) {
                WatchZoneProgressListener curListener = weakRef.get();
                if (curListener == listener) {
                    toRemove = weakRef;
                    break;
                }
            }
            mListeners.remove(toRemove);
        }
    }

    public static WatchZoneUpdateManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new WatchZoneUpdateManager(context.getApplicationContext());
        }
        return sInstance;
    }

    public boolean updateWatchZone(WatchZone watchZone) {
        if (mWatchZoneStatuses.containsKey(watchZone.getCreatedTimestamp())) {
            return false;
        } else {
            WatchZoneUpdater updater = mWatchZoneUpdaterFactory.createNewWatchZoneUpdater();
            mWatchZoneStatuses.put(watchZone.getCreatedTimestamp(), updater);
            updater.updateWatchZone(watchZone, mProgressListener);
            return true;
        }
    }

    public Set<Long> getUpdatingWatchZoneTimestamps() {
        return mWatchZoneStatuses.keySet();
    }

    public int getProgressForWatchZone(long createdTimestamp) {
        if (mWatchZoneStatuses.containsKey(createdTimestamp)) {
            return mWatchZoneStatuses.get(createdTimestamp).getProgress();
        } else {
            return INVALID_PROGRESS;
        }
    }

    private void notifyProgress(long id, int progress) {
        List<WeakReference> toRemove = new ArrayList<>();
        List<WatchZoneProgressListener> listeners = new ArrayList<>();
        for (WeakReference<WatchZoneProgressListener> weakRef : mListeners) {
            WatchZoneProgressListener curListener = weakRef.get();
            if (curListener == null) {
                toRemove.add(weakRef);
            } else {
                listeners.add(curListener);
            }
        }

        for (WatchZoneProgressListener listener : listeners) {
            listener.onWatchZoneUpdateProgress(id, progress);
        }

        for (WeakReference removeMe : toRemove) {
            mListeners.remove(removeMe);
        }

        mListeners.remove(toRemove);
    }

    private void notifyComplete(long id) {
        List<WeakReference> toRemove = new ArrayList<>();
        List<WatchZoneProgressListener> listeners = new ArrayList<>();
        for (WeakReference<WatchZoneProgressListener> weakRef : mListeners) {
            WatchZoneProgressListener curListener = weakRef.get();
            if (curListener == null) {
                toRemove.add(weakRef);
            } else {
                listeners.add(curListener);
            }
        }

        for (WatchZoneProgressListener listener : listeners) {
            listener.onWatchZoneUpdateComplete(id);
        }

        for (WeakReference removeMe : toRemove) {
            mListeners.remove(removeMe);
        }

        mListeners.remove(toRemove);
    }

    private final WatchZoneProgressListener mProgressListener = new WatchZoneProgressListener() {
        @Override
        public void onWatchZoneUpdateProgress(long createdTimestamp, int progress) {
            notifyProgress(createdTimestamp, progress);
        }

        @Override
        public void onWatchZoneUpdateComplete(long createdTimestamp) {
            notifyComplete(createdTimestamp);
            mWatchZoneStatuses.remove(createdTimestamp);
        }
    };
}
