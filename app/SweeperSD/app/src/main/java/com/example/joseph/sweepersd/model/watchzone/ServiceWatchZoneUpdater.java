package com.example.joseph.sweepersd.model.watchzone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * Created by joseph on 9/5/16.
 */
public class ServiceWatchZoneUpdater implements WatchZoneUpdateManager.AlarmUpdater {
    private final Context mContext;

    private WatchZone mWatchZone;
    private int mProgress;
    private WatchZoneUpdateManager.AlarmProgressListener mListener;

    public ServiceWatchZoneUpdater(Context context) {
        mContext = context.getApplicationContext();

        IntentFilter filter = new IntentFilter();
        filter.addAction(WatchZoneUpdateService.ACTION_ALARM_PROGRESS);
        filter.addAction(WatchZoneUpdateService.ACTION_ALARM_FINISHED);
        mContext.registerReceiver(mAlarmUpdateReceiver, filter);
    }

    @Override
    public void updateAlarm(WatchZone watchZone, WatchZoneUpdateManager.AlarmProgressListener listener) {
        mWatchZone = watchZone;
        mListener = listener;

        Intent msgIntent = new Intent(mContext, WatchZoneUpdateService.class);
        msgIntent.putExtra(WatchZoneUpdateService.PARAM_ALARM_ID, watchZone.getCreatedTimestamp());

        mContext.startService(msgIntent);

        mProgress = 0;
    }

    @Override
    public int getProgress() {
        return mProgress;
    }

    private final BroadcastReceiver mAlarmUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(WatchZoneUpdateService.PARAM_ALARM_ID, 0);
            if (id == mWatchZone.getCreatedTimestamp()) {
                switch (intent.getAction()) {
                    case WatchZoneUpdateService.ACTION_ALARM_PROGRESS:
                        int progress = intent.getIntExtra(WatchZoneUpdateService.PARAM_PROGRESS, 0);
                        mProgress = progress;
                        mListener.onAlarmUpdateProgress(mWatchZone.getCreatedTimestamp(), progress);
                        break;
                    case WatchZoneUpdateService.ACTION_ALARM_FINISHED:
                        mListener.onAlarmUpdateComplete(mWatchZone.getCreatedTimestamp());
                        mContext.unregisterReceiver(this);
                        break;
                    default:
                }
            }
        }
    };
}
