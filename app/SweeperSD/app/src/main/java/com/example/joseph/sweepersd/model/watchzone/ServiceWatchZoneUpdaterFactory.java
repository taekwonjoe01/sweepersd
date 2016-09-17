package com.example.joseph.sweepersd.model.watchzone;

import android.content.Context;

/**
 * Created by joseph on 9/5/16.
 */
public class ServiceWatchZoneUpdaterFactory implements WatchZoneUpdateManager.AlarmUpdaterFactory {
    private final Context mContext;

    public ServiceWatchZoneUpdaterFactory(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public WatchZoneUpdateManager.AlarmUpdater createNewAlarmUpdater() {
        return new ServiceWatchZoneUpdater(mContext);
    }
}
