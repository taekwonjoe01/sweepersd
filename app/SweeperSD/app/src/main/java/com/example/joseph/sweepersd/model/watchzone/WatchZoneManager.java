package com.example.joseph.sweepersd.model.watchzone;

import android.content.Context;

import com.example.joseph.sweepersd.model.limits.Limit;
import com.example.joseph.sweepersd.model.limits.LimitSchedule;
import com.google.android.gms.maps.model.LatLng;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by joseph on 6/12/16.
 */
public class WatchZoneManager implements WatchZoneFileHelper.WatchZoneUpdateListener {
    private static final String TAG = WatchZoneManager.class.getSimpleName();

    private final WatchZoneFileHelper mWatchZoneFileHelper;
    private final HashMap<Long, WatchZone> mWatchZones;
    private final Context mContext;

    private Set<WeakReference<WatchZoneChangeListener>> mListeners = new HashSet<>();

    public interface WatchZoneChangeListener {
        void onWatchZoneUpdated(Long createdTimestamp);
        void onWatchZoneCreated(Long createdTimestamp);
        void onWatchZoneDeleted(Long createdTimestamp);
    }

    public WatchZoneManager(Context context) {
        mContext = context;
        mWatchZoneFileHelper = new WatchZoneFileHelper(mContext, this);

        List<WatchZone> watchZones = mWatchZoneFileHelper.loadWatchZones();
        if (watchZones.isEmpty()) {
            List<LimitSchedule> schedules = new ArrayList<>();
            for (int i = 1; i < 5; i++) {
                for (int j = 1; j < 8; j++) {
                    schedules.add(new LimitSchedule(12, 15, j, i));
                }
            }
            List<SweepingAddress> addresses = new ArrayList<>();
            addresses.add(new SweepingAddress(new LatLng(0, 0), "Satan's Butthole",
                    new Limit(666666666, "Satan's Butthole", new int[]{0, 666},
                            "Satan's Butthole", schedules)));
            WatchZone satan =
                    new WatchZone(666, 0, "Satan's Butthole", new LatLng(0, 0), 666, addresses);
            boolean watchZoneCreated = mWatchZoneFileHelper.saveWatchZone(satan);

            WatchZoneUpdateManager watchZoneUpdateManager = WatchZoneUpdateManager.getInstance(mContext);
            watchZoneUpdateManager.updateWatchZone(satan);
            watchZones.add(satan);
        }
        mWatchZones = new HashMap<>();
        for (WatchZone watchZone : watchZones) {
            mWatchZones.put(watchZone.getCreatedTimestamp(), watchZone);
        }
    }

    public void addWatchZoneChangeListener(WatchZoneChangeListener listener) {
        if (listener != null) {
            mListeners.add(new WeakReference<WatchZoneChangeListener>(listener));
        }
    }

    public void removeWatchZoneChangeListener(WatchZoneChangeListener listener) {
        if (listener != null) {
            WeakReference<WatchZoneChangeListener> toRemove = null;
            for (WeakReference<WatchZoneChangeListener> weakRef : mListeners) {
                WatchZoneChangeListener curListener = weakRef.get();
                if (curListener == listener) {
                    toRemove = weakRef;
                    break;
                }
            }
            mListeners.remove(toRemove);
        }
    }

    public void addWatchZoneProgressListener(WatchZoneUpdateManager.WatchZoneProgressListener listener) {
        WatchZoneUpdateManager.getInstance(mContext).addListener(listener);
    }

    public void removeWatchZoneProgressListener(WatchZoneUpdateManager.WatchZoneProgressListener listener) {
        WatchZoneUpdateManager.getInstance(mContext).removeListener(listener);
    }

    public WatchZone getWatchZone(Long createdTimestamp) {
        return mWatchZones.get(createdTimestamp);
    }

    public Set<Long> getWatchZones() {
        return mWatchZones.keySet();
    }

    public Set<Long> getUpdatingWatchZones() {
        return WatchZoneUpdateManager.getInstance(mContext).getUpdatingWatchZoneTimestamps();
    }

    public int getProgressForWatchZone(long createdTimestamp) {
        return WatchZoneUpdateManager.getInstance(mContext).getProgressForWatchZone(createdTimestamp);
    }

    /**
     * Create a new watch zone and start a new WatchZoneUpdateService for this watch zone. The watch zone will be
     * added to the WatchZoneManager asynchronously. If this function returns success, there will be a
     * subsequent call to onWatchZoneCreated(long timestamp).
     */
    public long createWatchZone(LatLng center, int radius) {
        long createdTimestamp = System.currentTimeMillis();
        long lastUpdatedTimestamp = 0;
        WatchZone watchZone = new WatchZone(createdTimestamp, lastUpdatedTimestamp, null, center, radius, null);
        boolean watchZoneCreated = mWatchZoneFileHelper.saveWatchZone(watchZone);

        if (watchZoneCreated) {
            WatchZoneUpdateManager watchZoneUpdateManager = WatchZoneUpdateManager.getInstance(mContext);
            watchZoneUpdateManager.updateWatchZone(watchZone);
            return createdTimestamp;
        } else {
            return 0;
        }
    }

    public boolean refreshWatchZone(long createdTimestamp) {
        if (mWatchZones.containsKey(createdTimestamp)) {
            WatchZoneUpdateManager.getInstance(mContext).updateWatchZone(mWatchZones.get(createdTimestamp));
            return true;
        } else {
            return false;
        }
    }

    public boolean updateWatchZone(long createdTimestamp, LatLng center, int radius) {
        if (mWatchZones.containsKey(createdTimestamp)) {
            WatchZone newWatchZone =
                    new WatchZone(createdTimestamp, System.currentTimeMillis(), null,
                            center, radius, null);
            boolean watchZoneCreated = mWatchZoneFileHelper.saveWatchZone(newWatchZone);
            if (watchZoneCreated) {
                return refreshWatchZone(createdTimestamp);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean deleteWatchZone(long createdTimestamp) {
        if (mWatchZones.containsKey(createdTimestamp)) {
            return mWatchZoneFileHelper.deleteWatchZone(mWatchZones.get(createdTimestamp));
        } else {
            return false;
        }
    }

    @Override
    public void onWatchZoneDeleted(long createdTimestamp) {
        mWatchZones.remove(createdTimestamp);
        notifyWatchZoneDeleted(createdTimestamp);
    }

    @Override
    public void onWatchZoneUpdated(long createdTimestamp) {
        WatchZone watchZone = mWatchZoneFileHelper.loadWatchZone(createdTimestamp);
        if (watchZone != null) {
            boolean newWatchZone = false;
            if (!mWatchZones.containsKey(createdTimestamp)) {
                newWatchZone = true;
            }
            mWatchZones.put(createdTimestamp, watchZone);
            if (newWatchZone) {
                notifyWatchZoneAdded(createdTimestamp);
            } else {
                notifyWatchZoneUpdated(createdTimestamp);
            }
        } else {
            mWatchZones.remove(createdTimestamp);
            notifyWatchZoneDeleted(createdTimestamp);
        }
    }

    private void notifyWatchZoneUpdated(long createdTimestamp) {
        for (WeakReference<WatchZoneChangeListener> weakRef : mListeners) {
            WatchZoneChangeListener listener = weakRef.get();
            if (listener != null) {
                listener.onWatchZoneUpdated(createdTimestamp);
            } else {
                mListeners.remove(weakRef);
            }
        }
    }

    private void notifyWatchZoneAdded(long createdTimestamp) {
        for (WeakReference<WatchZoneChangeListener> weakRef : mListeners) {
            WatchZoneChangeListener listener = weakRef.get();
            if (listener != null) {
                listener.onWatchZoneCreated(createdTimestamp);
            } else {
                mListeners.remove(weakRef);
            }
        }
    }

    private void notifyWatchZoneDeleted(long createdTimestamp) {
        for (WeakReference<WatchZoneChangeListener> weakRef : mListeners) {
            WatchZoneChangeListener listener = weakRef.get();
            if (listener != null) {
                listener.onWatchZoneDeleted(createdTimestamp);
            } else {
                mListeners.remove(weakRef);
            }
        }
    }
}
