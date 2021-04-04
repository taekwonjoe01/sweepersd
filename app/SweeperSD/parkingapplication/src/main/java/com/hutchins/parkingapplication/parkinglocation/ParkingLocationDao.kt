package com.hutchins.parkingapplication.parkinglocation

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Created by joeyhutchins on 3/27/21.
 */
@Dao
interface ParkingLocationDao {
    @get:Query("SELECT * FROM parkingLocationRecords ORDER BY timestamp DESC LIMIT 1")
    val lastParkingLocationRecord: Flow<ParkingLocationRecord?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addParkingLocationRecord(parkingLocationRecord: ParkingLocationRecord): Long

    @get:Query("SELECT * FROM parkingLocationRecords")
    val parkingLocationRecords: Flow<List<ParkingLocationRecord>>

    @Query("SELECT * FROM parkingLocationRecords WHERE recordId IN (:recordIds)")
    fun getParkingLocationRecords(recordIds: List<Long>): Flow<List<ParkingLocationRecord>>

    @Query("SELECT * FROM parkingLocationRecords WHERE recordId = :recordId")
    fun getParkingLocationRecord(recordId: Long): Flow<ParkingLocationRecord>
}