package com.example.joseph.sweepersd.limit;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.text.TextUtils;

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

    @ColumnInfo(name = "addressValidatedTimestamp")
    private long addressValidatedTimestamp;

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

    public long getAddressValidatedTimestamp() {
        return addressValidatedTimestamp;
    }

    void setUid(long uid) {
        this.uid = uid;
    }

    void setAddressValidatedTimestamp(long addressValidatedTimestamp) {
        this.addressValidatedTimestamp = addressValidatedTimestamp;
    }

    void setStreet(String street) {
        this.street = street;
    }

    public void setStartRange(int startRange) {
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

    public boolean isChanged(Limit compareTo) {
        boolean result = false;

        if (this.uid == compareTo.getUid()) {
            if (this.startRange != compareTo.getStartRange()) {
                result = true;
            } else if (this.endRange != compareTo.getEndRange()) {
                result = true;
            } else if (TextUtils.equals(this.street, compareTo.getStreet())) {
                result = true;
            } else if (TextUtils.equals(this.rawLimitString, compareTo.getRawLimitString())) {
                result = true;
            }
        }

        return result;
    }
}
