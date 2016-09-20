package com.example.joseph.sweepersd.model.watchzone;

import android.content.Context;

/**
 * Created by joseph on 9/5/16.
 */
public class ServiceWatchZoneUpdaterFactory implements WatchZoneUpdateManager.WatchZoneUpdaterFactory {
    private final Context mContext;

    public ServiceWatchZoneUpdaterFactory(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public WatchZoneUpdateManager.WatchZoneUpdater createNewWatchZoneUpdater() {
        return new ServiceWatchZoneUpdater(mContext);
    }
}
