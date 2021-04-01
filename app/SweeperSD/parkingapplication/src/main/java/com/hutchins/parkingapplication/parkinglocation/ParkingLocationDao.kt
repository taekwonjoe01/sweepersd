package com.hutchins.parkingapplication.parkinglocation

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Created by joeyhutchins on 3/27/21.
 */
@Dao
interface ParkingLocationDao {
    @get:Query("SELECT * FROM parkingLocationRecords ORDER BY timestamp DESC LIMIT 1")
    val lastParkingLocationRecord: ParkingLocationRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addParkingLocationRecord(parkingLocationRecord: ParkingLocationRecord): Long

    @get:Query("SELECT * FROM parkingLocationRecords")
    val parkingLocationRecords: List<ParkingLocationRecord>
}