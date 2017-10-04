package com.example.joseph.sweepersd.revision3.watchzone;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ServiceLifecycleDispatcher;
import android.content.ComponentName;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Map;

public class WatchZoneUpdateJob extends JobService implements LifecycleOwner {
    private static final int WATCH_ZONE_UPDATE_FOREGROUND_JOB_ID = 100;
    private static final int WATCH_ZONE_UPDATE_BACKGROUND_JOB_ID = 101;

    private static final int ONE_DAY = 1000 * 60 * 60 * 24;
    private static final int ONE_MINUTE = 1000 * 60;
    private static final int ONE_HOUR = 1000 * 60 * 60;

    private WatchZoneModelUpdater mWatchZoneModelUpdater;
    private ServiceLifecycleDispatcher mDispatcher;

    public static void scheduleAppForegroundJob(Context context) {
        scheduleAppBackgroundJob(context);

        JobScheduler jobScheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        if (jobScheduler.getPendingJob(WATCH_ZONE_UPDATE_FOREGROUND_JOB_ID) != null) {
            return;
        }

                JobInfo.Builder builder = new JobInfo.Builder(WATCH_ZONE_UPDATE_FOREGROUND_JOB_ID,
                new ComponentName(context, WatchZoneUpdateJob.class));
        builder.setMinimumLatency(0L);
        builder.setOverrideDeadline(0L);
        builder.setBackoffCriteria(ONE_MINUTE, JobInfo.BACKOFF_POLICY_LINEAR);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        jobScheduler.schedule(builder.build());
    }

    public static void scheduleAppBackgroundJob(Context context) {
        JobScheduler jobScheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        if (jobScheduler.getPendingJob(WATCH_ZONE_UPDATE_BACKGROUND_JOB_ID) != null) {
            return;
        }

        JobInfo.Builder builder = new JobInfo.Builder(WATCH_ZONE_UPDATE_BACKGROUND_JOB_ID,
                new ComponentName(context, WatchZoneUpdateJob.class));
        builder.setPeriodic(ONE_DAY);
        builder.setPersisted(true);
        builder.setBackoffCriteria(ONE_HOUR, JobInfo.BACKOFF_POLICY_EXPONENTIAL);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        jobScheduler.schedule(builder.build());
    }

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        Log.e("Joey", "Job Starting");
        mWatchZoneModelUpdater = WatchZoneModelUpdater.getInstance(this);
        mDispatcher = new ServiceLifecycleDispatcher(this);

        mDispatcher.onServicePreSuperOnCreate();
        mDispatcher.onServicePreSuperOnStart();
        mWatchZoneModelUpdater.observe(this, new Observer<Map<Long, Integer>>() {
            @Override
            public void onChanged(@Nullable Map<Long, Integer> longIntegerMap) {
                if (longIntegerMap != null) {
                    if (longIntegerMap.isEmpty()) {
                        Log.e("Joey", "Job Finished");
                        jobFinished(jobParameters, false);
                    }
                }
            }
        });
        // Signal that another thread is doing the work.
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        mDispatcher.onServicePreSuperOnDestroy();

        Log.e("Joey", "Job Terminated");
        return true;
    }

    @Override
    public Lifecycle getLifecycle() {
        return mDispatcher.getLifecycle();
    }
}
