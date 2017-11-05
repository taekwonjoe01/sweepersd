package com.example.joseph.sweepersd.watchzone.model;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Relation;

import com.example.joseph.sweepersd.limit.Limit;
import com.example.joseph.sweepersd.limit.LimitModel;

import java.util.List;

public class PointModel {
    @Embedded
    public WatchZonePoint point;

    @Relation(parentColumn = "limitId", entityColumn = "uid", entity = Limit.class)
    public List<LimitModel> limitModels;

    public boolean isChanged(PointModel compareTo) {
        boolean result = false;

        if (this.point.getUid() != compareTo.point.getUid()) {
            result = true;
        } else if (point.isChanged(compareTo.point)) {
            result = true;
        } else if (this.limitModels.size() != compareTo.limitModels.size()) {
            result = true;
        } else {
            for (int i = 0; i < limitModels.size(); i++) {
                LimitModel myModel = limitModels.get(i);
                LimitModel otherModel = compareTo.limitModels.get(i);
                if (myModel.isChanged(otherModel)) {
                    result = true;
                    break;
                }
            }
        }

        return result;
    }
}
