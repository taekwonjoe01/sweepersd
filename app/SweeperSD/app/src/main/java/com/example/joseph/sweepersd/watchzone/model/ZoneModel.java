package com.example.joseph.sweepersd.watchzone.model;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Relation;

import com.example.joseph.sweepersd.limit.LimitModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZoneModel {
    @Embedded
    public WatchZone watchZone;

    @Relation(parentColumn = "uid", entityColumn = "watchZoneId", entity = WatchZonePoint.class)
    public List<PointModel> points;

    public Boolean isChanged(ZoneModel compareTo) {
        Boolean result = null;

        if (this.watchZone.getUid() == compareTo.watchZone.getUid()) {
            result = false;
            if (this.watchZone.isChanged(compareTo.watchZone)) {
                result = true;
            } else if (this.points.size() != compareTo.points.size()) {
                result = true;
            } else {
                for (int i = 0; i < this.points.size(); i++) {
                    PointModel myPoint = this.points.get(i);
                    PointModel otherPoint = compareTo.points.get(i);
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
        for (PointModel p : points) {
            if (p.limitModels != null) {
                for (LimitModel limitModel : p.limitModels) {
                    if (!results.containsKey(limitModel.limit.getUid())) {
                        results.put(limitModel.limit.getUid(), limitModel);
                    }
                }
            }
        }
        return results;
    }
}
