package com.hutchins.parkingapplication.parkinglocation

import android.content.Context
import android.util.Log
import androidx.room.Room

/**
 * Created by joeyhutchins on 3/27/21.
 */
object ParkingLocationRepo {
    lateinit var applicationContext: Context

    private val parkingLocationDatabase: ParkingLocationDatabase by lazy {
        Log.e("Joey", "Creating database")
        Room.databaseBuilder(
                applicationContext,
                ParkingLocationDatabase::class.java, "parkingLocationDatabase"
        ).build()
    }

    suspend fun getParkingLocationRecords(): List<ParkingLocationRecord> {
        return parkingLocationDatabase.parkingLocationDao().parkingLocationRecords
    }

    suspend fun addParkingLocationRecord(parkingLocationRecord: ParkingLocationRecord): Long {
        return parkingLocationDatabase.parkingLocationDao().addParkingLocationRecord(parkingLocationRecord)
    }

    suspend fun getLastParkingLocationRecord(): ParkingLocationRecord? {
        return parkingLocationDatabase.parkingLocationDao().lastParkingLocationRecord
    }
}