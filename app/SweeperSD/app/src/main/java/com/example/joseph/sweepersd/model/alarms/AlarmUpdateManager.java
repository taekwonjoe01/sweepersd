package com.example.joseph.sweepersd.model.alarms;

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
public class AlarmUpdateManager {
    public static final int INVALID_PROGRESS = -1;
    private static AlarmUpdateManager sInstance;

    private final Context mContext;
    private HashMap<Long, AlarmUpdater> mAlarmStatuses = new HashMap<>();
    private AlarmUpdaterFactory mAlarmUpdaterFactory;

    private Set<WeakReference<AlarmProgressListener>> mListeners = new HashSet<>();

    interface AlarmUpdaterFactory {
        AlarmUpdater createNewAlarmUpdater();
    }

    interface AlarmUpdater {
        void updateAlarm(Alarm alarm, AlarmProgressListener listener);
        int getProgress();
    }

    public interface AlarmProgressListener {
        void onAlarmUpdateProgress(long createdTimestamp, int progress);
        void onAlarmUpdateComplete(long createdTimestamp);
    }

    private AlarmUpdateManager(Context context) {
        mContext = context;

        mAlarmUpdaterFactory = new ServiceAlarmUpdaterFactory(mContext);
    }

    /**
     * For test purposes only
     */
    void setAlarmUpdaterFactory(AlarmUpdaterFactory factory) {
        if (factory == null) {
            mAlarmUpdaterFactory = new ServiceAlarmUpdaterFactory(mContext);
        } else {
            mAlarmUpdaterFactory = factory;
        }
    }

    public void addListener(AlarmProgressListener listener) {
        if (listener != null) {
            mListeners.add(new WeakReference<>(listener));
        }
    }

    public void removeListener(AlarmProgressListener listener) {
        if (listener != null) {
            WeakReference<AlarmProgressListener> toRemove = null;
            for (WeakReference<AlarmProgressListener> weakRef : mListeners) {
                AlarmProgressListener curListener = weakRef.get();
                if (curListener == listener) {
                    toRemove = weakRef;
                    break;
                }
            }
            mListeners.remove(toRemove);
        }
    }

    public static AlarmUpdateManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AlarmUpdateManager(context.getApplicationContext());
        }
        return sInstance;
    }

    public boolean updateAlarm(Alarm alarm) {
        if (mAlarmStatuses.containsKey(alarm.getCreatedTimestamp())) {
            return false;
        } else {
            AlarmUpdater updater = mAlarmUpdaterFactory.createNewAlarmUpdater();
            mAlarmStatuses.put(alarm.getCreatedTimestamp(), updater);
            updater.updateAlarm(alarm, mProgressListener);
            return true;
        }
    }

    public Set<Long> getUpdatingAlarmTimestamps() {
        return mAlarmStatuses.keySet();
    }

    public int getProgressForAlarm(long createdTimestamp) {
        if (mAlarmStatuses.containsKey(createdTimestamp)) {
            return mAlarmStatuses.get(createdTimestamp).getProgress();
        } else {
            return INVALID_PROGRESS;
        }
    }

    private void notifyProgress(long id, int progress) {
        List<WeakReference> toRemove = new ArrayList<>();
        List<AlarmProgressListener> listeners = new ArrayList<>();
        for (WeakReference<AlarmProgressListener> weakRef : mListeners) {
            AlarmProgressListener curListener = weakRef.get();
            if (curListener == null) {
                toRemove.add(weakRef);
            } else {
                listeners.add(curListener);
            }
        }

        for (AlarmProgressListener listener : listeners) {
            listener.onAlarmUpdateProgress(id, progress);
        }

        for (WeakReference removeMe : toRemove) {
            mListeners.remove(removeMe);
        }

        mListeners.remove(toRemove);
    }

    private void notifyComplete(long id) {
        List<WeakReference> toRemove = new ArrayList<>();
        List<AlarmProgressListener> listeners = new ArrayList<>();
        for (WeakReference<AlarmProgressListener> weakRef : mListeners) {
            AlarmProgressListener curListener = weakRef.get();
            if (curListener == null) {
                toRemove.add(weakRef);
            } else {
                listeners.add(curListener);
            }
        }

        for (AlarmProgressListener listener : listeners) {
            listener.onAlarmUpdateComplete(id);
        }

        for (WeakReference removeMe : toRemove) {
            mListeners.remove(removeMe);
        }

        mListeners.remove(toRemove);
    }

    private final AlarmProgressListener mProgressListener = new AlarmProgressListener() {
        @Override
        public void onAlarmUpdateProgress(long createdTimestamp, int progress) {
            notifyProgress(createdTimestamp, progress);
        }

        @Override
        public void onAlarmUpdateComplete(long createdTimestamp) {
            notifyComplete(createdTimestamp);
            mAlarmStatuses.remove(createdTimestamp);
        }
    };
}
