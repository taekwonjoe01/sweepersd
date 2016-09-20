package com.example.joseph.sweepersd.model.watchzone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by joseph on 9/17/16.
 */
public class WatchZoneAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent msgIntent = new Intent(context, WatchZoneNotificationDeliverer.class);
        msgIntent.setType(intent.getType());
        context.startService(msgIntent);
    }
}
