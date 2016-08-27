package com.example.joseph.sweepersd.limits;

/**
 * Created by joseph on 8/27/16.
 */
public class LimitSchedule {
    private final int mStartHour;
    private final int mEndHour;
    private final int mDay;

    public LimitSchedule(int startHour, int endHour, int day) {
        mStartHour = startHour;
        mEndHour = endHour;
        mDay = day;
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
}
