package com.example.joseph.sweepersd.alarms;

import android.content.Context;

import com.example.joseph.sweepersd.utils.AlarmHelper;

import java.util.List;

/**
 * Created by joseph on 6/12/16.
 */
public class AlarmModel {
    private static final String KEY_NUM_ALARMS = "NUM_ALARMS";
    private static final String KEY_ALARM_PREFIX = "ALARM_";
    private final List<Alarm> mAlarms;
    private final Context mContext;

    public AlarmModel(Context context) {
        mContext = context;

        mAlarms = AlarmHelper.loadAlarms(context);
    }

    public List<Alarm> getAlarms() {
        return mAlarms;
    }

    public void saveAlarm(Alarm alarm) {
        if (alarm != null) {
            if (!mAlarms.contains(alarm)) {
                mAlarms.add(alarm);
            }
            AlarmHelper.saveAlarm(mContext, alarm);
        }
    }

    public void removeAlarm(Alarm alarm) {
        if (alarm != null) {
            mAlarms.remove(alarm);
            AlarmHelper.deleteAlarm(mContext, alarm);
        }
    }
}
