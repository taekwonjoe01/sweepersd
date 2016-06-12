package com.example.joseph.sweepersd.alarms;

import android.content.Context;
import android.location.Location;

import com.example.joseph.sweepersd.LocationDetails;

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

    public Alarm createAndAddAlarm(Context context, Location location, int radius) {
        LocationDetails locationDetails = LocationDetails.createFromLocation(context, location);
        Alarm alarm = new Alarm(locationDetails);
        mAlarms.add(alarm);
        return alarm;
    }

    public void removeAlarm(Alarm alarm) {
        if (alarm != null) {
            mAlarms.remove(alarm);
        }
    }
}
