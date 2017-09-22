package com.example.joseph.sweepersd.revision3;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {Limit.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract LimitDao limitDao();
}
