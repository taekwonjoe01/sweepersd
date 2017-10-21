package com.example.joseph.sweepersd.scheduling;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.joseph.sweepersd.alert.AlertNotificationJob;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Wake up the app!
        ScheduleJob.scheduleJob(context);
        AlertNotificationJob.scheduleJob(context);
    }
}
