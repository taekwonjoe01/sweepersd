package com.example.joseph.sweepersd;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import com.example.joseph.sweepersd.experimental.activityrecognition.ActivityReport;
import com.example.joseph.sweepersd.experimental.activityrecognition.ActivityReportDao;
import com.example.joseph.sweepersd.limit.Limit;
import com.example.joseph.sweepersd.limit.LimitDao;
import com.example.joseph.sweepersd.limit.LimitSchedule;
import com.example.joseph.sweepersd.watchzone.model.WatchZone;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneDao;
import com.example.joseph.sweepersd.watchzone.model.WatchZonePoint;

@Database(entities = {Limit.class, LimitSchedule.class, WatchZone.class, WatchZonePoint.class,
        ActivityReport.class},
        version = 1)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase sInstance;

    public static AppDatabase getInstance(Context context) {
        if (sInstance == null) {
            sInstance = Room.databaseBuilder(context.getApplicationContext(),
                    AppDatabase.class, "AppDatabase").allowMainThreadQueries().build();
        }
        return sInstance;
    }

    public abstract LimitDao limitDao();

    public abstract WatchZoneDao watchZoneDao();

    public abstract ActivityReportDao activityReportDao();
}
