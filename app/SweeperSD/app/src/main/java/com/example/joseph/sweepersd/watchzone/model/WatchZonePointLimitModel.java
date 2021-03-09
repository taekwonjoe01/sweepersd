package com.example.joseph.sweepersd.watchzone.model;


import androidx.room.Embedded;
import androidx.room.Relation;

import com.example.joseph.sweepersd.limit.Limit;
import com.example.joseph.sweepersd.limit.LimitModel;

import java.util.List;

public class WatchZonePointLimitModel {
    @Embedded
    public WatchZonePointLimit pointLimit;

    @Relation(parentColumn = "limitId", entityColumn = "uid", entity = Limit.class)
    public List<LimitModel> limitModels;
}
