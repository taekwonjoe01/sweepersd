package com.example.joseph.sweepersd.model.alarms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * Created by joseph on 9/5/16.
 */
public class ServiceAlarmUpdater implements AlarmUpdateManager.AlarmUpdater {
    private final Context mContext;

    private Alarm mAlarm;
    private int mProgress;
    private AlarmUpdateManager.AlarmProgressListener mListener;

    public ServiceAlarmUpdater(Context context) {
        mContext = context.getApplicationContext();

        IntentFilter filter = new IntentFilter();
        filter.addAction(AlarmUpdateService.ACTION_ALARM_PROGRESS);
        filter.addAction(AlarmUpdateService.ACTION_ALARM_FINISHED);
        mContext.registerReceiver(mAlarmUpdateReceiver, filter);
    }

    @Override
    public void updateAlarm(Alarm alarm, AlarmUpdateManager.AlarmProgressListener listener) {
        mAlarm = alarm;
        mListener = listener;

        Intent msgIntent = new Intent(mContext, AlarmUpdateService.class);
        msgIntent.putExtra(AlarmUpdateService.PARAM_ALARM_ID, alarm.getCreatedTimestamp());

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
            long id = intent.getLongExtra(AlarmUpdateService.PARAM_ALARM_ID, 0);
            if (id == mAlarm.getCreatedTimestamp()) {
                switch (intent.getAction()) {
                    case AlarmUpdateService.ACTION_ALARM_PROGRESS:
                        int progress = intent.getIntExtra(AlarmUpdateService.PARAM_PROGRESS, 0);
                        mProgress = progress;
                        mListener.onAlarmUpdateProgress(mAlarm.getCreatedTimestamp(), progress);
                        break;
                    case AlarmUpdateService.ACTION_ALARM_FINISHED:
                        mListener.onAlarmUpdateComplete(mAlarm.getCreatedTimestamp());
                        mContext.unregisterReceiver(this);
                        break;
                    default:
                }
            }
        }
    };
}
