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

    void setUid(long uid) {
        this.uid = uid;
    }

    void setStreet(String street) {
        this.street = street;
    }

    void setStartRange(int startRange) {
        this.startRange = startRange;
    }

    void setEndRange(int endRange) {
        this.endRange = endRange;
    }

    void setRawLimitString(String rawLimitString) {
        this.rawLimitString = rawLimitString;
    }

    void setPosted(boolean posted) {
        this.isPosted = posted;
    }

    public boolean isPosted() {
        return isPosted;
    }
}
