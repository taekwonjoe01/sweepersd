package com.example.joseph.sweepersd.watchzone.model;

import com.example.joseph.sweepersd.limit.LimitModel;
import com.example.joseph.sweepersd.limit.LimitSchedule;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class WatchZoneUtils {
    private static final String TAG = WatchZoneUtils.class.getSimpleName();
    private static final int REQUEST_CODE_ALARM = 0;

    public static long TWENTY_FOUR_HOURS = 1000 * 60 * 60 * 24;
    public static long TWELVE_HOURS = 1000 * 60 * 60 * 12;
    public static long SIX_HOURS = 1000 * 60 * 60 * 6;
    public static long ONE_HOUR = 1000 * 60 * 60;

    private static final long HOUR_OFFSET_12 = 1000L * 60L * 60L * 12L;
    private static final long HOUR_OFFSET_24 = 1000L * 60L * 60L * 24L;
    private static final long HOUR_OFFSET_48 = 1000L * 60L * 60L * 48L;

    public static LimitScheduleDate findNextSweepingDate(LimitSchedule schedule, boolean includeEndTime) {
        LimitScheduleDate result = null;
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
                                schedule.getStartMinute(),
                                0);
                        int sweepingLengthMinutes = ((schedule.getEndHour() * 60) + schedule.getEndMinute()) -
                                ((schedule.getStartHour() * 60) + schedule.getStartMinute());
                        if (sweepingLengthMinutes < 0) {
                            sweepingLengthMinutes += 24 * 60;
                        }
                        GregorianCalendar potentialEndTime = new GregorianCalendar(
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH),
                                schedule.getStartHour(),
                                schedule.getStartMinute(),
                                0);
                        potentialEndTime.add(Calendar.MINUTE, sweepingLengthMinutes);

                        if (potentialStartTime.getTime().getTime() > today.getTime().getTime()) {
                            result = new LimitScheduleDate(
                                    schedule, potentialStartTime, potentialEndTime);
                            break;
                        } else if (includeEndTime &&
                                potentialEndTime.getTime().getTime() > today.getTime().getTime()) {
                            result = new LimitScheduleDate(
                                    schedule, potentialStartTime, potentialEndTime);
                            break;
                        }
                    }
                }

                calendar.add(Calendar.DAY_OF_YEAR, 1);
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

    public static void sortByNextStartTime(List<LimitScheduleDate> datesToSort) {
        Collections.sort(datesToSort, new Comparator<LimitScheduleDate>() {
            @Override
            public int compare(LimitScheduleDate date1, LimitScheduleDate date2) {
                long val = date1.getStartCalendar().getTime().getTime() -
                        date2.getStartCalendar().getTime().getTime();
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

    public static List<LimitScheduleDate> getAllSweepingDatesForLimitSchedules(List<LimitSchedule> schedules,
                                                                               int numDaysInPast,
                                                                               int numDaysInFuture) {
        List<LimitScheduleDate> limitScheduleDates = new ArrayList<>();
        for (LimitSchedule s : schedules) {
            limitScheduleDates.addAll(getAllSweepingDatesForLimitSchedule(s, numDaysInPast, numDaysInFuture));
        }

        return limitScheduleDates;
    }

    public static List<LimitScheduleDate> getAllSweepingDatesForLimitSchedule(LimitSchedule schedule,
                                                                              int numDaysInPast,
                                                                              int numDaysInFuture) {
        List<LimitScheduleDate> results = new ArrayList<>();
        if (isValidLimitSchedule(schedule)) {
            GregorianCalendar calendar = new GregorianCalendar(
                    TimeZone.getTimeZone("America/Los_Angeles"), Locale.US);

            for (int j = 0; j < numDaysInPast; j++) {
                LimitScheduleDate date = checkCalendarForSweeping(schedule, calendar);

                if (date != null) {
                    results.add(date);
                }

                calendar.add(Calendar.DAY_OF_YEAR, -1);
            }
            calendar = new GregorianCalendar(
                    TimeZone.getTimeZone("America/Los_Angeles"), Locale.US);
            for (int j = 0; j < numDaysInFuture; j++) {
                LimitScheduleDate date = checkCalendarForSweeping(schedule, calendar);

                if (date != null) {
                    results.add(date);
                }

                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
        }

        return results;
    }

    private static LimitScheduleDate checkCalendarForSweeping(LimitSchedule schedule,
                                                              GregorianCalendar calendar) {
        LimitScheduleDate result = null;

        int dow = calendar.get(Calendar.DAY_OF_WEEK);
        int dom = calendar.get(Calendar.DAY_OF_MONTH);

        int rangeStart = ((schedule.getWeekNumber() - 1) * 7) + 1;
        int endRange = ((schedule.getWeekNumber()) * 7);
        if (dom >= rangeStart && dom < (endRange + 1)) {
            if (dow == schedule.getDayNumber()) {
                GregorianCalendar potentialStartTime = new GregorianCalendar(
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH),
                        schedule.getStartHour(),
                        schedule.getStartMinute(),
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
                        schedule.getStartMinute(),
                        0);
                potentialEndTime.add(Calendar.HOUR, sweepingLength);

                result = new LimitScheduleDate(schedule, potentialStartTime, potentialEndTime);
            } else if (dow == (schedule.getDayNumber() % 7) + 1) {
                int sweepingLengthMinutes = ((schedule.getEndHour() * 60) + schedule.getEndMinute()) -
                        ((schedule.getStartHour() * 60) + schedule.getStartMinute());
                if (sweepingLengthMinutes < 0) {
                    GregorianCalendar potentialStartTime = new GregorianCalendar(
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH),
                            schedule.getStartHour(),
                            schedule.getStartMinute(),
                            0);
                    sweepingLengthMinutes += 24 * 60;
                    GregorianCalendar potentialEndTime = new GregorianCalendar(
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH),
                            schedule.getStartHour(),
                            schedule.getStartMinute(),
                            0);
                    potentialEndTime.add(Calendar.MINUTE, sweepingLengthMinutes);

                    result = new LimitScheduleDate(schedule, potentialStartTime, potentialEndTime);
                }
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

    public static List<LimitScheduleDate> getStartTimeOrderedDatesForLimitSchedules(
            List<LimitSchedule> schedules, boolean includeEndTime) {
        List<LimitScheduleDate> limitScheduleDates = new ArrayList<>();
        for (LimitSchedule s : schedules) {
            limitScheduleDates.add(WatchZoneUtils.findNextSweepingDate(s, includeEndTime));
        }
        WatchZoneUtils.sortByNextStartTime(limitScheduleDates);

        return limitScheduleDates;
    }

    public static long getNextSweepingStartTime(List<LimitSchedule> schedules) {
        List<LimitScheduleDate> allDates = WatchZoneUtils.getStartTimeOrderedDatesForLimitSchedules(
                        schedules, false);

        if (allDates.size() > 0) {
            return allDates.get(0).getStartCalendar().getTime().getTime();
        } else {
            return 0;
        }
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

    public static List<LimitScheduleDate> getStartTimeOrderedDatesForWatchZone(WatchZoneModel model) {
        List<LimitScheduleDate> results = null;
        Map<Long, LimitModel> limitModels = model.getUniqueLimitModels();
        if (!limitModels.isEmpty()) {
            List<LimitSchedule> allSchedules = new ArrayList<>();
            for (Long limitUid : limitModels.keySet()) {
                LimitModel limitModel = limitModels.get(limitUid);
                allSchedules.addAll(limitModel.schedules);
            }
            results = getStartTimeOrderedDatesForLimitSchedules(allSchedules, true);
        }
        return results;
    }

    public static long getNextEventTimestampForWatchZone(WatchZoneModel model) {
        long result = -1L;
        Map<Long, LimitModel> limitModels = model.getUniqueLimitModels();
        if (!limitModels.isEmpty()) {
            List<LimitSchedule> allSchedules = new ArrayList<>();
            for (Long limitUid : limitModels.keySet()) {
                LimitModel limitModel = limitModels.get(limitUid);
                allSchedules.addAll(limitModel.schedules);
            }
            long startOffset = getStartHourOffset(model.watchZone);
            result = getNextEventTimestampForLimitSchedules(allSchedules, startOffset);
        }
        return result;
    }

    public static long getNextEventTimestampForLimitSchedules(List<LimitSchedule> schedules,
                                                              long startOffset) {
        List<LimitScheduleDate> allDates = WatchZoneUtils.getStartTimeOrderedDatesForLimitSchedules(
                schedules, true);
        List<Long> timeOrderedTimestamps = new ArrayList<>();
        for (LimitScheduleDate date : allDates) {
            long startTimeWithOffset = date.getStartCalendar().getTime().getTime() - startOffset;
            GregorianCalendar today = new GregorianCalendar(
                    TimeZone.getTimeZone("America/Los_Angeles"), Locale.US);
            if (startTimeWithOffset > today.getTime().getTime()) {
                timeOrderedTimestamps.add(startTimeWithOffset);
            }
            if (date.getStartCalendar().getTime().getTime() > today.getTime().getTime()) {
                timeOrderedTimestamps.add(date.getStartCalendar().getTime().getTime());
            }
            timeOrderedTimestamps.add(date.getEndCalendar().getTime().getTime());
        }
        Collections.sort(timeOrderedTimestamps);

        if (timeOrderedTimestamps.size() > 0) {
            return timeOrderedTimestamps.get(0);
        } else {
            return -1L;
        }
    }

    public static long getStartHourOffset(WatchZone watchZone) {
        int remindRange = watchZone.getRemindRange();
        switch (remindRange) {
            case WatchZone.REMIND_RANGE_48_HOURS:
                return HOUR_OFFSET_48;
            case WatchZone.REMIND_RANGE_24_HOURS:
                return HOUR_OFFSET_24;
            case WatchZone.REMIND_RANGE_12_HOURS:
                return HOUR_OFFSET_12;
            default:
                return HOUR_OFFSET_48;
        }
    }
}
