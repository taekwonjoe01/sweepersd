package com.example.joseph.sweepersd.revision3.watchzone;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;

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

    public void setLimitId(long limitId) {
        this.limitId = limitId;
    }

    public void setWatchZoneId(long watchZoneId) {
        this.watchZoneId = watchZoneId;
    }
}
