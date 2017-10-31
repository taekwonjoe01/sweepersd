package com.example.joseph.sweepersd.alert.geofence;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.ServiceLifecycleDispatcher;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.joseph.sweepersd.utils.Jobs;
import com.example.joseph.sweepersd.utils.Preferences;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneBaseObserver;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelsObserver;

import java.util.ArrayList;
import java.util.Map;

public class WatchZoneFenceJob extends JobService implements LifecycleOwner {
    private static final String TAG = WatchZoneFenceJob.class.getSimpleName();

    private ServiceLifecycleDispatcher mDispatcher;

    public static void scheduleJob(Context context) {
        Log.i(TAG, "Scheduling " + TAG);
        JobScheduler jobScheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        if (jobScheduler.getPendingJob(Jobs.GEOFENCE_JOB) != null) {
            return;
        }
        Log.i(TAG, "Building " + TAG);

        JobInfo.Builder builder = new JobInfo.Builder(Jobs.GEOFENCE_JOB,
                new ComponentName(context, WatchZoneFenceJob.class));
        builder.setMinimumLatency(15000L);
        jobScheduler.schedule(builder.build());
    }

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        Log.i(TAG, "Starting " + TAG);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putLong(Preferences.PREFERENCE_FENCE_JOB_LAST_STARTED,
                System.currentTimeMillis()).commit();

        mDispatcher = new ServiceLifecycleDispatcher(this);

        mDispatcher.onServicePreSuperOnCreate();
        mDispatcher.onServicePreSuperOnStart();

        WatchZoneModelRepository.getInstance(this).observe(this, new WatchZoneModelsObserver(
                new WatchZoneModelsObserver.WatchZoneModelsChangedCallback() {
            @Override
            public void onModelsChanged(Map<Long, WatchZoneModel> data,
                                        WatchZoneBaseObserver.ChangeSet changeSet) {
                // Do nothing.
            }

            @Override
            public void onDataLoaded(Map<Long, WatchZoneModel> models) {
                GeofenceManager fenceManager = new GeofenceManager(WatchZoneFenceJob.this);
                fenceManager.updateGeofences(new ArrayList<>(models.values()));
                preferences.edit().putLong(Preferences.PREFERENCE_FENCE_JOB_LAST_FINISHED,
                        System.currentTimeMillis()).commit();
                jobFinished(jobParameters, false);
            }
            @Override
            public void onDataInvalid() {
                // Do nothing
            }
        }));
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