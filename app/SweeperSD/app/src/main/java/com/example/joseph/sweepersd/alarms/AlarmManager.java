package com.example.joseph.sweepersd.alarms;

import android.content.Context;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by joseph on 6/12/16.
 */
public class AlarmManager implements AlarmHelper.AlarmUpdateListener {
    private static final String KEY_NUM_ALARMS = "NUM_ALARMS";
    private static final String KEY_ALARM_PREFIX = "ALARM_";

    private final AlarmHelper mAlarmHelper;
    private final HashMap<Long, Alarm> mAlarms;
    private final Context mContext;

    private Set<WeakReference<AlarmManagerListener>> mListeners = new HashSet<>();

    public interface AlarmManagerListener {
        void onAlarmsUpdated(List<Alarm> alarms);
    }

    public AlarmManager(Context context) {
        mContext = context;
        mAlarmHelper = new AlarmHelper(mContext, this);

        List<Alarm> alarms = mAlarmHelper.loadAlarms();
        mAlarms = new HashMap<>();
        for (Alarm alarm : alarms) {
            mAlarms.put(alarm.getCreatedTimestamp(), alarm);
        }
    }

    public void addListener(AlarmManagerListener listener) {
        if (listener != null) {
            mListeners.add(new WeakReference<AlarmManagerListener>(listener));
        }
    }

    public void removeListener(AlarmManagerListener listener) {
        if (listener != null) {
            WeakReference<AlarmManagerListener> toRemove = null;
            for (WeakReference<AlarmManagerListener> weakRef : mListeners) {
                AlarmManagerListener curListener = weakRef.get();
                if (curListener == listener) {
                    toRemove = weakRef;
                    break;
                }
            }
            mListeners.remove(toRemove);
        }
    }

    public List<Alarm> getAlarms() {
        return new ArrayList<>(mAlarms.values());
    }

    public void saveAlarm(Alarm alarm) {
        if (alarm != null) {
            mAlarmHelper.saveAlarm(alarm);
        }
    }

    public void removeAlarm(Alarm alarm) {
        if (alarm != null) {
            mAlarmHelper.deleteAlarm(alarm);
        }
    }

    @Override
    public void onAlarmDeleted(long createdTimestamp) {
        mAlarms.remove(createdTimestamp);
        notifyAlarmsUpdated();
    }

    @Override
    public void onAlarmUpdated(long createdTimestamp) {
        Alarm alarm = mAlarmHelper.loadAlarm(createdTimestamp);
        if (alarm != null) {
            mAlarms.put(createdTimestamp, alarm);
        } else {
            mAlarms.remove(createdTimestamp);
        }
        notifyAlarmsUpdated();
    }

    private void notifyAlarmsUpdated() {
        for (WeakReference<AlarmManagerListener> weakRef : mListeners) {
            AlarmManagerListener listener = weakRef.get();
            if (listener != null) {
                listener.onAlarmsUpdated(getAlarms());
            } else {
                mListeners.remove(weakRef);
            }
        }
    }
}
