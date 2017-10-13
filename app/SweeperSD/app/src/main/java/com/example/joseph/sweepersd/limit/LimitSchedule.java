package com.example.joseph.sweepersd.limit;

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

    void setUid(long uid) {
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

    void setStartHour(int startHour) {
        this.startHour = startHour;
    }

    void setEndHour(int endHour) {
        this.endHour = endHour;
    }

    void setDayNumber(int dayNumber) {
        this.dayNumber = dayNumber;
    }

    void setWeekNumber(int weekNumber) {
        this.weekNumber = weekNumber;
    }

    void setLimitId(long limitId) {
        this.limitId = limitId;
    }

    public boolean isChanged(LimitSchedule compareTo) {
        boolean result = false;

        if (this.uid == compareTo.getUid()) {
            if (this.startHour != compareTo.getStartHour()) {
                result = true;
            } else if (this.endHour != compareTo.getEndHour()) {
                result = true;
            } else if (this.dayNumber != compareTo.getDayNumber()) {
                result = true;
            } else if (this.weekNumber != compareTo.getWeekNumber()) {
                result = true;
            } else if (this.limitId != compareTo.getLimitId()) {
                result = true;
            }
        }

        return result;
    }
}
