package com.example.joseph.sweepersd.scheduling;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.joseph.sweepersd.utils.PendingIntents;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneUtils;

import java.util.Date;
import java.util.List;

public class ScheduleManager {
    private static final String TAG = ScheduleManager.class.getSimpleName();

    private final Context mApplicationContext;

    public ScheduleManager(Context context) {
        mApplicationContext = context.getApplicationContext();
    }

    public long scheduleWatchZones(List<WatchZoneModel> watchZoneModels) {
        long nextEventTimestamp = -1L;
        for (WatchZoneModel model : watchZoneModels) {
            if (model.getStatus() == WatchZoneModel.Status.VALID) {
                long nextTimestamp = WatchZoneUtils.getNextEventTimestampForWatchZone(model);
                if (nextTimestamp != -1L) {
                    if (nextEventTimestamp == -1L || nextTimestamp < nextEventTimestamp) {
                        nextEventTimestamp = nextTimestamp;
                    }
                }
            }
        }
        if (nextEventTimestamp != -1L) {
            scheduleAlarm(nextEventTimestamp);
        }
        return nextEventTimestamp;
    }

    private void scheduleAlarm(long timestamp) {
        AlarmManager alarmMgr;
        PendingIntent alarmIntent;

        alarmMgr = (AlarmManager) mApplicationContext.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(mApplicationContext, AlarmReceiver.class);
        alarmIntent = PendingIntent.getBroadcast(mApplicationContext, PendingIntents.REQUEST_CODE_ALARM, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        alarmMgr.set(AlarmManager.RTC_WAKEUP, timestamp, alarmIntent);
        Log.i(TAG, "Alarm scheduled for " + new Date(timestamp).toString());
    }
}
