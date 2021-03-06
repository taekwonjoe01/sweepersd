package com.example.joseph.sweepersd.scheduling;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.joseph.sweepersd.utils.PendingIntents;
import com.example.joseph.sweepersd.utils.Preferences;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneUtils;

import java.util.Date;
import java.util.List;

public class ScheduleUpdater {
    private static final String TAG = ScheduleUpdater.class.getSimpleName();

    private final Context mApplicationContext;

    public ScheduleUpdater(Context context) {
        mApplicationContext = context.getApplicationContext();
    }

    public long scheduleWatchZones(List<WatchZoneModel> watchWatchZoneModels) {
        long nextEventTimestamp = -1L;
        for (WatchZoneModel model : watchWatchZoneModels) {
            long nextTimestamp = WatchZoneUtils.getNextEventTimestampForWatchZone(model);
            if (nextTimestamp != -1L) {
                if (nextEventTimestamp == -1L || nextTimestamp < nextEventTimestamp) {
                    nextEventTimestamp = nextTimestamp;
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

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mApplicationContext);
        preferences.edit().putLong(Preferences.PREFERENCE_ALARM_SCHEDULED_FOR,
                timestamp).apply();
    }
}
