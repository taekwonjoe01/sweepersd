package com.example.joseph.sweepersd.limit;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Relation;

import java.util.List;

public class LimitModel {
    @Embedded
    public Limit limit;
    @Relation(parentColumn = "uid", entityColumn = "limitId")
    public List<LimitSchedule> schedules;
}
