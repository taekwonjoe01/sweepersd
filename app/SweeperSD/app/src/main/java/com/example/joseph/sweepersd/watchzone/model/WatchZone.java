package com.example.joseph.sweepersd.watchzone.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.text.TextUtils;

@Entity(tableName = "watchzones")
public class WatchZone {
    @PrimaryKey(autoGenerate = true)
    private long uid;

    @ColumnInfo(name = "label")
    private String label;

    @ColumnInfo(name = "centerLatitude")
    private double centerLatitude;

    @ColumnInfo(name = "centerLongitude")
    private double centerLongitude;

    @ColumnInfo(name = "radius")
    private int radius;

    @ColumnInfo(name = "lastSweepingUpdated")
    private long lastSweepingUpdated;

    public long getUid() {
        return uid;
    }

    public String getLabel() {
        return label;
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

    public long getLastSweepingUpdated() {
        return lastSweepingUpdated;
    }

    void setUid(long uid) {
        this.uid = uid;
    }

    void setLabel(String label) {
        this.label = label;
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

    void setLastSweepingUpdated(long lastSweepingUpdated) {
        this.lastSweepingUpdated = lastSweepingUpdated;
    }

    public boolean isChanged(WatchZone compareTo) {
        boolean result = false;

        if (this.uid == compareTo.getUid()) {
            if (this.radius != compareTo.getRadius()) {
                result = true;
            } else if (this.centerLatitude != compareTo.getCenterLatitude()) {
                result = true;
            } else if (this.centerLongitude != compareTo.getCenterLongitude()) {
                result = true;
            } else if (!TextUtils.equals(this.label, compareTo.getLabel())) {
                result = true;
            } else if (this.lastSweepingUpdated != compareTo.getLastSweepingUpdated()) {
                result = true;
            }
        }

        return result;
    }
}
