package com.example.joseph.sweepersd.utils;

import android.util.Log;

import com.example.joseph.sweepersd.watchzone.model.LimitScheduleDate;
import com.example.joseph.sweepersd.watchzone.model.WatchZone;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class SummaryHelper {

    public enum SummaryStatus {
        NOW,
        SOON,
        LATER,
        NEVER
    }

    public static class WatchZoneModelSummary {
        public SummaryStatus summaryStatus;
        public LimitScheduleDate dateForStatus;
    }

    public static WatchZoneModelSummary getStatusFromModel(WatchZoneModel watchZoneModel) {
        WatchZoneModelSummary result = new WatchZoneModelSummary();
        result.summaryStatus = SummaryStatus.NEVER;
        result.dateForStatus = null;

        final WatchZone watchZone = watchZoneModel.watchZone;

        List<LimitScheduleDate> sweepingDates =
                WatchZoneUtils.getStartTimeOrderedDatesForWatchZone(watchZoneModel);
        if (sweepingDates != null && !sweepingDates.isEmpty()) {
            long now = new GregorianCalendar(
                    TimeZone.getTimeZone("America/Los_Angeles"), Locale.US).getTime().getTime();
            long startOffset = WatchZoneUtils.getStartHourOffset(watchZone);
            LimitScheduleDate date = sweepingDates.get(0);

            long warningTime = date.getStartCalendar().getTime().getTime() - startOffset;
            Log.e("Joey", "warning time: " + new SimpleDateFormat("EEE, MMM dd 'at' K:mma").format(new Date(warningTime)));
            long startTime = date.getStartCalendar().getTime().getTime();
            Log.e("Joey", "start time: " + new SimpleDateFormat("EEE, MMM dd 'at' K:mma").format(new Date(startTime)));
            long endTime = date.getEndCalendar().getTime().getTime();
            Log.e("Joey", "end time: " + new SimpleDateFormat("EEE, MMM dd 'at' K:mma").format(new Date(endTime)));
            if (startTime <= now && endTime >= now) {
                result.summaryStatus = SummaryStatus.NOW;
                Log.e("Joey", "returning now");
            } else if (warningTime <= now && endTime >= now) {
                result.summaryStatus = SummaryStatus.SOON;
                Log.e("Joey", "returning soon");
            } else {
                result.summaryStatus = SummaryStatus.LATER;
                Log.e("Joey", "returning later");
            }
            result.dateForStatus = date;
            /*Log.e("Joey", "For this model:");

            for (LimitScheduleDate d : sweepingDates) {
                Log.e("Joey", new SimpleDateFormat("EEE, MMM dd 'at' K:mma").format(new Date(d.getStartCalendar().getTime().getTime())));
            }*/
        }
        return result;
    }

    public static String getNextSweepingString(LimitScheduleDate date) {
        String result = "";

        long nextSweepingTime = date.getStartCalendar().getTime().getTime();

        Calendar sweeping = Calendar.getInstance();
        sweeping.setTime(new Date(nextSweepingTime));
        Calendar today = Calendar.getInstance();
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DATE, 1);
        if (sweeping.get(Calendar.DATE) == today.get(Calendar.DATE)) {
            result = "Next sweeping is today at " +
                    new SimpleDateFormat("K:mma").format(new Date(nextSweepingTime));
        } else if (sweeping.get(Calendar.DATE) == tomorrow.get(Calendar.DATE)) {
            result = "Next sweeping is tomorrow at " +
                    new SimpleDateFormat("K:mma").format(new Date(nextSweepingTime));
        } else {
            result = "Next sweeping is " +
                    new SimpleDateFormat("EEE, MMM dd 'at' K:mma").format(new Date(nextSweepingTime));
        }

        return result;
    }
}
