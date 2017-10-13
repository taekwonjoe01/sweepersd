package com.example.joseph.sweepersd.archived.model.limits;

import java.util.List;

/**
 * Created by joseph on 3/13/16.
 */
public class Limit {
    private final int mId;
    private final String mStreet;
    private final int[] mRange;
    private final String mLimit;
    private final List<LimitSchedule> mSchedules;

    public Limit(int id, String street, int[] range, String limit, List<LimitSchedule> schedules) {
        mId = id;
        mStreet = street;
        mRange = range;
        mLimit = limit;
        mSchedules = schedules;
    }

    public int getId() {
        return mId;
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

    @Override
    public boolean equals(Object o) {
        boolean equals = false;
        if (o instanceof Limit) {
            Limit other = (Limit) o;
            equals = this.mId == other.mId;
        }
        return equals;
    }
}
