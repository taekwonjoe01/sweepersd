package com.example.joseph.sweepersd.model.alarms;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by joseph on 6/12/16.
 */
public class AlarmManager implements AlarmFileHelper.AlarmUpdateListener {
    private static final String TAG = AlarmManager.class.getSimpleName();

    private final AlarmFileHelper mAlarmFileHelper;
    private final HashMap<Long, Alarm> mAlarms;
    private final Context mContext;

    private Set<WeakReference<AlarmChangeListener>> mListeners = new HashSet<>();

    public interface AlarmChangeListener {
        void onAlarmUpdated(Long createdTimestamp);
        void onAlarmCreated(Long createdTimestamp);
        void onAlarmDeleted(Long createdTimestamp);
    }

    public AlarmManager(Context context) {
        mContext = context;
        mAlarmFileHelper = new AlarmFileHelper(mContext, this);

        List<Alarm> alarms = mAlarmFileHelper.loadAlarms();
        mAlarms = new HashMap<>();
        for (Alarm alarm : alarms) {
            Log.e("Joey", "size of sweepingaddresses " + alarm.getSweepingAddresses().size());
            mAlarms.put(alarm.getCreatedTimestamp(), alarm);
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

    public void addAlarmProgressListener(AlarmUpdateManager.AlarmProgressListener listener) {
        AlarmUpdateManager.getInstance(mContext).addListener(listener);
    }

    public void removeAlarmProgressListener(AlarmUpdateManager.AlarmProgressListener listener) {
        AlarmUpdateManager.getInstance(mContext).removeListener(listener);
    }

    public Alarm getAlarm(Long createdTimestamp) {
        return mAlarms.get(createdTimestamp);
    }

    public Set<Long> getAlarms() {
        return mAlarms.keySet();
    }

    public Set<Long> getUpdatingAlarms() {
        return AlarmUpdateManager.getInstance(mContext).getUpdatingAlarmTimestamps();
    }

    public int getProgressForAlarm(long createdTimestamp) {
        return AlarmUpdateManager.getInstance(mContext).getProgressForAlarm(createdTimestamp);
    }

    /**
     * Create a new alarm and start a new AlarmUpdateService for this alarm. The alarm will be
     * added to the AlarmManager asynchronously. If this function returns success, there will be a
     * subsequent call to onAlarmCreated(long timestamp).
     */
    public long createAlarm(LatLng center, int radius) {
        long createdTimestamp = System.currentTimeMillis();
        long lastUpdatedTimestamp = 0;
        Alarm alarm = new Alarm(createdTimestamp, lastUpdatedTimestamp, null, center, radius, null);
        boolean alarmCreated = mAlarmFileHelper.saveAlarm(alarm);

        if (alarmCreated) {
            AlarmUpdateManager alarmUpdateManager = AlarmUpdateManager.getInstance(mContext);
            alarmUpdateManager.updateAlarm(alarm);
            return createdTimestamp;
        } else {
            return 0;
        }
    }

    public boolean refreshAlarm(long createdTimestamp) {
        if (mAlarms.containsKey(createdTimestamp)) {
            AlarmUpdateManager.getInstance(mContext).updateAlarm(mAlarms.get(createdTimestamp));
            return true;
        } else {
            return false;
        }
    }

    public boolean updateAlarm(long createdTimestamp, LatLng center, int radius) {
        if (mAlarms.containsKey(createdTimestamp)) {
            Alarm newAlarm =
                    new Alarm(createdTimestamp, System.currentTimeMillis(), null,
                            center, radius, null);
            boolean alarmCreated = mAlarmFileHelper.saveAlarm(newAlarm);
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
            return mAlarmFileHelper.deleteAlarm(mAlarms.get(createdTimestamp));
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
        Alarm alarm = mAlarmFileHelper.loadAlarm(createdTimestamp);
        if (alarm != null) {
            boolean newAlarm = false;
            if (!mAlarms.containsKey(createdTimestamp)) {
                newAlarm = true;
            }
            mAlarms.put(createdTimestamp, alarm);
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
