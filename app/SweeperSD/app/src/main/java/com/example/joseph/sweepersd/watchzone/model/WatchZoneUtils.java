package com.example.joseph.sweepersd.watchzone.model;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.joseph.sweepersd.limit.LimitSchedule;
import com.example.joseph.sweepersd.watchzone.WatchZoneAlarmReceiver;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class WatchZoneUtils {
    private static final String TAG = WatchZoneUtils.class.getSimpleName();
    private static final int REQUEST_CODE_ALARM = 0;

    public static long TWENTY_FOUR_HOURS = 1000 * 60 * 60 * 24;
    public static long TWELVE_HOURS = 1000 * 60 * 60 * 12;
    public static long SIX_HOURS = 1000 * 60 * 60 * 6;
    public static long ONE_HOUR = 1000 * 60 * 60;

    public static SweepingDate findNextSweepingDate(LimitSchedule schedule) {
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
                    if (dow == schedule.getDayNumber()) {
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
        return schedule != null && schedule.getDayNumber() > 0 && schedule.getDayNumber() < 8
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

    public static List<SweepingDate> getAllSweepingDatesForLimitSchedules(List<LimitSchedule> schedules,
                                                                          int numDaysInPast,
                                                                          int numDaysInFuture) {
        List<SweepingDate> sweepingDates = new ArrayList<>();
        for (LimitSchedule s : schedules) {
            sweepingDates.addAll(getAllSweepingDatesForLimitSchedule(s, numDaysInPast, numDaysInFuture));
        }

        return sweepingDates;
    }

    public static List<SweepingDate> getAllSweepingDatesForLimitSchedule(LimitSchedule schedule,
                                                                         int numDaysInPast,
                                                                         int numDaysInFuture) {
        List<SweepingDate> results = new ArrayList<>();
        if (isValidLimitSchedule(schedule)) {
            GregorianCalendar calendar = new GregorianCalendar(
                    TimeZone.getTimeZone("America/Los_Angeles"), Locale.US);

            for (int j = 0; j < numDaysInPast; j++) {
                SweepingDate date = checkCalendarForSweeping(schedule, calendar);

                if (date != null) {
                    results.add(date);
                }

                calendar.add(Calendar.DAY_OF_YEAR, -1);
            }
            calendar = new GregorianCalendar(
                    TimeZone.getTimeZone("America/Los_Angeles"), Locale.US);
            for (int j = 0; j < numDaysInFuture; j++) {
                SweepingDate date = checkCalendarForSweeping(schedule, calendar);

                if (date != null) {
                    results.add(date);
                }

                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
        }

        return results;
    }

    private static SweepingDate checkCalendarForSweeping(LimitSchedule schedule,
                                                         GregorianCalendar calendar) {
        SweepingDate result = null;

        int dow = calendar.get(Calendar.DAY_OF_WEEK);
        int dom = calendar.get(Calendar.DAY_OF_MONTH);

        int rangeStart = ((schedule.getWeekNumber() - 1) * 7) + 1;
        int endRange = ((schedule.getWeekNumber()) * 7) + 1;
        if (dom >= rangeStart && dom < endRange) {
            if (dow == schedule.getDayNumber()) {
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

                result = new SweepingDate(
                        schedule, potentialStartTime, potentialEndTime);
            }
        }
        return result;
    }

    /**
     * Many sweeping addresses have the same limit. We don't want to show duplicated limits on UI.
     * @param points
     * @return
     */
    public static List<Long> getUniqueLimitIds(List<WatchZonePoint> points) {
        List<Long> results = new ArrayList<>();
        for (WatchZonePoint p : points) {
            if (p.getLimitId() != -1) {
                if (!results.contains(p.getLimitId())) {
                    results.add(p.getLimitId());
                }
            }
        }
        return results;
    }

    public static List<SweepingDate> getTimeOrderedSweepingDatesForLimitSchedules(List<LimitSchedule> schedules) {
        //WatchZoneUtils.filterInvalidLimitSchedules(schedules);
        List<SweepingDate> sweepingDates = new ArrayList<>();
        for (LimitSchedule s : schedules) {
            sweepingDates.add(WatchZoneUtils.findNextSweepingDate(s));
        }
        WatchZoneUtils.sortByNextStartTime(sweepingDates);

        return sweepingDates;
    }

    public static long getNextSweepingTime(List<LimitSchedule> schedules) {
        List<SweepingDate> allDates = WatchZoneUtils.getTimeOrderedSweepingDatesForLimitSchedules(
                        schedules);

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

    public static long getAlarmTimeForSweepingTime(long sweepingTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        long timeUntil = sweepingTime - calendar.getTimeInMillis();

        if (timeUntil > TWENTY_FOUR_HOURS) {
            return sweepingTime - TWENTY_FOUR_HOURS;
        } else if (timeUntil > TWELVE_HOURS) {
            return sweepingTime - TWELVE_HOURS;
        } else if (timeUntil > SIX_HOURS) {
            return sweepingTime - SIX_HOURS;
        } else if (timeUntil > ONE_HOUR) {
            return sweepingTime - ONE_HOUR;
        } else {
            return calendar.getTimeInMillis();
        }
    }

    public static void scheduleWatchZoneNotification(Context context, WatchZoneModel model) {
        List<LimitSchedule> allSchedules = new ArrayList<>();
        for (Long limitUid : model.getWatchZoneLimitModelUids()) {
            WatchZoneLimitModel limitModel = model.getWatchZoneLimitModel(limitUid);
            allSchedules.addAll(limitModel.getLimitSchedulesModel().getScheduleList());
        }
        long nextSweepingTime = getNextSweepingTime(allSchedules);
        if (nextSweepingTime > 0) {
            long alarmTime = getAlarmTimeForSweepingTime(nextSweepingTime);
            AlarmManager alarmMgr;
            PendingIntent alarmIntent;

            alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, WatchZoneAlarmReceiver.class);
            intent.setType(model.getWatchZoneUid() + "");
            alarmIntent = PendingIntent.getBroadcast(context, REQUEST_CODE_ALARM, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);


            alarmMgr.set(AlarmManager.RTC_WAKEUP, alarmTime, alarmIntent);
            Log.i(TAG, "Alarm scheduled for " + new Date(alarmTime).toString());
        }
    }

    public static void unscheduleWatchZoneNotification(Context context, WatchZoneModel model) {
        AlarmManager alarmMgr;
        PendingIntent alarmIntent;

        alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, WatchZoneAlarmReceiver.class);
        intent.setType(model.getWatchZoneUid() + "");
        alarmIntent = PendingIntent.getBroadcast(context, REQUEST_CODE_ALARM, intent,
                PendingIntent.FLAG_NO_CREATE);
        if (alarmIntent != null) {
            alarmMgr.cancel(alarmIntent);
        }
    }
}
