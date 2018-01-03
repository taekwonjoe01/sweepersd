package com.example.joseph.sweepersd.alert.geofence;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "watchzonefences")
public class WatchZoneFence {
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "watchZoneId")
    private long watchZoneId;

    @ColumnInfo(name = "inRegion")
    private boolean inRegion;

    @ColumnInfo(name = "centerLatitude")
    private double centerLatitude;

    @ColumnInfo(name = "centerLongitude")
    private double centerLongitude;

    @ColumnInfo(name = "radius")
    private int radius;

    void setWatchZoneId(long watchZoneId) {
        this.watchZoneId = watchZoneId;
    }

    public long getWatchZoneId() {
        return watchZoneId;
    }

    public boolean isInRegion() {
        return inRegion;
    }

    public void setInRegion(boolean inRegion) {
        this.inRegion = inRegion;
    }

    public double getCenterLatitude() {
        return centerLatitude;
    }

    public double getCenterLongitude() {
        return centerLongitude;
    }

    public int getRadius() {
        return radius;
    }

    void setCenterLatitude(double centerLatitude) {
        this.centerLatitude = centerLatitude;
    }

    void setCenterLongitude(double centerLongitude) {
        this.centerLongitude = centerLongitude;
    }

    void setRadius(int radius) {
        this.radius = radius;
    }

    public Boolean isChanged(WatchZoneFence compareTo) {
        Boolean result = null;

        if (this.watchZoneId == compareTo.getWatchZoneId()) {
            result = false;
            if (this.radius != compareTo.getRadius()) {
                result = true;
            } else if (this.centerLatitude != compareTo.getCenterLatitude()) {
                result = true;
            } else if (this.centerLongitude != compareTo.getCenterLongitude()) {
                result = true;
            } else if (!inRegion && compareTo.isInRegion()) {
                result = true;
            } else if (inRegion && !compareTo.isInRegion()) {
                result = true;
            }
        }

        return result;
    }
}
