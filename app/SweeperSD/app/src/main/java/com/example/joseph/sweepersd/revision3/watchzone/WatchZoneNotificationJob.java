package com.example.joseph.sweepersd.revision3.watchzone;

import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.content.Intent;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.presentation.manualalarms.WatchZoneViewActivity;
import com.example.joseph.sweepersd.revision3.limit.LimitSchedule;
import com.example.joseph.sweepersd.revision3.utils.Jobs;
import com.example.joseph.sweepersd.revision3.utils.Notifications;
import com.example.joseph.sweepersd.revision3.watchzone.model.WatchZoneLimitModel;
import com.example.joseph.sweepersd.revision3.watchzone.model.WatchZoneModel;
import com.example.joseph.sweepersd.revision3.watchzone.model.WatchZoneModelRepository;
import com.example.joseph.sweepersd.revision3.watchzone.model.WatchZoneUtils;

import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WatchZoneNotificationJob extends JobService implements LifecycleOwner {
    private static final String TAG = WatchZoneNotificationJob.class.getSimpleName();

    private static final String WATCH_ZONE_UID = "WATCH_ZONE_UID";

    private ServiceLifecycleDispatcher mDispatcher;

    public static void scheduleJob(Context context, long watchZoneUid) {
        Log.i(TAG, "Scheduling " + TAG);
        JobScheduler jobScheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        if (jobScheduler.getPendingJob(Jobs.WATCH_ZONE_NOTIFICATION_JOB) != null) {
            return;
        }
        Log.i(TAG, "Building " + TAG);

        JobInfo.Builder builder = new JobInfo.Builder(Jobs.WATCH_ZONE_NOTIFICATION_JOB,
                new ComponentName(context, WatchZoneNotificationJob.class));
        builder.setMinimumLatency(0L);
        PersistableBundle bundle = new PersistableBundle();
        bundle.putLong(WATCH_ZONE_UID, watchZoneUid);
        builder.setExtras(bundle);
        jobScheduler.schedule(builder.build());
    }

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        Log.i(TAG, "Starting " + TAG);

        final Long uid = jobParameters.getExtras().getLong(WATCH_ZONE_UID);

        mDispatcher = new ServiceLifecycleDispatcher(this);

        mDispatcher.onServicePreSuperOnCreate();
        mDispatcher.onServicePreSuperOnStart();
        WatchZoneModelRepository.getInstance(this).observe(this, new Observer<WatchZoneModelRepository>() {
            @Override
            public void onChanged(@Nullable WatchZoneModelRepository watchZoneModelRepository) {
                if (watchZoneModelRepository.watchZoneExists(uid)) {
                    WatchZoneModel model = watchZoneModelRepository.getWatchZoneModel(uid);
                    if (model != null) {
                        WatchZoneModel.Status status = model.getStatus();
                        if (status == WatchZoneModel.Status.VALID) {
                            List<LimitSchedule> allSchedules = new ArrayList<>();
                            for (Long limitUid : model.getWatchZoneLimitModelUids()) {
                                WatchZoneLimitModel limitModel = model.getWatchZoneLimitModel(limitUid);
                                allSchedules.addAll(limitModel.getLimitSchedulesModel().getScheduleList());
                            }

                            long nextSweepingTime = WatchZoneUtils.getNextSweepingTime(
                                    allSchedules);

                            sendNotification(model.getWatchZone().getLabel(), nextSweepingTime);
                            WatchZoneUtils.scheduleWatchZoneNotification(
                                    WatchZoneNotificationJob.this, model);
                            jobFinished(jobParameters, false);
                        } else if (status != WatchZoneModel.Status.LOADING) {
                            // invalid
                            jobFinished(jobParameters, false);
                        }
                    }
                } else {
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

    private void sendNotification(String label, long sweepingTime) {
        String message = "Street Sweeping " + new Date(sweepingTime).toString() + " near " +
                WordUtils.capitalize(label) + ".";
        Intent notificationIntent = new Intent(this, WatchZoneViewActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent intent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_lrg_no_parking_red)
                .setContentTitle("Street Sweeping Warning")
                .setContentText(message)
                .setVibrate(new long[]{1000, 750, 500, 750, 500, 750, 500})
                .setContentIntent(intent);

        NotificationManager notificationManager = (NotificationManager)
                this.getSystemService(this.NOTIFICATION_SERVICE);
        notificationManager.notify(Notifications.NOTIFICATION_WATCH_ZONE_STREET_SWEEPING, builder.build());
    }
}
