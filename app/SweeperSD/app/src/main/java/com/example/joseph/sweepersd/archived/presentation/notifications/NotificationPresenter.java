package com.example.joseph.sweepersd.archived.presentation.notifications;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.example.joseph.sweepersd.archived.model.watchzone.WatchZone;
import com.example.joseph.sweepersd.archived.presentation.manualalarms.WatchZoneViewActivity;
import com.hutchins.tbd.R;

import org.apache.commons.lang3.text.WordUtils;

import androidx.core.app.NotificationCompat;

/**
 * Created by joseph on 4/7/16.
 */
public class NotificationPresenter {
    private static final String TAG = NotificationPresenter.class.getSimpleName();

    /*public static void sendParkedNotificationIfEnabled(Context context) {
        if (SettingsUtils.isParkedNotificationEnabled(context)) {
            String message = "Park detected";
            Intent notificationIntent = new Intent(context, MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent intent = PendingIntent.getActivity(context, 0,
                    notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_lrg_car_black)
                    .setContentTitle("Parked")
                    .setContentText(message)
                    .setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 })
                    .setContentIntent(intent);

            NotificationManager notificationManager = (NotificationManager)
                    context.getSystemService(context.NOTIFICATION_SERVICE);
            notificationManager.notify(NotificationType.PARKED.ordinal(), builder.build());
        }
    }*/

    /*public static void sendRedzoneWarningNotification(Context context) {
        String message = "WARNING: Street sweeping soon!";
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent intent = PendingIntent.getActivity(context, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_lrg_no_parking_yellow)
                .setContentTitle("Sweeping soon")
                .setContentText(message)
                .setVibrate(new long[]{1000, 750, 500, 750, 500, 750, 500})
                .setContentIntent(intent);

        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(context.NOTIFICATION_SERVICE);
        notificationManager.notify(NotificationType.REDZONE_WARNING.ordinal(), builder.build());
    }*/

    public static void sendWatchZoneNotification(Context context, WatchZone watchzone,
                                                 int hoursUntilStarting) {
        String message = "WARNING: Sweeping near " + WordUtils.capitalize(watchzone.getLabel())
                + " starting in " + hoursUntilStarting + " hours.";
        Intent notificationIntent = new Intent(context, WatchZoneViewActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent intent = PendingIntent.getActivity(context, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_lrg_no_parking_red)
                .setContentTitle("Street Sweeping")
                .setContentText(message)
                .setVibrate(new long[]{1000, 750, 500, 750, 500, 750, 500})
                .setContentIntent(intent);

        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(context.NOTIFICATION_SERVICE);
        int id = (int) watchzone.getCreatedTimestamp() / 1000;
        notificationManager.notify(id, builder.build());
    }

    /*private void sendParkedInLimitZoneNotification() {
        // TODO: hard coded strings, Notification builder cleanup and nicer
        if (!mPotentialParkedLocations.isEmpty()) {
            String message = "You just parked in a street sweeping zone!";
            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent intent = PendingIntent.getActivity(this, 0,
                    notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setSmallIcon(android.R.drawable.ic_notification_overlay)
                    .setContentTitle("lambda")
                    .setContentText(message)
                    .setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 })
                    .setContentIntent(intent);

            android.app.NotificationPresenter notificationManager = (android.app.NotificationPresenter)
                    getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_PARKED_LIMIT_ID, builder.build());
        }
    }

    private void sendParkedInRedZoneNotification(long msTilSweeping) {
        // TODO: hard coded strings, Notification builder cleanup and nicer
        if (!mPotentialParkedLocations.isEmpty()) {
            long hoursUntilParking = msTilSweeping / 3600000;
            long leftOverMinutes = (msTilSweeping % 3600000) / 60000;
            long daysUntilSweeping = hoursUntilParking / 24;
            long leftOverHours = hoursUntilParking % 24;
            String message = "Street Sweeping in " + daysUntilSweeping + " days, "
                    + leftOverHours + " hours, and " + leftOverMinutes + " minutes.";
            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent intent = PendingIntent.getActivity(this, 0,
                    notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setSmallIcon(android.R.drawable.ic_notification_overlay)
                    .setContentTitle("lambda")
                    .setContentText(message)
                    .setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 })
                    .setContentIntent(intent);

            android.app.NotificationPresenter notificationManager = (android.app.NotificationPresenter)
                    getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_PARKED_REDZONE_ID, builder.build());
        }
    }*/
}
