package com.example.joseph.sweepersd.revision3.watchzone;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

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

    public void setUid(long uid) {
        this.uid = uid;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setCenterLatitude(double centerLatitude) {
        this.centerLatitude = centerLatitude;
    }

    public void setCenterLongitude(double centerLongitude) {
        this.centerLongitude = centerLongitude;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public void setLastSweepingUpdated(long lastSweepingUpdated) {
        this.lastSweepingUpdated = lastSweepingUpdated;
    }
}
