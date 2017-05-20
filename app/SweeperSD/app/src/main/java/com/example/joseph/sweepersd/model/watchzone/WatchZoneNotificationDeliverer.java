package com.example.joseph.sweepersd.model.watchzone;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.example.joseph.sweepersd.presentation.notifications.NotificationPresenter;

import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by joseph on 9/20/16.
 */
public class WatchZoneNotificationDeliverer extends IntentService {
    private static final String TAG = WatchZoneNotificationDeliverer.class.getSimpleName();

    public WatchZoneNotificationDeliverer() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "Starting " + TAG);

        WatchZoneManager manager = new WatchZoneManager(this);
        long id = Long.parseLong(intent.getType());
        WatchZone watchZone = manager.getWatchZoneComplete(id);

        if (watchZone != null) {
            long nextSweepingTime = WatchZoneUtils.getNextSweepingTimeFromAddresses(
                    watchZone.getSweepingAddresses());
            GregorianCalendar today = new GregorianCalendar(
                    TimeZone.getTimeZone("America/Los_Angeles"), Locale.US);
            long timeUntil = nextSweepingTime - today.getTime().getTime();

            int hours = (int) Math.ceil(((double) timeUntil) / ((double) (1000 * 60 * 60)));

            NotificationPresenter.sendWatchZoneNotification(this, watchZone, hours);
            WatchZoneUtils.scheduleWatchZoneAlarm(this, watchZone);
        }
    }
}
