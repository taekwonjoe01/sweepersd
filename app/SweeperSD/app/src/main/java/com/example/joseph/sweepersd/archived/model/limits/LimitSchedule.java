package com.example.joseph.sweepersd.archived.model.limits;

/**
 * Created by joseph on 8/27/16.
 */
public class LimitSchedule {
    private final int mStartHour;
    private final int mEndHour;
    private final int mDay;
    private final int mWeekNumber;

    public LimitSchedule(int startHour, int endHour, int day, int weekNumber) {
        mStartHour = startHour;
        mEndHour = endHour;
        mDay = day;
        mWeekNumber = weekNumber;
    }

    public int getStartHour() {
        return mStartHour;
    }

    public int getEndHour() {
        return mEndHour;
    }

    public int getDay() {
        return mDay;
    }

    public int getWeekNumber() {
        return mWeekNumber;
    }
}
