package com.example.joseph.sweepersd.revision3;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;

@Entity(foreignKeys = @ForeignKey(entity = Limit.class,
        parentColumns = "uid",
        childColumns = "limitId"))
public class LimitSchedule {
    @PrimaryKey
    private int uid;

    private int startHour;
    private int endHour;
    private int dayNumber;
    private int weekNumber;
    private int limitId;

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public int getStartHour() {
        return startHour;
    }

    public int getEndHour() {
        return endHour;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public int getWeekNumber() {
        return weekNumber;
    }

    public int getLimitId() {
        return limitId;
    }

    public void setStartHour(int startHour) {
        this.startHour = startHour;
    }

    public void setEndHour(int endHour) {
        this.endHour = endHour;
    }

    public void setDayNumber(int dayNumber) {
        this.dayNumber = dayNumber;
    }

    public void setWeekNumber(int weekNumber) {
        this.weekNumber = weekNumber;
    }

    public void setLimitId(int limitId) {
        this.limitId = limitId;
    }
}
