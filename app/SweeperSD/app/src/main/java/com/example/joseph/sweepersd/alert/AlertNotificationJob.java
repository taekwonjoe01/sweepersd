package com.example.joseph.sweepersd.alert;

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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.joseph.sweepersd.alert.geofence.WatchZoneFence;
import com.example.joseph.sweepersd.alert.geofence.WatchZoneFenceRepository;
import com.example.joseph.sweepersd.utils.Jobs;
import com.example.joseph.sweepersd.utils.Preferences;
import com.example.joseph.sweepersd.utils.BaseObserver;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelsObserver;
import com.example.joseph.sweepersd.watchzone.model.ZoneModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AlertNotificationJob extends JobService implements LifecycleOwner {
    private static final String TAG = AlertNotificationJob.class.getSimpleName();

    private static final String WATCH_ZONE_UID = "WATCH_ZONE_UID";

    private ServiceLifecycleDispatcher mDispatcher;

    private boolean mWatchZoneModelsLoaded;
    private boolean mWatchZoneFencesLoaded;

    public static void scheduleJob(Context context) {
        Log.i(TAG, "Scheduling " + TAG);
        JobScheduler jobScheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        if (jobScheduler.getPendingJob(Jobs.ALERT_JOB) != null) {
            return;
        }
        Log.i(TAG, "Building " + TAG);

        JobInfo.Builder builder = new JobInfo.Builder(Jobs.ALERT_JOB,
                new ComponentName(context, AlertNotificationJob.class));
        builder.setMinimumLatency(30000L);
        jobScheduler.schedule(builder.build());
    }

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        Log.i(TAG, "Starting " + TAG);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putLong(Preferences.PREFERENCE_WATCH_ZONE_NOTIFICATION_LAST_STARTED,
                System.currentTimeMillis()).commit();

        final Long uid = jobParameters.getExtras().getLong(WATCH_ZONE_UID);

        mDispatcher = new ServiceLifecycleDispatcher(this);

        mDispatcher.onServicePreSuperOnCreate();
        mDispatcher.onServicePreSuperOnStart();

        WatchZoneModelRepository.getInstance(this).getZoneModelsLiveData().observe(this, new WatchZoneModelsObserver(true,
                new WatchZoneModelsObserver.WatchZoneModelsChangedCallback() {
            @Override
            public void onModelsChanged(Map<Long, ZoneModel> data,
                                        BaseObserver.ChangeSet changeSet) {
                // Do nothing. Will be rescheduled if happens
            }

            @Override
            public void onDataLoaded(Map<Long, ZoneModel> models) {
                mWatchZoneModelsLoaded = true;
                if (mWatchZoneFencesLoaded) {
                    AlertManager alertManager = new AlertManager(AlertNotificationJob.this);
                    alertManager.updateAlertNotification(new ArrayList<>(models.values()),
                            WatchZoneFenceRepository.getInstance(AlertNotificationJob.this).getFencesLiveData().getValue());
                    preferences.edit().putLong(Preferences.PREFERENCE_WATCH_ZONE_NOTIFICATION_LAST_FINISHED,
                            System.currentTimeMillis()).commit();
                }
                jobFinished(jobParameters, false);
            }
            @Override
            public void onDataInvalid() {
                // Do nothing
            }
        }));
        WatchZoneFenceRepository.getInstance(this).getFencesLiveData().observe(this,
                new Observer<List<WatchZoneFence>>() {
                    @Override
                    public void onChanged(@Nullable List<WatchZoneFence> watchZoneFences) {
                        if (watchZoneFences != null) {
                            mWatchZoneFencesLoaded = true;
                            if (mWatchZoneModelsLoaded) {
                                AlertManager alertManager = new AlertManager(AlertNotificationJob.this);
                                alertManager.updateAlertNotification(new ArrayList<>(
                                                WatchZoneModelRepository.getInstance(
                                                        AlertNotificationJob.this).getZoneModelsLiveData().getValue()),
                                        watchZoneFences);
                                preferences.edit().putLong(Preferences.PREFERENCE_WATCH_ZONE_NOTIFICATION_LAST_FINISHED,
                                        System.currentTimeMillis()).commit();
                            }
                        }
                    }
                });
        mWatchZoneFencesLoaded = false;
        mWatchZoneModelsLoaded = false;

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
