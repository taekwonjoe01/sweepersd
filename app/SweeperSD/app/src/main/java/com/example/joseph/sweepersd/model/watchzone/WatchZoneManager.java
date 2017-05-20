package com.example.joseph.sweepersd.model.watchzone;

import android.content.Context;

import com.google.android.gms.maps.model.LatLng;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
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
        void onWatchZoneUpdated(long createdTimestamp);
        void onWatchZoneCreated(long createdTimestamp);
        void onWatchZoneDeleted(long createdTimestamp);
    }

    public WatchZoneManager(Context context) {
        mContext = context;
        mWatchZoneFileHelper = new WatchZoneFileHelper(mContext, this);

        List<Long> watchZones = mWatchZoneFileHelper.getWatchZoneList();

        /*if (watchZones.isEmpty()) {
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
            watchZones.add((long)666);
        }*/
        mWatchZones = new HashMap<>();
        for (Long watchZone : watchZones) {
            mWatchZones.put(watchZone, null);
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

    public WatchZone getWatchZoneBrief(long createdTimestamp) {
        WatchZone result = null;
        if (mWatchZones.containsKey(createdTimestamp)) {
            WatchZone zone = mWatchZones.get(createdTimestamp);
            if (zone == null) {
                zone = mWatchZoneFileHelper.loadWatchZoneBrief(createdTimestamp);
            }
            result = zone;
        }
        return result;
    }

    public WatchZone getWatchZoneComplete(long createdTimestamp) {
        WatchZone result = null;
        if (mWatchZones.containsKey(createdTimestamp)) {
            WatchZone zone = mWatchZones.get(createdTimestamp);
            if (zone == null) {
                zone = mWatchZoneFileHelper.loadWatchZone(createdTimestamp);
                mWatchZones.put(createdTimestamp, zone);
            }
            result = zone;
        }
        return result;
    }

    public List<Long> getWatchZones() {
        List<Long> results = new ArrayList<>(mWatchZones.keySet());
        Collections.sort(results);
        return results;
    }

    public List<Long> getUpdatingWatchZones() {
        List<Long> results = new ArrayList<>(
                WatchZoneUpdateManager.getInstance(mContext).getUpdatingWatchZoneTimestamps());
        Collections.sort(results);
        return results;
    }

    public int getProgressForWatchZone(long createdTimestamp) {
        return WatchZoneUpdateManager.getInstance(mContext).getProgressForWatchZone(createdTimestamp);
    }

    /**
     * Create a new watch zone and start a new WatchZoneUpdateService for this watch zone. The watch zone will be
     * added to the WatchZoneManager asynchronously. If this function returns success, there will be a
     * subsequent call to onWatchZoneCreated(long timestamp).
     */
    public long createWatchZone(String label, LatLng center, int radius) {
        long createdTimestamp = System.currentTimeMillis();
        long lastUpdatedTimestamp = 0;
        WatchZone watchZone = new WatchZone(createdTimestamp, lastUpdatedTimestamp, label, center,
                radius, null);
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

    public boolean updateWatchZone(WatchZone watchZoneToUpdate) {
        if (mWatchZones.containsKey(watchZoneToUpdate.getCreatedTimestamp())) {
            WatchZone newWatchZone =
                    new WatchZone(watchZoneToUpdate.getCreatedTimestamp(),
                            System.currentTimeMillis(), watchZoneToUpdate.getLabel(),
                            watchZoneToUpdate.getCenter(),
                            watchZoneToUpdate.getRadius(), null);
            boolean watchZoneUpdated = mWatchZoneFileHelper.saveWatchZone(newWatchZone);
            if (watchZoneUpdated) {
                return refreshWatchZone(watchZoneToUpdate.getCreatedTimestamp());
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean deleteWatchZone(long createdTimestamp) {
        if (mWatchZones.containsKey(createdTimestamp)) {
            return mWatchZoneFileHelper.deleteWatchZone(createdTimestamp);
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
