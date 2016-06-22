package com.example.joseph.sweepersd.alarms;

import java.util.List;

/**
 * Created by joseph on 6/12/16.
 */
public class AlarmModel {
    private final List<Alarm> mAlarms;

    public AlarmModel(List<Alarm> alarms) {
        mAlarms = alarms;
    }

    public List<Alarm> getAlarms() {
        return mAlarms;
    }

    public void addAlarm(Alarm alarm) {
        if (alarm != null) {
            mAlarms.add(alarm);
        }
    }

    public void removeAlarm(Alarm alarm) {
        if (alarm != null) {
            mAlarms.remove(alarm);
        }
    }
}
