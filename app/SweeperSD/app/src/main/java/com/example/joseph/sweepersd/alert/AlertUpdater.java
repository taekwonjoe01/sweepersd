package com.example.joseph.sweepersd.alert;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.alert.geofence.WatchZoneFence;
import com.example.joseph.sweepersd.utils.Notifications;
import com.example.joseph.sweepersd.watchzone.UserZonesActivity;
import com.example.joseph.sweepersd.watchzone.model.LimitScheduleDate;
import com.example.joseph.sweepersd.watchzone.model.WatchZone;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneUtils;

import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class AlertUpdater {
    private final Context mApplicationContext;

    public AlertUpdater(Context context) {
        mApplicationContext = context.getApplicationContext();
    }

    public void updateAlertNotification(List<WatchZoneModel> models, List<WatchZoneFence> fences) {
        List<String> currentLabels = new ArrayList<>();
        List<String> upcomingLabels = new ArrayList<>();
        for (WatchZoneModel model : models) {
            boolean isInBounds = false;
            if (model.watchZone.getRemindPolicy() == WatchZone.REMIND_POLICY_ANYWHERE) {
                isInBounds = true;
            } else if (model.watchZone.getRemindPolicy() == WatchZone.REMIND_POLICY_NEARBY) {
                WatchZoneFence fence = null;
                for (WatchZoneFence f : fences) {
                    if (f.getWatchZoneId() == model.watchZone.getUid()) {
                        fence = f;
                    }
                }
                if (fence != null && fence.isInRegion()) {
                    isInBounds = true;
                }
            }
            if (isInBounds) {
                List<LimitScheduleDate> sweepingDates =
                        WatchZoneUtils.getStartTimeOrderedDatesForWatchZone(model);
                if (sweepingDates != null) {
                    List<LimitScheduleDate> currentSweeping = new ArrayList<>();
                    List<LimitScheduleDate> upcomingSweeping = new ArrayList<>();
                    long now = new GregorianCalendar(
                            TimeZone.getTimeZone("America/Los_Angeles"), Locale.US).getTime().getTime();
                    long startOffset = WatchZoneUtils.getStartHourOffset(model.watchZone);
                    for (LimitScheduleDate date : sweepingDates) {
                        long warningTime = date.getStartCalendar().getTime().getTime() - startOffset;
                        long startTime = date.getStartCalendar().getTime().getTime();
                        long endTime = date.getEndCalendar().getTime().getTime();
                        if (startTime <= now && endTime >= now) {
                            currentSweeping.add(date);
                        } else if (warningTime <= now && endTime >= now) {
                            upcomingSweeping.add(date);
                        }
                    }

                    if (currentSweeping.size() > 0) {
                        currentLabels.add(WordUtils.capitalize(model.watchZone.getLabel()));
                    }
                    if (upcomingSweeping.size() > 0) {
                        upcomingLabels.add(WordUtils.capitalize(model.watchZone.getLabel()));
                    }
                }
            }
        }
        if (!currentLabels.isEmpty() || !upcomingLabels.isEmpty()) {
            if (upcomingLabels.isEmpty()) {
                if (currentLabels.size() == 1) {
                    String message = "Current Sweeping at " + currentLabels.get(0);
                    sendNotification(message);
                } else {
                    String message = "Current Sweeping at " + currentLabels.size() + " zones.";
                    sendNotification(message);
                }
            } else {
                String message = "";
                if (currentLabels.size() == 1) {
                    message += "Current Sweeping at " + currentLabels.get(0);
                } else if (!currentLabels.isEmpty()) {
                    message += "Current Sweeping at " + currentLabels.size() + " zones.";
                }
                if (currentLabels.isEmpty()) {
                    if (upcomingLabels.size() == 1) {
                        message += "Upcoming Sweeping at " + upcomingLabels.get(0);
                    } else if (!upcomingLabels.isEmpty()) {
                        message += "Upcoming sweeping at " + upcomingLabels.size() + " zones.";
                    }
                }
                sendNotification(message);
            }
        } else {
            cancelNotification();
        }
    }

    private void sendNotification(String message) {
        Intent notificationIntent = new Intent(mApplicationContext, UserZonesActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent intent = PendingIntent.getActivity(mApplicationContext, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mApplicationContext)
                .setSmallIcon(R.drawable.ic_lrg_no_parking_red)
                .setContentText(message)
                .setOngoing(true)
                .setContentIntent(intent);

        NotificationManager notificationManager = (NotificationManager)
                mApplicationContext.getSystemService(mApplicationContext.NOTIFICATION_SERVICE);
        notificationManager.notify(Notifications.NOTIFICATION_WATCH_ZONE_STREET_SWEEPING, builder.build());
    }

    private void cancelNotification() {
        NotificationManager notificationManager = (NotificationManager)
                mApplicationContext.getSystemService(mApplicationContext.NOTIFICATION_SERVICE);
        notificationManager.cancel(Notifications.NOTIFICATION_WATCH_ZONE_STREET_SWEEPING);
    }
}
