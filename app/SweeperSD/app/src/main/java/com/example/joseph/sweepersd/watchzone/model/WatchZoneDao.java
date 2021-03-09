package com.example.joseph.sweepersd.watchzone.model;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

import static androidx.room.OnConflictStrategy.REPLACE;

@Dao
public interface WatchZoneDao {
    @Transaction @Query("SELECT * FROM watchzones")
    LiveData<List<WatchZoneModel>> getAllZonesLiveData();

    @Transaction @Query("SELECT * FROM watchzones WHERE uid LIKE :uid LIMIT 1")
    LiveData<WatchZoneModel> getZoneLiveDataForUid(long uid);

    @Transaction @Query("SELECT * FROM watchzones")
    List<WatchZone> getAllZones();

    @Query("SELECT * FROM watchzonepoints WHERE watchZoneId LIKE :uid")
    List<WatchZonePoint> getWatchZonePointsForWatchZoneId(long uid);

    @Query("SELECT * FROM watchzonepoints")
    List<WatchZonePoint> getAllWatchZonePoints();

    @Query("SELECT * FROM watchzonepointlimits WHERE watchZonePointId LIKE :pointUid")
    List<WatchZonePointLimit> getWatchZonePointLimits(long pointUid);

    @Query("SELECT * FROM watchzones WHERE uid LIKE :uid LIMIT 1")
    WatchZone getWatchZone(long uid);

    @Insert(onConflict = REPLACE)
    long insertWatchZone(WatchZone watchZone);

    @Insert(onConflict = REPLACE)
    long[] insertWatchZonePoints(List<WatchZonePoint> points);

    @Insert(onConflict = REPLACE)
    long[] insertWatchZonePointLimits(List<WatchZonePointLimit> pointLimits);

    @Update(onConflict = REPLACE)
    int updateWatchZone(WatchZone watchZone);

    @Update(onConflict = REPLACE)
    int updateWatchZonePoint(WatchZonePoint point);

    @Delete
    int deleteWatchZone(WatchZone watchZone);

    @Delete
    int deleteWatchZonePoints(List<WatchZonePoint> watchZonePoints);

    @Delete
    int deleteWatchZonePointLimits(List<WatchZonePointLimit> watchZonePointLimits);
}
