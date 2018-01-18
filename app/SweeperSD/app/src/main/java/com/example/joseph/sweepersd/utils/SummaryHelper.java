package com.example.joseph.sweepersd.utils;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.watchzone.model.LimitScheduleDate;
import com.example.joseph.sweepersd.watchzone.model.WatchZone;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    public static SummaryStatus getStatusFromModel(WatchZoneModel watchZoneModel) {
        SummaryStatus result = SummaryStatus.NEVER;

        final WatchZone watchZone = watchZoneModel.watchZone;

        List<LimitScheduleDate> sweepingDates =
                WatchZoneUtils.getStartTimeOrderedDatesForWatchZone(watchZoneModel);
        if (sweepingDates != null) {
            List<LimitScheduleDate> currentSweeping = new ArrayList<>();
            List<LimitScheduleDate> upcomingSweeping = new ArrayList<>();
            long now = new GregorianCalendar(
                    TimeZone.getTimeZone("America/Los_Angeles"), Locale.US).getTime().getTime();
            long startOffset = WatchZoneUtils.getStartHourOffset(watchZone);
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

            if (!currentSweeping.isEmpty()) {
                result = SummaryStatus.NOW;
            } else if (!upcomingSweeping.isEmpty()) {
                result = SummaryStatus.SOON;
            } else if (!sweepingDates.isEmpty()) {
                result = SummaryStatus.LATER;
            }
        }
        return result;
    }
}
