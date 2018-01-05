package com.example.joseph.sweepersd;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * This BootupReceiver exists because there is a bug with the JobService.setPersisted method.
 * setPersisted(true) is expected to start the job on bootup, however it does not occur. setPersisted
 * only seems to be working for when the app is "swipe killed". Anyway, workaround is easy, just
 * use this explicit Bootup receiver to launch the job.
 */
public class BootupReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        AppUpdateJob.scheduleJob(context);
    }
}
