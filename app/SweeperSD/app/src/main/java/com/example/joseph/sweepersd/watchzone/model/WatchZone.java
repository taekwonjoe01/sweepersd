package com.example.joseph.sweepersd.watchzone.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.text.TextUtils;

@Entity(tableName = "watchzones")
public class WatchZone {
    public static final int REMIND_RANGE_48_HOURS = 0;
    public static final int REMIND_RANGE_24_HOURS = 1;
    public static final int REMIND_RANGE_12_HOURS = 2;
    public static final int REMIND_RANGE_DEFAULT = REMIND_RANGE_48_HOURS;

    public static final int REMIND_POLICY_ANYWHERE = 0;
    public static final int REMIND_POLICY_NEARBY = 1;
    public static final int REMIND_POLICY_DEFAULT = REMIND_POLICY_ANYWHERE;

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

    @ColumnInfo(name = "remindRange")
    private int remindRange;

    @ColumnInfo(name = "remindPolicy")
    private int remindPolicy;

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

    public int getRemindRange() {
        return remindRange;
    }

    public int getRemindPolicy() {
        return remindPolicy;
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

    void setRemindRange(int remindRange) {
        this.remindRange = remindRange;
    }

    void setRemindPolicy(int remindPolicy) {
        this.remindPolicy = remindPolicy;
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
            } else if (this.remindRange != compareTo.getRemindRange()) {
                result = true;
            } else if (this.remindPolicy != compareTo.getRemindPolicy()) {
                result = true;
            }
        }

        return result;
    }
}
