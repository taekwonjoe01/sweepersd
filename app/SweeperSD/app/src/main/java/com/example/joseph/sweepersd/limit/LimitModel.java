package com.example.joseph.sweepersd.limit;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Relation;

import java.util.List;

public class LimitModel {
    @Embedded
    public Limit limit;
    @Relation(parentColumn = "uid", entityColumn = "limitId")
    public List<LimitSchedule> schedules;

    public boolean isChanged(LimitModel compareTo) {
        boolean result = false;

        if (this.limit.isChanged(compareTo.limit)) {
            result = true;
        } else if (this.schedules.size() != compareTo.schedules.size()) {
            result = true;
        } else {
            for (int i = 0; i < schedules.size(); i++) {
                LimitSchedule mySchedule = this.schedules.get(i);
                LimitSchedule otherSchedule = compareTo.schedules.get(i);
                if (mySchedule.isChanged(otherSchedule)) {
                    result = true;
                    break;
                }
            }
        }

        return result;
    }
}
