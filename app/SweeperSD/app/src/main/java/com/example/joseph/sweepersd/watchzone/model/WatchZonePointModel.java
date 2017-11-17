package com.example.joseph.sweepersd.watchzone.model;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Relation;

import com.example.joseph.sweepersd.limit.Limit;
import com.example.joseph.sweepersd.limit.LimitModel;

import java.util.List;

public class WatchZonePointModel {
    @Embedded
    public WatchZonePoint point;

    @Relation(parentColumn = "uid", entityColumn = "watchZonePointId", entity = WatchZonePointLimit.class)
    public List<WatchZonePointLimitModel> pointLimitModels;

    public boolean isChanged(WatchZonePointModel compareTo) {
        boolean result = false;

        if (this.point.getUid() != compareTo.point.getUid()) {
            result = true;
        } else if (point.isChanged(compareTo.point)) {
            result = true;
        } else if (this.pointLimitModels.size() != compareTo.pointLimitModels.size()) {
            result = true;
        } else {
            for (int i = 0; i < pointLimitModels.size(); i++) {
                WatchZonePointLimitModel myModel = pointLimitModels.get(i);
                WatchZonePointLimitModel otherModel = compareTo.pointLimitModels.get(i);
                if (myModel.pointLimit.getUid() != otherModel.pointLimit.getUid()) {
                    result = true;
                    break;
                }
            }
        }

        return result;
    }
}
