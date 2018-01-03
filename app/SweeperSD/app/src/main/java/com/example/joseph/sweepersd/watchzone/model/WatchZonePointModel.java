package com.example.joseph.sweepersd.watchzone.model;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Relation;

import java.util.List;

public class WatchZonePointModel {
    @Embedded
    public WatchZonePoint point;

    @Relation(parentColumn = "uid", entityColumn = "watchZonePointId", entity = WatchZonePointLimit.class)
    public List<WatchZonePointLimitModel> pointLimitModels;

    public Boolean isChanged(WatchZonePointModel compareTo) {
        Boolean result = point.isChanged(compareTo.point);

        if (result != null && !result) {
            if (this.pointLimitModels.size() != compareTo.pointLimitModels.size()) {
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
        }

        return result;
    }
}
