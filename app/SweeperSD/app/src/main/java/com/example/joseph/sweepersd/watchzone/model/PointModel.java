package com.example.joseph.sweepersd.watchzone.model;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Relation;

import com.example.joseph.sweepersd.limit.Limit;
import com.example.joseph.sweepersd.limit.LimitModel;

import java.util.Set;

public class PointModel {
    @Embedded
    public WatchZonePoint point;

    @Relation(parentColumn = "limitId", entityColumn = "uid", entity = Limit.class)
    public Set<LimitModel> limitModels;
}
