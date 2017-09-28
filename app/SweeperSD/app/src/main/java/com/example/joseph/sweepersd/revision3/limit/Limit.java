package com.example.joseph.sweepersd.revision3.limit;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "limits")
public class Limit {
    @PrimaryKey(autoGenerate = true)
    private long uid;

    @ColumnInfo(name = "street")
    private String street;

    @ColumnInfo(name = "startRange")
    private int startRange;

    @ColumnInfo(name = "endRange")
    private int endRange;

    @ColumnInfo(name = "rawLimitString")
    private String rawLimitString;

    @ColumnInfo(name = "isPosted")
    private boolean isPosted;

    public long getUid() {
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

    public void setUid(long uid) {
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

    public void setPosted(boolean posted) {
        this.isPosted = posted;
    }

    public boolean isPosted() {
        return isPosted;
    }
}
