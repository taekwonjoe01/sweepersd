package com.example.joseph.sweepersd;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.example.joseph.sweepersd.model.watchzone.WatchZone;
import com.example.joseph.sweepersd.model.watchzone.WatchZoneManager;
import com.example.joseph.sweepersd.model.watchzone.WatchZoneUtils;

/**
 * Created by joseph on 9/20/16.
 */
public class BootupSchedulerService extends IntentService {
    private static final String TAG = BootupSchedulerService.class.getSimpleName();

    public BootupSchedulerService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "Starting " + TAG);

        WatchZoneManager manager = new WatchZoneManager(this);
        for (long timestamp : manager.getWatchZones()) {
            WatchZone watchZone = manager.getWatchZoneComplete(timestamp);

            if (watchZone != null) {
                WatchZoneUtils.scheduleWatchZoneAlarm(this, watchZone);
            }
        }
    }
}
