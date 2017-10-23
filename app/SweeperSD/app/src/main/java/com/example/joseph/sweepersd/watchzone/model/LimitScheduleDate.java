package com.example.joseph.sweepersd.watchzone.model;


import com.example.joseph.sweepersd.limit.LimitSchedule;

import java.util.GregorianCalendar;

public class LimitScheduleDate {
    private final LimitSchedule mSchedule;
    private final GregorianCalendar mStartCalendar;
    private final GregorianCalendar mEndCalendar;

    LimitScheduleDate(LimitSchedule schedule, GregorianCalendar startCalendar,
                      GregorianCalendar endCalendar) {
        mSchedule = schedule;
        mStartCalendar = startCalendar;
        mEndCalendar = endCalendar;
    }

    public LimitSchedule getLimitSchedule() {
        return mSchedule;
    }

    public GregorianCalendar getStartCalendar() {
        return mStartCalendar;
    }

    public GregorianCalendar getEndCalendar() {
        return mEndCalendar;
    }
}
