package com.example.joseph.sweepersd.revision3;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

import com.example.joseph.sweepersd.model.limits.LimitSchedule;

import java.util.List;

@Entity(tableName = "limits")
public class Limit {

    @PrimaryKey
    private int uid;

    @ColumnInfo(name = "street")
    private String street;

    @ColumnInfo(name = "startRange")
    private int startRange;

    @ColumnInfo(name = "endRange")
    private int endRange;

    @ColumnInfo(name = "rawLimitString")
    private String rawLimitString;

    @Ignore
    private List<LimitSchedule> mSchedules;

    public int getUid() {
        return uid;
    }

    public String getStreet() {
        return street;
    }

    public int getStartRange() {
        return startRange;
    }

    public int getEndRange() {
        return endRange;
    }

    public String getRawLimitString() {
        return rawLimitString;
    }

    public List<LimitSchedule> getmSchedules() {
        return mSchedules;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public void setStartRange(int startRange) {
        this.startRange = startRange;
    }

    public void setEndRange(int endRange) {
        this.endRange = endRange;
    }

    public void setRawLimitString(String rawLimitString) {
        this.rawLimitString = rawLimitString;
    }

    public void setmSchedules(List<LimitSchedule> mSchedules) {
        this.mSchedules = mSchedules;
    }
}
