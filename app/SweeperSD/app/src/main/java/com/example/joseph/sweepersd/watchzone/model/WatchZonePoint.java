package com.example.joseph.sweepersd.watchzone.model;

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

    @ColumnInfo(name = "watchZoneId", index = true)
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

    public long getWatchZoneId() {
        return watchZoneId;
    }

    public long getWatchZoneUpdatedTimestampMs() {
        return watchZoneUpdatedTimestampMs;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setWatchZoneId(long watchZoneId) {
        this.watchZoneId = watchZoneId;
    }

    public void setWatchZoneUpdatedTimestampMs(long timestamp) {
        watchZoneUpdatedTimestampMs = timestamp;
    }

    public boolean isChanged(WatchZonePoint compareTo) {
        boolean result = false;

        if (this.uid == compareTo.getUid()) {
            if (this.longitude != compareTo.getLongitude()) {
                result = true;
            } else if (this.latitude != compareTo.getLatitude()) {
                result = true;
            } else if (!TextUtils.equals(this.address, compareTo.getAddress())) {
                result = true;
            } else if (this.watchZoneId != compareTo.getWatchZoneId()) {
                result = true;
            } else if (this.watchZoneUpdatedTimestampMs != compareTo.getWatchZoneUpdatedTimestampMs()) {
                result = true;
            }
        }

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        boolean result = false;
        if (obj instanceof WatchZonePoint) {
            WatchZonePoint other = (WatchZonePoint) obj;
            result = !isChanged(other);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
