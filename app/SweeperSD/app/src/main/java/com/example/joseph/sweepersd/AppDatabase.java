package com.example.joseph.sweepersd;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import android.content.Context;

import com.example.joseph.sweepersd.alert.geofence.WatchZoneFence;
import com.example.joseph.sweepersd.alert.geofence.WatchZoneFenceDao;
import com.example.joseph.sweepersd.experimental.activityrecognition.ActivityReport;
import com.example.joseph.sweepersd.experimental.activityrecognition.ActivityReportDao;
import com.example.joseph.sweepersd.limit.Limit;
import com.example.joseph.sweepersd.limit.LimitDao;
import com.example.joseph.sweepersd.limit.LimitSchedule;
import com.example.joseph.sweepersd.watchzone.model.WatchZone;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneDao;
import com.example.joseph.sweepersd.watchzone.model.WatchZonePoint;
import com.example.joseph.sweepersd.watchzone.model.WatchZonePointLimit;

@Database(entities = {Limit.class, LimitSchedule.class, WatchZone.class, WatchZonePoint.class,
        ActivityReport.class, WatchZoneFence.class, WatchZonePointLimit.class },
        version = 1, exportSchema = false)
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

    public abstract WatchZoneFenceDao watchZoneFenceDao();
}
