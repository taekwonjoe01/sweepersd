package com.example.joseph.sweepersd.alert.geofence;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

@Dao
public interface WatchZoneFenceDao {
    @Query("SELECT * FROM watchzonefences")
    LiveData<List<WatchZoneFence>> getAllGeofencesLiveData();

    @Query("SELECT * FROM watchzonefences")
    List<WatchZoneFence> getAllGeofences();

    @Query("SELECT * FROM watchzonefences WHERE uid LIKE (:uid) LIMIT 1")
    WatchZoneFence getFenceByUid(long uid);

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
