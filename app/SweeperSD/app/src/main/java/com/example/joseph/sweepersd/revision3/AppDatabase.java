package com.example.joseph.sweepersd.revision3;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import com.example.joseph.sweepersd.revision3.limit.Limit;
import com.example.joseph.sweepersd.revision3.limit.LimitDao;
import com.example.joseph.sweepersd.revision3.limit.LimitSchedule;
import com.example.joseph.sweepersd.revision3.watchzone.model.WatchZone;
import com.example.joseph.sweepersd.revision3.watchzone.model.WatchZoneDao;
import com.example.joseph.sweepersd.revision3.watchzone.model.WatchZonePoint;

@Database(entities = {Limit.class, LimitSchedule.class, WatchZone.class, WatchZonePoint.class},
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
}
