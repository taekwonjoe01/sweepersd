package com.example.joseph.sweepersd.revision3.watchzone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.joseph.sweepersd.revision3.watchzone.WatchZoneNotificationJob;

public class WatchZoneAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        long uid = Long.parseLong(intent.getType());
        WatchZoneNotificationJob.scheduleJob(context, uid);
    }
}
