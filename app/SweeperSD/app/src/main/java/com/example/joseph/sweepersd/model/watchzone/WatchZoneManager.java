package com.example.joseph.sweepersd.model.watchzone;

import android.content.Context;

import com.google.android.gms.maps.model.LatLng;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by joseph on 6/12/16.
 */
public class WatchZoneManager implements WatchZoneFileHelper.AlarmUpdateListener {
    private static final String TAG = WatchZoneManager.class.getSimpleName();

    private final WatchZoneFileHelper mWatchZoneFileHelper;
    private final HashMap<Long, WatchZone> mAlarms;
    private final Context mContext;

    private Set<WeakReference<AlarmChangeListener>> mListeners = new HashSet<>();

    public interface AlarmChangeListener {
        void onAlarmUpdated(Long createdTimestamp);
        void onAlarmCreated(Long createdTimestamp);
        void onAlarmDeleted(Long createdTimestamp);
    }

    public WatchZoneManager(Context context) {
        mContext = context;
        mWatchZoneFileHelper = new WatchZoneFileHelper(mContext, this);

        List<WatchZone> watchZones = mWatchZoneFileHelper.loadAlarms();
        mAlarms = new HashMap<>();
        for (WatchZone watchZone : watchZones) {
            mAlarms.put(watchZone.getCreatedTimestamp(), watchZone);
        }
    }

    public void addAlarmChangeListener(AlarmChangeListener listener) {
        if (listener != null) {
            mListeners.add(new WeakReference<AlarmChangeListener>(listener));
        }
    }

    public void removeAlarmChangeListener(AlarmChangeListener listener) {
        if (listener != null) {
            WeakReference<AlarmChangeListener> toRemove = null;
            for (WeakReference<AlarmChangeListener> weakRef : mListeners) {
                AlarmChangeListener curListener = weakRef.get();
                if (curListener == listener) {
                    toRemove = weakRef;
                    break;
                }
            }
            mListeners.remove(toRemove);
        }
    }

    public void addAlarmProgressListener(WatchZoneUpdateManager.AlarmProgressListener listener) {
        WatchZoneUpdateManager.getInstance(mContext).addListener(listener);
    }

    public void removeAlarmProgressListener(WatchZoneUpdateManager.AlarmProgressListener listener) {
        WatchZoneUpdateManager.getInstance(mContext).removeListener(listener);
    }

    public WatchZone getAlarm(Long createdTimestamp) {
        return mAlarms.get(createdTimestamp);
    }

    public Set<Long> getAlarms() {
        return mAlarms.keySet();
    }

    public Set<Long> getUpdatingAlarms() {
        return WatchZoneUpdateManager.getInstance(mContext).getUpdatingAlarmTimestamps();
    }

    public int getProgressForAlarm(long createdTimestamp) {
        return WatchZoneUpdateManager.getInstance(mContext).getProgressForAlarm(createdTimestamp);
    }

    /**
     * Create a new alarm and start a new WatchZoneUpdateService for this alarm. The alarm will be
     * added to the WatchZoneManager asynchronously. If this function returns success, there will be a
     * subsequent call to onAlarmCreated(long timestamp).
     */
    public long createAlarm(LatLng center, int radius) {
        long createdTimestamp = System.currentTimeMillis();
        long lastUpdatedTimestamp = 0;
        WatchZone watchZone = new WatchZone(createdTimestamp, lastUpdatedTimestamp, null, center, radius, null);
        boolean alarmCreated = mWatchZoneFileHelper.saveAlarm(watchZone);

        if (alarmCreated) {
            WatchZoneUpdateManager watchZoneUpdateManager = WatchZoneUpdateManager.getInstance(mContext);
            watchZoneUpdateManager.updateAlarm(watchZone);
            return createdTimestamp;
        } else {
            return 0;
        }
    }

    public boolean refreshAlarm(long createdTimestamp) {
        if (mAlarms.containsKey(createdTimestamp)) {
            WatchZoneUpdateManager.getInstance(mContext).updateAlarm(mAlarms.get(createdTimestamp));
            return true;
        } else {
            return false;
        }
    }

    public boolean updateAlarm(long createdTimestamp, LatLng center, int radius) {
        if (mAlarms.containsKey(createdTimestamp)) {
            WatchZone newWatchZone =
                    new WatchZone(createdTimestamp, System.currentTimeMillis(), null,
                            center, radius, null);
            boolean alarmCreated = mWatchZoneFileHelper.saveAlarm(newWatchZone);
            if (alarmCreated) {
                return refreshAlarm(createdTimestamp);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean deleteAlarm(long createdTimestamp) {
        if (mAlarms.containsKey(createdTimestamp)) {
            return mWatchZoneFileHelper.deleteAlarm(mAlarms.get(createdTimestamp));
        } else {
            return false;
        }
    }

    @Override
    public void onAlarmDeleted(long createdTimestamp) {
        mAlarms.remove(createdTimestamp);
        notifyAlarmDeleted(createdTimestamp);
    }

    @Override
    public void onAlarmUpdated(long createdTimestamp) {
        WatchZone watchZone = mWatchZoneFileHelper.loadAlarm(createdTimestamp);
        if (watchZone != null) {
            boolean newAlarm = false;
            if (!mAlarms.containsKey(createdTimestamp)) {
                newAlarm = true;
            }
            mAlarms.put(createdTimestamp, watchZone);
            if (newAlarm) {
                notifyAlarmAdded(createdTimestamp);
            } else {
                notifyAlarmUpdated(createdTimestamp);
            }
        } else {
            mAlarms.remove(createdTimestamp);
            notifyAlarmDeleted(createdTimestamp);
        }
    }

    private void notifyAlarmUpdated(long createdTimestamp) {
        for (WeakReference<AlarmChangeListener> weakRef : mListeners) {
            AlarmChangeListener listener = weakRef.get();
            if (listener != null) {
                listener.onAlarmUpdated(createdTimestamp);
            } else {
                mListeners.remove(weakRef);
            }
        }
    }

    private void notifyAlarmAdded(long createdTimestamp) {
        for (WeakReference<AlarmChangeListener> weakRef : mListeners) {
            AlarmChangeListener listener = weakRef.get();
            if (listener != null) {
                listener.onAlarmCreated(createdTimestamp);
            } else {
                mListeners.remove(weakRef);
            }
        }
    }

    private void notifyAlarmDeleted(long createdTimestamp) {
        for (WeakReference<AlarmChangeListener> weakRef : mListeners) {
            AlarmChangeListener listener = weakRef.get();
            if (listener != null) {
                listener.onAlarmDeleted(createdTimestamp);
            } else {
                mListeners.remove(weakRef);
            }
        }
    }
}
