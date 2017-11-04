package com.example.joseph.sweepersd.watchzone.model;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Relation;

import java.util.List;

public class ZoneModel {
    @Embedded
    public WatchZone watchZone;

    @Relation(parentColumn = "uid", entityColumn = "watchZoneId", entity = WatchZonePoint.class)
    public List<PointModel> points;
}
