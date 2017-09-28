package com.example.joseph.sweepersd.revision3.limit;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;

import static android.arch.persistence.room.ForeignKey.CASCADE;

@Entity(tableName = "limitSchedules", foreignKeys = @ForeignKey(entity = Limit.class,
        parentColumns = "uid",
        childColumns = "limitId", onDelete=CASCADE))
public class LimitSchedule {
    @PrimaryKey(autoGenerate = true)
    private long uid;

    @ColumnInfo(name = "startHour")
    private int startHour;

    @ColumnInfo(name = "endHour")
    private int endHour;

    @ColumnInfo(name = "dayNumber")
    private int dayNumber;

    @ColumnInfo(name = "weekNumber")
    private int weekNumber;

    @ColumnInfo(name = "limitId", index = true)
    private long limitId;

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
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

    public long getLimitId() {
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

    public void setLimitId(long limitId) {
        this.limitId = limitId;
    }
}
