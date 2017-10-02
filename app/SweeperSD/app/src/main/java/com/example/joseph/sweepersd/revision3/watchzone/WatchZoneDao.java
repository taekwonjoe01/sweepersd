package com.example.joseph.sweepersd.revision3.watchzone;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

@Dao
public interface WatchZoneDao {
    @Query("SELECT * FROM watchzones")
    LiveData<List<WatchZone>> getAllWatchZonesLiveData();

    @Query("SELECT * FROM watchzones")
    List<WatchZone> getAllWatchZones();

    @Query("SELECT * FROM watchzonepoints WHERE watchZoneId LIKE :uid")
    LiveData<List<WatchZonePoint>> getWatchZonePointsLiveData(long uid);

    @Query("SELECT * FROM watchzonepoints WHERE watchZoneId LIKE :uid")
    List<WatchZonePoint> getWatchZonePoints(long uid);

    @Query("SELECT * FROM watchzonepoints WHERE uid LIKE :uid LIMIT 1")
    WatchZonePoint getWatchZonePoint(long uid);

    @Query("SELECT * FROM watchzones WHERE uid LIKE :uid")
    LiveData<WatchZone> getWatchZoneLiveData(long uid);

    @Query("SELECT * FROM watchzones WHERE uid LIKE :uid LIMIT 1")
    WatchZone getWatchZone(long uid);

    @Insert(onConflict = REPLACE)
    long insertWatchZone(WatchZone watchZone);

    @Insert(onConflict = REPLACE)
    long insertWatchZonePoint(WatchZonePoint point);

    @Insert(onConflict = REPLACE)
    long[] insertWatchZonePoints(List<WatchZonePoint> points);

    @Update(onConflict = REPLACE)
    int updateWatchZone(WatchZone watchZone);

    @Update(onConflict = REPLACE)
    int updateWatchZonePoint(WatchZonePoint point);

    @Delete
    int deleteWatchZone(WatchZone watchZone);

    @Delete
    int deleteWatchZonePoints(List<WatchZonePoint> watchZonePoints);
}
