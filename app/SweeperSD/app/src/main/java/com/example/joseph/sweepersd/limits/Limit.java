package com.example.joseph.sweepersd.limits;

import java.util.List;

/**
 * Created by joseph on 3/13/16.
 */
public class Limit {
    private final String mStreet;
    private final int[] mRange;
    private final String mLimit;
    private final List<LimitSchedule> mSchedules;

    public Limit(String street, int[] range, String limit, List<LimitSchedule> schedules) {
        mStreet = street;
        mRange = range;
        mLimit = limit;
        mSchedules = schedules;
    }

    public String getStreet() {
        return mStreet;
    }

    public int[] getRange() {
        return mRange;
    }

    public String getLimit() {
        return mLimit;
    }

    public List<LimitSchedule> getSchedules() {
        return mSchedules;
    }
}
