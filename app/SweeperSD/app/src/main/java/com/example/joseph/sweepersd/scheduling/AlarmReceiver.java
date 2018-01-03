package com.example.joseph.sweepersd.scheduling;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.joseph.sweepersd.AppUpdateJob;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        LastAlarm.getInstance().postAlarm();
        // Wake up the app!
        AppUpdateJob.scheduleJob(context);
    }
}
