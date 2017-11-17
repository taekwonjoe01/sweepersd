package com.example.joseph.sweepersd.watchzone.model;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Relation;

import com.example.joseph.sweepersd.limit.LimitModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WatchZoneModel {
    @Embedded
    public WatchZone watchZone;

    @Relation(parentColumn = "uid", entityColumn = "watchZoneId", entity = WatchZonePoint.class)
    public List<WatchZonePointModel> points;

    public Boolean isChanged(WatchZoneModel compareTo) {
        Boolean result = null;

        if (this.watchZone.getUid() == compareTo.watchZone.getUid()) {
            result = false;
            if (this.watchZone.isChanged(false, compareTo.watchZone)) {
                result = true;
            } else if (this.points.size() != compareTo.points.size()) {
                result = true;
            } else {
                for (int i = 0; i < this.points.size(); i++) {
                    WatchZonePointModel myPoint = this.points.get(i);
                    WatchZonePointModel otherPoint = compareTo.points.get(i);
                    if (myPoint.isChanged(otherPoint)) {
                        result = true;
                        break;
                    }
                }
            }
        }

        return result;
    }

    public Map<Long, LimitModel> getUniqueLimitModels() {
        Map<Long, LimitModel> results = new HashMap<>();
        for (WatchZonePointModel p : points) {
            if (p.pointLimitModels != null) {
                for (WatchZonePointLimitModel pointLimitModel : p.pointLimitModels) {
                    if (pointLimitModel.limitModels != null) {
                        for (LimitModel limitModel : pointLimitModel.limitModels) {
                            if (!results.containsKey(limitModel.limit.getUid())) {
                                results.put(limitModel.limit.getUid(), limitModel);
                            }
                        }
                    }
                }
            }
        }
        return results;
    }
}
