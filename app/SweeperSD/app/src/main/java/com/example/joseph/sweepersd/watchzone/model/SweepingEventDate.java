package com.example.joseph.sweepersd.watchzone.model;


import com.example.joseph.sweepersd.limit.LimitSchedule;

import java.util.GregorianCalendar;

public class SweepingEventDate {
    private final LimitSchedule mSchedule;
    private final GregorianCalendar mStartTime;

    SweepingEventDate(LimitSchedule schedule, GregorianCalendar startTime) {
        mSchedule = schedule;
        mStartTime = startTime;
    }

    public LimitSchedule getLimitSchedule() {
        return mSchedule;
    }

    public GregorianCalendar getCalendar() {
        return mStartTime;
    }
}
