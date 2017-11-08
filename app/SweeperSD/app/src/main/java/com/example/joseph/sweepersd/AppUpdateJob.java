package com.example.joseph.sweepersd;

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

import com.example.joseph.sweepersd.alert.AlertManager;
import com.example.joseph.sweepersd.alert.geofence.WatchZoneFenceManager;
import com.example.joseph.sweepersd.limit.AddressValidatorManager;
import com.example.joseph.sweepersd.scheduling.ScheduleManager;
import com.example.joseph.sweepersd.utils.Jobs;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelUpdater;

import java.util.Map;

public class AppUpdateJob extends JobService implements LifecycleOwner {
    private static final String TAG = AppUpdateJob.class.getSimpleName();
    private static final long ONE_MINUTE = 1000L * 60L;

    private ServiceLifecycleDispatcher mDispatcher;

    public static void cancelJob(Context context) {
        /*Log.i(TAG, "Cancelling " + TAG);
        JobScheduler jobScheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        if (jobScheduler.getPendingJob(Jobs.APP_UPDATE_JOB) != null) {
            jobScheduler.cancel(Jobs.APP_UPDATE_JOB);
        }*/
    }

    public static void scheduleJob(Context context) {
        Log.i(TAG, "Scheduling " + TAG);
        JobScheduler jobScheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        /*if (jobScheduler.getPendingJob(Jobs.APP_UPDATE_JOB) != null) {
            return;
        }*/
        Log.i(TAG, "Building " + TAG);

        JobInfo.Builder builder = new JobInfo.Builder(Jobs.APP_UPDATE_JOB,
                new ComponentName(context, AppUpdateJob.class));
        builder.setMinimumLatency(0L);
        builder.setOverrideDeadline(0L);
        builder.setPersisted(true);
        builder.setBackoffCriteria(ONE_MINUTE, JobInfo.BACKOFF_POLICY_LINEAR);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        jobScheduler.schedule(builder.build());
    }

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        Log.i(TAG, "Starting " + TAG);

        mDispatcher = new ServiceLifecycleDispatcher(this);

        mDispatcher.onServicePreSuperOnCreate();
        mDispatcher.onServicePreSuperOnStart();

        WatchZoneModelUpdater.getInstance(this).observe(this,
                new Observer<Map<Long, Integer>>() {
            @Override
            public void onChanged(@Nullable Map<Long, Integer> progressMap) {
                if (progressMap.isEmpty()) {
                    Boolean addressValidatorBusy = AddressValidatorManager.getInstance(AppUpdateJob.this).getValue();
                    Boolean alertBusy = AlertManager.getInstance(AppUpdateJob.this).getValue();
                    Boolean scheduleBusy = ScheduleManager.getInstance(AppUpdateJob.this).getValue();
                    Boolean fencesBusy = WatchZoneFenceManager.getInstance(AppUpdateJob.this).getValue();
                    boolean busy = addressValidatorBusy == null ? true : addressValidatorBusy ||
                            alertBusy == null ? true : alertBusy ||
                            scheduleBusy == null ? true : scheduleBusy ||
                            fencesBusy == null ? true : fencesBusy;
                    if (!busy) {
                        Log.d(TAG, "Finishing from WZMU " + TAG);
                        finish(jobParameters);
                    }
                }
            }
        });
        AddressValidatorManager.getInstance(this).observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean working) {
                if (!working) {
                    Boolean alertBusy = AlertManager.getInstance(AppUpdateJob.this).getValue();
                    Boolean scheduleBusy = ScheduleManager.getInstance(AppUpdateJob.this).getValue();
                    Boolean fencesBusy = WatchZoneFenceManager.getInstance(AppUpdateJob.this).getValue();
                    boolean updaterBusy = WatchZoneModelUpdater.getInstance(AppUpdateJob.this).getValue() == null ? true :
                            !WatchZoneModelUpdater.getInstance(AppUpdateJob.this).getValue().isEmpty();
                    boolean busy = updaterBusy ||
                            alertBusy == null ? true : alertBusy ||
                            scheduleBusy == null ? true : scheduleBusy ||
                            fencesBusy == null ? true : fencesBusy;
                    if (!busy) {
                        Log.d(TAG, "Finishing from AVM " + TAG);
                        finish(jobParameters);
                    }
                }
            }
        });
        AlertManager.getInstance(this).observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean working) {
                if (!working) {
                    Boolean addressValidatorBusy = AddressValidatorManager.getInstance(AppUpdateJob.this).getValue();
                    Boolean scheduleBusy = ScheduleManager.getInstance(AppUpdateJob.this).getValue();
                    Boolean fencesBusy = WatchZoneFenceManager.getInstance(AppUpdateJob.this).getValue();
                    boolean updaterBusy = WatchZoneModelUpdater.getInstance(AppUpdateJob.this).getValue() == null ? true :
                            !WatchZoneModelUpdater.getInstance(AppUpdateJob.this).getValue().isEmpty();
                    boolean busy = addressValidatorBusy == null ? true : addressValidatorBusy ||
                            updaterBusy ||
                            scheduleBusy == null ? true : scheduleBusy ||
                            fencesBusy == null ? true : fencesBusy;
                    if (!busy) {
                        Log.d(TAG, "Finishing from AM " + TAG);
                        finish(jobParameters);
                    }
                }
            }
        });
        ScheduleManager.getInstance(this).observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean working) {
                if (!working) {
                    Boolean addressValidatorBusy = AddressValidatorManager.getInstance(AppUpdateJob.this).getValue();
                    Boolean alertBusy = AlertManager.getInstance(AppUpdateJob.this).getValue();
                    Boolean fencesBusy = WatchZoneFenceManager.getInstance(AppUpdateJob.this).getValue();
                    boolean updaterBusy = WatchZoneModelUpdater.getInstance(AppUpdateJob.this).getValue() == null ? true :
                            !WatchZoneModelUpdater.getInstance(AppUpdateJob.this).getValue().isEmpty();
                    boolean busy = addressValidatorBusy == null ? true : addressValidatorBusy ||
                            alertBusy == null ? true : alertBusy ||
                            updaterBusy ||
                            fencesBusy == null ? true : fencesBusy;
                    if (!busy) {
                        Log.d(TAG, "Finishing from SM " + TAG);
                        finish(jobParameters);
                    }
                }
            }
        });
        WatchZoneFenceManager.getInstance(this).observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean working) {
                if (!working) {
                    Boolean addressValidatorBusy = AddressValidatorManager.getInstance(AppUpdateJob.this).getValue();
                    Boolean alertBusy = AlertManager.getInstance(AppUpdateJob.this).getValue();
                    Boolean scheduleBusy = ScheduleManager.getInstance(AppUpdateJob.this).getValue();
                    boolean updaterBusy = WatchZoneModelUpdater.getInstance(AppUpdateJob.this).getValue() == null ? true :
                            !WatchZoneModelUpdater.getInstance(AppUpdateJob.this).getValue().isEmpty();
                    boolean busy = addressValidatorBusy == null ? true : addressValidatorBusy ||
                            alertBusy == null ? true : alertBusy ||
                            scheduleBusy == null ? true : scheduleBusy ||
                            updaterBusy;
                    if (!busy) {
                        Log.d(TAG, "Finishing from WZFM " + TAG);
                        finish(jobParameters);
                    }
                }
            }
        });

        // Signal that another thread is doing the work.
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.i(TAG, "Interrupting " + TAG);
        mDispatcher.onServicePreSuperOnDestroy();

        // Please reschedule
        return true;
    }

    @Override
    public Lifecycle getLifecycle() {
        return mDispatcher.getLifecycle();
    }

    private void finish(JobParameters jobParameters) {
        Log.d(TAG, "Finishing " + TAG);
        mDispatcher.onServicePreSuperOnDestroy();
        jobFinished(jobParameters, false);
    }
}
