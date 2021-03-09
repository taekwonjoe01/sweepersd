package com.example.joseph.sweepersd;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ServiceLifecycleDispatcher;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;

import com.example.joseph.sweepersd.alert.AlertManager;
import com.example.joseph.sweepersd.alert.geofence.WatchZoneFenceManager;
import com.example.joseph.sweepersd.limit.AddressValidatorManager;
import com.example.joseph.sweepersd.scheduling.ScheduleManager;
import com.example.joseph.sweepersd.utils.Jobs;
import com.example.joseph.sweepersd.utils.Preferences;
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
        JobScheduler jobScheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        JobInfo.Builder builder = new JobInfo.Builder(Jobs.APP_UPDATE_JOB,
                new ComponentName(context, AppUpdateJob.class));
        builder.setMinimumLatency(0L);
        builder.setOverrideDeadline(0L);
        // Apparently this only works for killed applications and does not work across a manual device
        // reboot. Implementing a manual BroadcastReceiver that listens to onBootCompleted and launches
        // this Job.
        builder.setPersisted(true);
        builder.setBackoffCriteria(ONE_MINUTE, JobInfo.BACKOFF_POLICY_LINEAR);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        jobScheduler.schedule(builder.build());
    }

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putLong(Preferences.PREFERENCE_APP_UPDATER_LAST_STARTED,
                System.currentTimeMillis()).apply();

        mDispatcher = new ServiceLifecycleDispatcher(this);

        mDispatcher.onServicePreSuperOnCreate();
        mDispatcher.onServicePreSuperOnStart();

        WatchZoneModelUpdater.getInstance(this).observe(this,
                new Observer<Map<Long, Integer>>() {
            @Override
            public void onChanged(@Nullable Map<Long, Integer> progressMap) {
                if (progressMap != null && progressMap.isEmpty()) {
                    if (!isBusy()) {
                        finish(preferences, jobParameters);
                    }
                }
            }
        });
        AddressValidatorManager.getInstance(this).observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean working) {
                if (working != null && !working) {
                    if (!isBusy()) {
                        finish(preferences, jobParameters);
                    }
                }
            }
        });
        AlertManager.getInstance(this).observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean working) {
                if (working != null && !working) {
                    if (!isBusy()) {
                        finish(preferences, jobParameters);
                    }
                }
            }
        });
        ScheduleManager.getInstance(this).observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean working) {
                if (working != null && !working) {
                    if (!isBusy()) {
                        finish(preferences, jobParameters);
                    }
                }
            }
        });
        WatchZoneFenceManager.getInstance(this).observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean working) {
                if (working != null && !working) {
                    if (!isBusy()) {
                        finish(preferences, jobParameters);
                    }
                }
            }
        });

        // Signal that another thread is doing the work.
        return true;
    }

    private boolean isBusy() {
        Boolean addressValidatorBusy = AddressValidatorManager.getInstance(AppUpdateJob.this).getValue();
        Boolean alertBusy = AlertManager.getInstance(AppUpdateJob.this).getValue();
        Boolean scheduleBusy = ScheduleManager.getInstance(AppUpdateJob.this).getValue();
        Map<Long, Integer> progress = WatchZoneModelUpdater.getInstance(AppUpdateJob.this).getValue();
        Boolean fencesBusy = WatchZoneFenceManager.getInstance(AppUpdateJob.this).getValue();

        /*String log = "Following are still busy: ";
        if (addressValidatorBusy == null || addressValidatorBusy) {
            log += "AV ";
        }
        if (alertBusy == null || alertBusy) {
            log += "AM ";
        }
        if (scheduleBusy == null || scheduleBusy) {
            log += "SM ";
        }
        if (fencesBusy == null || fencesBusy) {
            log += "WZFM ";
        }
        if (progress == null || !progress.isEmpty()) {
            log += "WZMU ";
        }*/

        if (addressValidatorBusy == null || alertBusy == null || scheduleBusy == null
                || fencesBusy == null || progress == null) {
            //Log.d(TAG, log);
            return true;
        } else {
            /*if (addressValidatorBusy || alertBusy || scheduleBusy || fencesBusy || !progress.isEmpty()) {
                Log.d(TAG, log);
            }*/
            return addressValidatorBusy || alertBusy || scheduleBusy || fencesBusy || !progress.isEmpty();
        }
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        mDispatcher.onServicePreSuperOnDestroy();

        // Please reschedule
        return true;
    }

    @Override
    public Lifecycle getLifecycle() {
        return mDispatcher.getLifecycle();
    }

    private void finish(SharedPreferences preferences, JobParameters jobParameters) {
        preferences.edit().putLong(Preferences.PREFERENCE_APP_UPDATER_LAST_FINISHED,
                System.currentTimeMillis()).apply();
        mDispatcher.onServicePreSuperOnDestroy();
        jobFinished(jobParameters, false);
    }
}
