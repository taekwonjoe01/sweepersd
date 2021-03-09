package com.example.joseph.sweepersd.scheduling;

import android.app.job.JobParameters;
import android.app.job.JobService;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ServiceLifecycleDispatcher;
import androidx.annotation.Nullable;
import android.util.Log;

public class ScheduleJob extends JobService implements LifecycleOwner {
    private static final String TAG = ScheduleJob.class.getSimpleName();

    private static final String WATCH_ZONE_UID = "WATCH_ZONE_UID";

    private ServiceLifecycleDispatcher mDispatcher;

    /*public static void scheduleJob(Context context) {
        Log.i(TAG, "Scheduling " + TAG);
        JobScheduler jobScheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        if (jobScheduler.getPendingJob(Jobs.SCHEDULE_JOB) != null) {
            return;
        }
        Log.i(TAG, "Building " + TAG);

        JobInfo.Builder builder = new JobInfo.Builder(Jobs.SCHEDULE_JOB,
                new ComponentName(context, ScheduleJob.class));
        builder.setMinimumLatency(30000L);
        jobScheduler.schedule(builder.build());
    }*/

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        Log.i(TAG, "Starting " + TAG);
        /*final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putLong(Preferences.PREFERENCE_SCHEDULE_JOB_LAST_STARTED,
                System.currentTimeMillis()).commit();*/

        mDispatcher = new ServiceLifecycleDispatcher(this);

        mDispatcher.onServicePreSuperOnCreate();
        mDispatcher.onServicePreSuperOnStart();

        ScheduleManager.getInstance(this).observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean working) {
                if (!working) {
                    /*preferences.edit().putLong(Preferences.PREFERENCE_SCHEDULE_JOB_LAST_FINISHED,
                            System.currentTimeMillis()).commit();*/
                    jobFinished(jobParameters, false);
                }
            }
        });
        // Signal that another thread is doing the work.
        return true;
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
}
