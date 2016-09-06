package com.example.joseph.sweepersd.model.alarms;

import android.content.Context;

/**
 * Created by joseph on 9/5/16.
 */
public class ServiceAlarmUpdaterFactory implements AlarmUpdateManager.AlarmUpdaterFactory {
    private final Context mContext;

    public ServiceAlarmUpdaterFactory(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public AlarmUpdateManager.AlarmUpdater createNewAlarmUpdater() {
        return new ServiceAlarmUpdater(mContext);
    }
}
