package com.example.joseph.sweepersd.alert;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.utils.Notifications;
import com.example.joseph.sweepersd.watchzone.UserZonesActivity;
import com.example.joseph.sweepersd.watchzone.model.LimitScheduleDate;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneUtils;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class AlertManager {
    private final Context mApplicationContext;

    public AlertManager(Context context) {
        mApplicationContext = context.getApplicationContext();
    }

    public void updateAlertNotification(List<WatchZoneModel> models) {
        List<String> currentLabels = new ArrayList<>();
        List<String> upcomingLabels = new ArrayList<>();
        for (WatchZoneModel model : models) {
            List<LimitScheduleDate> sweepingDates =
                    WatchZoneUtils.getStartTimeOrderedDatesForWatchZone(model);
            List<LimitScheduleDate> currentSweeping = new ArrayList<>();
            List<LimitScheduleDate> upcomingSweeping = new ArrayList<>();
            long now = new GregorianCalendar(
                    TimeZone.getTimeZone("America/Los_Angeles"), Locale.US).getTime().getTime();
            long startOffset = WatchZoneUtils.getStartHourOffset(model.getWatchZone());
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
                currentLabels.add(model.getWatchZone().getLabel());
            }
            if (upcomingSweeping.size() > 0) {
                upcomingLabels.add(model.getWatchZone().getLabel());
            }
        }
        if (!currentLabels.isEmpty() || !upcomingLabels.isEmpty()) {
            if (upcomingLabels.isEmpty()) {
                if (currentLabels.size() <= 2) {
                    String message = "Current Sweeping at " + currentLabels.get(0);
                    if (currentLabels.size() == 2) {
                        message += "\nCurrent Sweeping at " + currentLabels.get(1);
                    }
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
                    message += "Upcoming Sweeping at " + upcomingLabels.get(0);
                    if (upcomingLabels.size() == 2) {
                        message += "\nUpcoming Sweeping at " + upcomingLabels.get(1);
                    } else if (!upcomingLabels.isEmpty()) {
                        message += "\nand " + upcomingLabels.size() + " other zones.";
                    }
                } else if (!upcomingLabels.isEmpty()) {
                    message += "\nUpcoming Sweeping at " + upcomingLabels.get(0);
                }
                sendNotification(message);
            }
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
}
