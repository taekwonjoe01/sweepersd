package com.example.joseph.sweepersd.revision3.watchzone;

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

    public boolean areSame(WatchZone compareTo) {
        boolean result = true;

        if (this.uid != compareTo.getUid()) {
            result = false;
        } else if (this.radius != compareTo.getRadius()) {
            result = false;
        } else if (this.centerLatitude != compareTo.getCenterLatitude()) {
            result = false;
        } else if (this.centerLatitude != compareTo.getCenterLongitude()) {
            result = false;
        } else if (TextUtils.equals(this.label, compareTo.getLabel())) {
            result = false;
        }

        return result;
    }
}
