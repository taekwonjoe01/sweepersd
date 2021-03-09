package com.example.joseph.sweepersd.alert.geofence;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import static androidx.room.OnConflictStrategy.REPLACE;

@Dao
public interface WatchZoneFenceDao {
    @Query("SELECT * FROM watchzonefences")
    LiveData<List<WatchZoneFence>> getAllGeofencesLiveData();

    @Query("SELECT * FROM watchzonefences")
    List<WatchZoneFence> getAllGeofences();

    @Query("SELECT * FROM watchzonefences WHERE watchZoneId LIKE (:watchZoneId) LIMIT 1")
    WatchZoneFence getFenceByWatchZoneUid(long watchZoneId);

    @Insert(onConflict = REPLACE)
    long[] insertGeofences(List<WatchZoneFence> watchZoneFences);

    @Insert(onConflict = REPLACE)
    long insertGeofence(WatchZoneFence watchZoneFence);

    @Update(onConflict = REPLACE)
    void updateGeofence(WatchZoneFence watchZoneFence);

    @Update(onConflict = REPLACE)
    void updateGeofences(List<WatchZoneFence> watchZoneFences);

    @Delete
    void delete(WatchZoneFence watchZoneFence);
}
