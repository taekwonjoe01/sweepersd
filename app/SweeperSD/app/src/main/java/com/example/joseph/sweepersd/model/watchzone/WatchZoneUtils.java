package com.example.joseph.sweepersd.model.watchzone;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.joseph.sweepersd.model.limits.Limit;
import com.example.joseph.sweepersd.model.limits.LimitSchedule;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by joseph on 9/17/16.
 */
public class WatchZoneUtils {
    private static final String TAG = WatchZoneUtils.class.getSimpleName();

    public static SweepingDate findSweepingDate(LimitSchedule schedule) {
        SweepingDate result = null;
        if (isValidLimitSchedule(schedule)) {
            GregorianCalendar today = new GregorianCalendar(
                TimeZone.getTimeZone("America/Los_Angeles"), Locale.US);
            GregorianCalendar calendar = new GregorianCalendar(
                    TimeZone.getTimeZone("America/Los_Angeles"), Locale.US);

            for (int j = 0; j < 40; j++) {
                int dow = calendar.get(Calendar.DAY_OF_WEEK);
                int dom = calendar.get(Calendar.DAY_OF_MONTH);

                int rangeStart = ((schedule.getWeekNumber() - 1) * 7) + 1;
                int endRange = ((schedule.getWeekNumber()) * 7) + 1;
                if (dom >= rangeStart && dom < endRange) {
                    if (dow == schedule.getDay()) {
                        GregorianCalendar potentialStartTime = new GregorianCalendar(
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH),
                                schedule.getStartHour(),
                                0,
                                0);
                        int sweepingLength = schedule.getEndHour() - schedule.getStartHour();
                        if (sweepingLength < 0) {
                            sweepingLength += 24;
                        }
                        GregorianCalendar potentialEndTime = new GregorianCalendar(
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH),
                                schedule.getStartHour(),
                                0,
                                0);
                        potentialEndTime.add(Calendar.HOUR, sweepingLength);

                        if (potentialEndTime.getTime().getTime() > today.getTime().getTime()) {
                            result = new SweepingDate(
                                    schedule, potentialStartTime, potentialEndTime);
                            break;
                        }
                    }
                }

                calendar.add(Calendar.DATE, 1);
            }
        }

        return result;
    }

    public static int getHour(Calendar calendar) {
        int hour = calendar.get(Calendar.HOUR);
        int ampm = calendar.get(Calendar.AM_PM);
        if (ampm == Calendar.PM && hour != 12) {
            hour = hour + 12 % 24;
        } else if (hour == 12) {
            hour = 0;
        }
        return hour;
    }

    public static boolean isValidLimitSchedule(LimitSchedule schedule) {
        return schedule != null && schedule.getDay() > 0 && schedule.getDay() < 8
                && schedule.getWeekNumber() > 0 && schedule.getWeekNumber() < 5
                && schedule.getStartHour() > -1 && schedule.getStartHour() < 24
                && schedule.getEndHour() > -1 && schedule.getEndHour() < 24;
    }

    /**
     * Soonest Sweeping Address will be first.
     * @param datesToSort
     * @return
     */
    public static void sortByNextStartTime(List<SweepingDate> datesToSort) {
        Collections.sort(datesToSort, new Comparator<SweepingDate>() {
            @Override
            public int compare(SweepingDate date1, SweepingDate date2) {
                long val = date1.getStartTime().getTime().getTime() -
                        date2.getStartTime().getTime().getTime();
                return (int)val;
            }
        });
    }

    public static void filterInvalidLimitSchedules(List<LimitSchedule> schedules) {
        List<LimitSchedule> schedulesToRemove = new ArrayList<>();
        for (LimitSchedule s : schedules) {
            if (!isValidLimitSchedule(s)) {
                schedulesToRemove.add(s);
            }
        }
        schedules.removeAll(schedulesToRemove);
    }

    public static List<Limit> getUniqueLimits(List<SweepingAddress> addresses) {
        List<Limit> results = new ArrayList<>();
        for (SweepingAddress a : addresses) {
            if (a.getLimit() != null) {
                if (!results.contains(a.getLimit())) {
                    results.add(a.getLimit());
                }
            }
        }
        return results;
    }

    public static List<SweepingDate> getTimeOrderedSweepingDatesForLimit(Limit l) {
        List<LimitSchedule> schedules = l.getSchedules();
        WatchZoneUtils.filterInvalidLimitSchedules(schedules);
        List<SweepingDate> sweepingDates = new ArrayList<>();
        for (LimitSchedule s : schedules) {
            sweepingDates.add(WatchZoneUtils.findSweepingDate(s));
        }
        WatchZoneUtils.sortByNextStartTime(sweepingDates);

        return sweepingDates;
    }

    public static long getNextSweepingTimeFromAddresses(List<SweepingAddress> addresses) {
        List<Limit> uniqueLimits = WatchZoneUtils.getUniqueLimits(addresses);
        List<SweepingDate> allDates = new ArrayList<>();
        for (Limit l : uniqueLimits) {
            allDates.addAll(
                    WatchZoneUtils.getTimeOrderedSweepingDatesForLimit(l));
        }

        WatchZoneUtils.sortByNextStartTime(allDates);
        WatchZoneUtils.filterOngoingSweepingDates(allDates, true);
        if (allDates.size() > 0) {
            return allDates.get(0).getStartTime().getTime().getTime();
        } else {
            return 0;
        }
    }

    public static void filterOngoingSweepingDates(List<SweepingDate> dates, boolean sorted) {
        GregorianCalendar today = new GregorianCalendar(
                TimeZone.getTimeZone("America/Los_Angeles"), Locale.US);
        List<SweepingDate> datesToRemove = new ArrayList<>();
        for (SweepingDate date : dates) {
            if (date.getStartTime().getTime().getTime() < today.getTime().getTime()) {
                datesToRemove.add(date);
            } else if (sorted) {
                break;
            }
        }
        dates.removeAll(datesToRemove);
    }

    public static long TWENTY_FOUR_HOURS = 1000 * 60 * 60 * 24;
    public static long TWELVE_HOURS = 1000 * 60 * 60 * 12;
    public static long SIX_HOURS = 1000 * 60 * 60 * 6;
    public static long ONE_HOUR = 1000 * 60 * 60;

    public static long getAlarmTimeForSweepingTime(long sweepingTime) {
        GregorianCalendar today = new GregorianCalendar(
                TimeZone.getTimeZone("America/Los_Angeles"), Locale.US);
        long timeUntil = sweepingTime - today.getTime().getTime();

        if (timeUntil > TWENTY_FOUR_HOURS) {
            return sweepingTime - TWENTY_FOUR_HOURS;
        } else if (timeUntil > TWELVE_HOURS) {
            return sweepingTime - TWELVE_HOURS;
        } else if (timeUntil > SIX_HOURS) {
            return sweepingTime - SIX_HOURS;
        } else if (timeUntil > ONE_HOUR) {
            return sweepingTime - ONE_HOUR;
        } else {
            return today.getTime().getTime();
        }
    }

    public static void scheduleWatchZoneAlarm(Context context, WatchZone watchZone) {
        long nextSweepingTime = getNextSweepingTimeFromAddresses(
                watchZone.getSweepingAddresses());
        if (nextSweepingTime > 0) {
            long alarmTime = getAlarmTimeForSweepingTime(nextSweepingTime);
            AlarmManager alarmMgr;
            PendingIntent alarmIntent;

            alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, WatchZoneAlarmReceiver.class);
            intent.setType(watchZone.getCreatedTimestamp() + "");
            alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);


            alarmMgr.set(AlarmManager.RTC_WAKEUP, alarmTime, alarmIntent);
            Log.i(TAG, "Alarm scheduled for " + new Date(alarmTime).toString());
        }
    }
}
