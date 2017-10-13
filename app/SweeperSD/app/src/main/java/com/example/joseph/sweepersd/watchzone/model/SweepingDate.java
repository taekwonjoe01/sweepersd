package com.example.joseph.sweepersd.watchzone.model;


import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import com.example.joseph.sweepersd.limit.LimitSchedule;

import java.util.GregorianCalendar;

@Entity(tableName = "sweepingDates")
public class SweepingDate {
    @PrimaryKey(autoGenerate = true)
    private long uid;

    @ColumnInfo(name = "watchZoneId")
    private long watchZoneId;
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
