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
public interface GeofenceDao {
    @Query("SELECT * FROM geofences")
    LiveData<List<Geofence>> getAllGeofencesLiveData();

    @Query("SELECT * FROM geofences")
    List<Geofence> getAllGeofences();

    @Insert(onConflict = REPLACE)
    long[] insertGeofences(List<Geofence> geofences);

    @Insert(onConflict = REPLACE)
    long insertGeofence(Geofence geofence);

    @Update(onConflict = REPLACE)
    void updateGeofence(Geofence geofence);

    @Delete
    void delete(Geofence geofence);
}
