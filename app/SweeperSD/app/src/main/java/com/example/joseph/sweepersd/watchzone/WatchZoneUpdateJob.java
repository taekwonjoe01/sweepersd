package com.example.joseph.sweepersd.watchzone;

import android.app.job.JobParameters;
import android.app.job.JobService;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ServiceLifecycleDispatcher;
import androidx.annotation.Nullable;
import android.util.Log;

import com.example.joseph.sweepersd.utils.ChangeSet;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelUpdater;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelsObserver;

import java.util.Map;

public class WatchZoneUpdateJob extends JobService implements LifecycleOwner {
    private static final String TAG = WatchZoneUpdateJob.class.getSimpleName();

    private static final long ONE_DAY = 1000L * 60L * 60L * 24L;
    private static final long ONE_MINUTE = 1000L * 60L;
    private static final long ONE_HOUR = 1000L * 60L * 60L;

    private ServiceLifecycleDispatcher mDispatcher;

    /*public static void scheduleAppForegroundJob(Context context) {
        scheduleAppBackgroundJob(context);

        JobScheduler jobScheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        if (jobScheduler.getPendingJob(Jobs.WATCH_ZONE_UPDATE_FOREGROUND_JOB_ID) != null) {
            return;
        }

        JobInfo.Builder builder = new JobInfo.Builder(Jobs.WATCH_ZONE_UPDATE_FOREGROUND_JOB_ID,
                new ComponentName(context, WatchZoneUpdateJob.class));
        builder.setMinimumLatency(0L);
        builder.setOverrideDeadline(0L);
        builder.setPersisted(true);
        builder.setBackoffCriteria(ONE_MINUTE, JobInfo.BACKOFF_POLICY_LINEAR);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        jobScheduler.schedule(builder.build());
    }

    public static void scheduleAppBackgroundJob(Context context) {
        JobScheduler jobScheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        if (jobScheduler.getPendingJob(Jobs.WATCH_ZONE_UPDATE_BACKGROUND_JOB_ID) != null) {
            return;
        }

        JobInfo.Builder builder = new JobInfo.Builder(Jobs.WATCH_ZONE_UPDATE_BACKGROUND_JOB_ID,
                new ComponentName(context, WatchZoneUpdateJob.class));
        builder.setPeriodic(ONE_DAY);
        builder.setPersisted(true);
        builder.setBackoffCriteria(ONE_HOUR, JobInfo.BACKOFF_POLICY_EXPONENTIAL);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        jobScheduler.schedule(builder.build());
    }*/

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        Log.i(TAG, "Starting update job.");

        /*SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putLong(Preferences.PREFERENCE_WATCH_ZONE_UPDATE_LAST_STARTED,
                System.currentTimeMillis()).commit();*/

        mDispatcher = new ServiceLifecycleDispatcher(this);

        mDispatcher.onServicePreSuperOnCreate();
        mDispatcher.onServicePreSuperOnStart();

        WatchZoneModelUpdater.getInstance(this).observe(this,
                new Observer<Map<Long, Integer>>() {
            @Override
            public void onChanged(@Nullable Map<Long, Integer> longIntegerMap) {
                // Do nothing - just observing so this object comes to life.
            }
        });
        WatchZoneModelRepository.getInstance(this).getCachedWatchZoneModelsLiveData().observe(this,
                new WatchZoneModelsObserver(true, new WatchZoneModelsObserver.WatchZoneModelsChangedCallback() {
            @Override
            public void onModelsChanged(Map<Long, WatchZoneModel> data, ChangeSet changeSet) {
                boolean finished = false;
                if (data != null) {
                    Map<Long, WatchZoneModel> models = data;
                    if (models != null && !models.isEmpty()) {
                        finished = true;
                        /*for (Long uid : models.keySet()) {
                            WatchZoneModel model = models.get(uid);
                            if (model == null || model.getStatus() != WatchZoneModel.Status.VALID) {
                                finished = false;
                            }
                        }*/
                    }
                }
                if (finished) {
                    /*SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(
                            WatchZoneUpdateJob.this);
                    preferences.edit().putLong(Preferences.PREFERENCE_WATCH_ZONE_UPDATE_LAST_FINISHED,
                            System.currentTimeMillis()).commit();*/
                    jobFinished(jobParameters, false);
                }
            }

            @Override
            public void onDataLoaded(Map<Long, WatchZoneModel> data) {
                boolean finished = false;
                if (data != null) {
                    Map<Long, WatchZoneModel> models = data;
                    if (models != null && !models.isEmpty()) {
                        finished = true;
                        /*for (Long uid : models.keySet()) {
                            WatchZoneModel model = models.get(uid);
                            if (model == null || model.getStatus() != WatchZoneModel.Status.VALID) {
                                finished = false;
                            }
                        }*/
                    }
                }
                if (finished) {
                    /*SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(
                            WatchZoneUpdateJob.this);
                    preferences.edit().putLong(Preferences.PREFERENCE_WATCH_ZONE_UPDATE_LAST_FINISHED,
                            System.currentTimeMillis()).commit();*/
                    jobFinished(jobParameters, false);
                }
            }

            @Override
            public void onDataInvalid() {

            }
        }));

        // Signal that another thread is doing the work.
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        mDispatcher.onServicePreSuperOnDestroy();

        Log.i(TAG, "Job interrupted. Rescheduling.");
        return true;
    }

    @Override
    public Lifecycle getLifecycle() {
        return mDispatcher.getLifecycle();
    }
}
