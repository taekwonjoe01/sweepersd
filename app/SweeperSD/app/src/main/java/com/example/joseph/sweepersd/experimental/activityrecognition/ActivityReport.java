package com.example.joseph.sweepersd.experimental.activityrecognition;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "activityreports")
public class ActivityReport {
    @PrimaryKey(autoGenerate = true)
    private long uid;

    @ColumnInfo(name = "vehicleConfidence")
    private int vehicleConfidence;

    @ColumnInfo(name = "footConfidence")
    private int footConfidence;

    @ColumnInfo(name = "bicycleConfidence")
    private int bicycleConfidence;

    @ColumnInfo(name = "walkingConfidence")
    private int walkingConfidence;

    @ColumnInfo(name = "stillConfidence")
    private int stillConfidence;

    @ColumnInfo(name = "tiltingConfidence")
    private int tiltingConfidence;

    @ColumnInfo(name = "runningConfidence")
    private int runningConfidence;

    @ColumnInfo(name = "unknownConfidence")
    private int unknownConfidence;

    @ColumnInfo(name = "activityTimestampMs")
    private long activityTimestampMs;public void setUid(long uid) {
        this.uid = uid;
    }

    void setVehicleConfidence(int vehicleConfidence) {
        this.vehicleConfidence = vehicleConfidence;
    }

    void setFootConfidence(int footConfidence) {
        this.footConfidence = footConfidence;
    }

    void setBicycleConfidence(int bicycleConfidence) {
        this.bicycleConfidence = bicycleConfidence;
    }

    void setWalkingConfidence(int walkingConfidence) {
        this.walkingConfidence = walkingConfidence;
    }

    void setStillConfidence(int stillConfidence) {
        this.stillConfidence = stillConfidence;
    }

    void setTiltingConfidence(int tiltingConfidence) {
        this.tiltingConfidence = tiltingConfidence;
    }

    void setRunningConfidence(int runningConfidence) {
        this.runningConfidence = runningConfidence;
    }

    void setUnknownConfidence(int unknownConfidence) {
        this.unknownConfidence = unknownConfidence;
    }

    void setActivityTimestampMs(long activityTimestampMs) {
        this.activityTimestampMs = activityTimestampMs;
    }

    public long getUid() {
        return uid;
    }

    public int getVehicleConfidence() {
        return vehicleConfidence;
    }

    public int getFootConfidence() {
        return footConfidence;
    }

    public int getBicycleConfidence() {
        return bicycleConfidence;
    }

    public int getWalkingConfidence() {
        return walkingConfidence;
    }

    public int getStillConfidence() {
        return stillConfidence;
    }

    public int getTiltingConfidence() {
        return tiltingConfidence;
    }

    public int getRunningConfidence() {
        return runningConfidence;
    }

    public int getUnknownConfidence() {
        return unknownConfidence;
    }

    public long getActivityTimestampMs() {
        return activityTimestampMs;
    }

}
