package com.hutchins.parkingapplication.parkinglocation

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow

/**
 * Created by joeyhutchins on 3/27/21.
 */
object ParkingLocationRepo {
    lateinit var applicationContext: Context

    private val parkingLocationDatabase: ParkingLocationDatabase by lazy {
        Room.databaseBuilder(
                applicationContext,
                ParkingLocationDatabase::class.java, "parkingLocationDatabase"
        ).build()
    }

    suspend fun getParkingLocationRecords(): Flow<List<ParkingLocationRecord>> {
        return parkingLocationDatabase.parkingLocationDao().parkingLocationRecords
    }

    suspend fun getParkingLocationRecord(recordId: Long): Flow<ParkingLocationRecord?> {
        return parkingLocationDatabase.parkingLocationDao().getParkingLocationRecord(recordId)
    }

    suspend fun addParkingLocationRecord(parkingLocationRecord: ParkingLocationRecord): Long {
        return parkingLocationDatabase.parkingLocationDao().addParkingLocationRecord(parkingLocationRecord)
    }

    suspend fun getLastParkingLocationRecord(): Flow<ParkingLocationRecord?> {
        return parkingLocationDatabase.parkingLocationDao().lastParkingLocationRecord
    }
}