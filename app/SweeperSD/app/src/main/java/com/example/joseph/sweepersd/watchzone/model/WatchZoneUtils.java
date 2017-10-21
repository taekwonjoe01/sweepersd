package com.example.joseph.sweepersd.watchzone.model;

import android.arch.lifecycle.Observer;
import android.content.Context;
import android.support.annotation.Nullable;

import com.example.joseph.sweepersd.limit.LimitSchedule;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;

public class WatchZoneUtils {
    private static final String TAG = WatchZoneUtils.class.getSimpleName();
    private static final int REQUEST_CODE_ALARM = 0;

    public static long TWENTY_FOUR_HOURS = 1000 * 60 * 60 * 24;
    public static long TWELVE_HOURS = 1000 * 60 * 60 * 12;
    public static long SIX_HOURS = 1000 * 60 * 60 * 6;
    public static long ONE_HOUR = 1000 * 60 * 60;

    private static final int HOUR_OFFSET_12 = 12;
    private static final int HOUR_OFFSET_24 = 24;
    private static final int HOUR_OFFSET_48 = 48;

    public static SweepingEventDate findNextSweepingDate(LimitSchedule schedule, boolean includeEndTime,
                                                         int startHourOffset) {
        SweepingEventDate result = null;
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
                        if (startHourOffset > 0) {
                            potentialStartTime.add(Calendar.HOUR, (startHourOffset * -1));
                        }
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

                        if (potentialStartTime.getTime().getTime() > today.getTime().getTime()) {
                            result = new SweepingEventDate(
                                    schedule, potentialStartTime);
                            break;
                        } else if (includeEndTime &&
                                potentialEndTime.getTime().getTime() > today.getTime().getTime()) {
                            result = new SweepingEventDate(
                                    schedule, potentialEndTime);
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
    public static void sortByNextEventTime(List<SweepingEventDate> datesToSort) {
        Collections.sort(datesToSort, new Comparator<SweepingEventDate>() {
            @Override
            public int compare(SweepingEventDate date1, SweepingEventDate date2) {
                long val = date1.getCalendar().getTime().getTime() -
                        date2.getCalendar().getTime().getTime();
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

    public static List<SweepingEventDate> getAllSweepingDatesForLimitSchedules(List<LimitSchedule> schedules,
                                                                               int numDaysInPast,
                                                                               int numDaysInFuture) {
        List<SweepingEventDate> sweepingEventDates = new ArrayList<>();
        for (LimitSchedule s : schedules) {
            sweepingEventDates.addAll(getAllSweepingDatesForLimitSchedule(s, numDaysInPast, numDaysInFuture));
        }

        return sweepingEventDates;
    }

    public static List<SweepingEventDate> getAllSweepingDatesForLimitSchedule(LimitSchedule schedule,
                                                                              int numDaysInPast,
                                                                              int numDaysInFuture) {
        List<SweepingEventDate> results = new ArrayList<>();
        if (isValidLimitSchedule(schedule)) {
            GregorianCalendar calendar = new GregorianCalendar(
                    TimeZone.getTimeZone("America/Los_Angeles"), Locale.US);

            for (int j = 0; j < numDaysInPast; j++) {
                SweepingEventDate date = checkCalendarForSweeping(schedule, calendar);

                if (date != null) {
                    results.add(date);
                }

                calendar.add(Calendar.DAY_OF_YEAR, -1);
            }
            calendar = new GregorianCalendar(
                    TimeZone.getTimeZone("America/Los_Angeles"), Locale.US);
            for (int j = 0; j < numDaysInFuture; j++) {
                SweepingEventDate date = checkCalendarForSweeping(schedule, calendar);

                if (date != null) {
                    results.add(date);
                }

                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
        }

        return results;
    }

    private static SweepingEventDate checkCalendarForSweeping(LimitSchedule schedule,
                                                              GregorianCalendar calendar) {
        SweepingEventDate result = null;

        int dow = calendar.get(Calendar.DAY_OF_WEEK);
        int dom = calendar.get(Calendar.DAY_OF_MONTH);

        int rangeStart = ((schedule.getWeekNumber() - 1) * 7) + 1;
        int endRange = ((schedule.getWeekNumber()) * 7) + 1;
        if (dom >= rangeStart && dom < (endRange + 1)) {
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

                result = new SweepingEventDate(schedule, potentialStartTime);
            } else if (dow == (schedule.getDayNumber() % 7) + 1) {
                int sweepingLength = schedule.getEndHour() - schedule.getStartHour();
                if (sweepingLength < 0) {
                    sweepingLength += 24;GregorianCalendar potentialEndTime = new GregorianCalendar(
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH),
                            schedule.getStartHour(),
                            0,
                            0);
                    potentialEndTime.add(Calendar.HOUR, sweepingLength);

                    result = new SweepingEventDate(schedule, potentialEndTime);
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

    public static List<SweepingEventDate> getTimeOrderedSweepingDatesForLimitSchedules(
            List<LimitSchedule> schedules, boolean includeEndTime, int startHourOffset) {
        List<SweepingEventDate> sweepingEventDates = new ArrayList<>();
        for (LimitSchedule s : schedules) {
            sweepingEventDates.add(WatchZoneUtils.findNextSweepingDate(s, includeEndTime, startHourOffset));
        }
        WatchZoneUtils.sortByNextEventTime(sweepingEventDates);

        return sweepingEventDates;
    }

    public static long getNextSweepingTime(List<LimitSchedule> schedules) {
        List<SweepingEventDate> allDates = WatchZoneUtils.getTimeOrderedSweepingDatesForLimitSchedules(
                        schedules, false, 0);

        if (allDates.size() > 0) {
            return allDates.get(0).getCalendar().getTime().getTime();
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

    public static long getNextEventTimestampForWatchZone(WatchZoneModel model) {
        long result = -1L;
        if (model.getStatus() == WatchZoneModel.Status.VALID) {
            List<LimitSchedule> allSchedules = new ArrayList<>();
            for (Long limitUid : model.getWatchZoneLimitModelUids()) {
                WatchZoneLimitModel limitModel = model.getWatchZoneLimitModel(limitUid);
                allSchedules.addAll(limitModel.getLimitSchedulesModel().getScheduleList());
            }
            int hourOffset = getHourOffset(model.getWatchZone());
            result = getNextEventTimestampForLimitSchedules(allSchedules, hourOffset);
        }
        return result;
    }

    public static long getNextEventTimestampForLimitSchedules(List<LimitSchedule> schedules,
                                                              int startHourOffset) {
        List<SweepingEventDate> allDates = WatchZoneUtils.getTimeOrderedSweepingDatesForLimitSchedules(
                schedules, true, startHourOffset);

        if (allDates.size() > 0) {
            return allDates.get(0).getCalendar().getTime().getTime();
        } else {
            return -1L;
        }
    }

    private static int getHourOffset(WatchZone watchZone) {
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

    public static List<WatchZoneModel> loadAllWatchZoneModels(final Context context) {
        List<Long> watchZoneModelUuids = WatchZoneRepository.getInstance(context).getWatchZoneUids();
        final CountDownLatch latch = new CountDownLatch(watchZoneModelUuids.size());

        for (final Long uid : watchZoneModelUuids) {
            WatchZoneModelRepository.getInstance(context).getWatchZoneModel(uid).observeForever(
                    new Observer<WatchZoneModel>() {
                @Override
                public void onChanged(@Nullable WatchZoneModel watchZoneModel) {
                    if (watchZoneModel != null) {
                        if (watchZoneModel.getStatus() != WatchZoneModel.Status.LOADING) {
                            latch.countDown();
                            WatchZoneModelRepository.getInstance(context)
                                    .getWatchZoneModel(uid).removeObserver(this);
                        }
                    }
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return WatchZoneModelRepository.getInstance(context).getWatchZoneModels();
    }
}
