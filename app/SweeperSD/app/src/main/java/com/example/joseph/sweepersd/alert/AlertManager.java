package com.example.joseph.sweepersd.alert;

import android.content.Context;

import com.example.joseph.sweepersd.watchzone.model.WatchZoneModel;

import java.util.List;

public class AlertManager {
    private final Context mApplicationContext;

    public AlertManager(Context context) {
        mApplicationContext = context.getApplicationContext();
    }

    public void updateAlertNotification(List<WatchZoneModel> models) {
            /*WatchZoneModel model = watchZoneModelRepository.getWatchZoneModel(uid);
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
                                    AlertNotificationJob.this, model);
                    preferences.edit().putLong(Preferences.PREFERENCE_WATCH_ZONE_NOTIFICATION_LAST_FINISHED,
                            System.currentTimeMillis()).commit();
                    jobFinished(jobParameters, false);
                } else if (status != WatchZoneModel.Status.LOADING) {
                    // invalid
                    preferences.edit().putLong(Preferences.PREFERENCE_WATCH_ZONE_NOTIFICATION_LAST_FINISHED,
                            System.currentTimeMillis()).commit();
                    jobFinished(jobParameters, false);
                }
            }
        }*/
    }

    /*private void sendNotification(String label, long sweepingTime) {
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
    }*/
}
