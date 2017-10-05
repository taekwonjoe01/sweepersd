package com.example.joseph.sweepersd.revision3.watchzone;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;
import android.text.TextUtils;

import static android.arch.persistence.room.ForeignKey.CASCADE;

@Entity(tableName = "watchzonepoints", foreignKeys = @ForeignKey(entity = WatchZone.class,
        parentColumns = "uid",
        childColumns = "watchZoneId", onDelete=CASCADE))
public class WatchZonePoint {
    @PrimaryKey(autoGenerate = true)
    private long uid;

    @ColumnInfo(name = "longitude")
    private double longitude;

    @ColumnInfo(name = "latitude")
    private double latitude;

    @ColumnInfo(name = "address")
    private String address;

    @ColumnInfo(name = "limitId")
    private long limitId;

    @ColumnInfo(name = "watchZoneId")
    private long watchZoneId;

    @ColumnInfo(name = "watchZoneUpdatedTimestampMs")
    private long watchZoneUpdatedTimestampMs;

    public long getUid() {
        return uid;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public String getAddress() {
        return address;
    }

    public long getLimitId() {
        return limitId;
    }

    public long getWatchZoneId() {
        return watchZoneId;
    }

    public long getWatchZoneUpdatedTimestampMs() {
        return watchZoneUpdatedTimestampMs;
    }

    void setUid(long uid) {
        this.uid = uid;
    }

    void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    void setAddress(String address) {
        this.address = address;
    }

    void setLimitId(long limitId) {
        this.limitId = limitId;
    }

    void setWatchZoneId(long watchZoneId) {
        this.watchZoneId = watchZoneId;
    }

    void setWatchZoneUpdatedTimestampMs(long timestamp) {
        watchZoneUpdatedTimestampMs = timestamp;
    }

    public boolean isChanged(WatchZonePoint compareTo) {
        boolean result = false;

        if (this.uid == compareTo.getUid()) {
            if (this.longitude != compareTo.getLongitude()) {
                result = true;
            } else if (this.latitude != compareTo.getLatitude()) {
                result = true;
            } else if (TextUtils.equals(this.address, compareTo.getAddress())) {
                result = true;
            } else if (this.limitId != compareTo.getLimitId()) {
                result = true;
            } else if (this.watchZoneId != compareTo.getWatchZoneId()) {
                result = true;
            } else if (this.watchZoneUpdatedTimestampMs != compareTo.getWatchZoneUpdatedTimestampMs()) {
                result = true;
            }
        }

        return result;
    }
}
