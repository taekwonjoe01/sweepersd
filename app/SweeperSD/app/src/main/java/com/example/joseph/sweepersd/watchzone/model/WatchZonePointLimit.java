package com.example.joseph.sweepersd.watchzone.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;

import static android.arch.persistence.room.ForeignKey.CASCADE;

@Entity(tableName = "watchzonepointlimits", foreignKeys = @ForeignKey(entity = WatchZonePoint.class,
        parentColumns = "uid",
        childColumns = "watchZonePointId", onDelete=CASCADE))
public class WatchZonePointLimit {
    @PrimaryKey(autoGenerate = true)
    private long uid;

    @ColumnInfo(name = "limitId")
    private long limitId;

    @ColumnInfo(name = "watchZonePointId", index = true)
    private long watchZonePointId;

    public void setUid(long uid) {
        this.uid = uid;
    }

    public void setLimitId(long limitId) {
        this.limitId = limitId;
    }

    public void setWatchZonePointId(long watchZonePointId) {
        this.watchZonePointId = watchZonePointId;
    }

    public long getUid() {
        return uid;
    }

    public long getLimitId() {
        return limitId;
    }

    public long getWatchZonePointId() {
        return watchZonePointId;
    }
}
