package com.example.joseph.sweepersd.model.watchzone;

import com.example.joseph.sweepersd.model.limits.LimitSchedule;

import java.util.GregorianCalendar;

/**
 * Created by joseph on 9/17/16.
 */
public class SweepingDate {
    private final LimitSchedule mSchedule;
    private final GregorianCalendar mStartTime;
    private final GregorianCalendar mEndTime;

    SweepingDate(LimitSchedule schedule, GregorianCalendar startTime, GregorianCalendar endTime) {
        mSchedule = schedule;
        mStartTime = startTime;
        mEndTime = endTime;
    }

    public LimitSchedule getLimitSchedule() {
        return mSchedule;
    }

    public GregorianCalendar getStartTime() {
        return mStartTime;
    }

    public GregorianCalendar getEndTime() {
        return mEndTime;
    }
}
