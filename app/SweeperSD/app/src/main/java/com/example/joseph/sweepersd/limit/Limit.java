package com.example.joseph.sweepersd.limit;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
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

    public void setUid(long uid) {
        this.uid = uid;
    }

    public void setAddressValidatedTimestamp(long addressValidatedTimestamp) {
        this.addressValidatedTimestamp = addressValidatedTimestamp;
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

    public Boolean isChanged(Limit compareTo) {
        Boolean result = null;

        if (this.uid == compareTo.getUid()) {
            result = false;
            if (this.startRange != compareTo.getStartRange()) {
                result = true;
            } else if (this.endRange != compareTo.getEndRange()) {
                result = true;
            } else if (!TextUtils.equals(this.street, compareTo.getStreet())) {
                result = true;
            } else if (!TextUtils.equals(this.rawLimitString, compareTo.getRawLimitString())) {
                result = true;
            }
        }
        return result;
    }
}
