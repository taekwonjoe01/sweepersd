package com.hutchins.parkingapplication.parkinglocation

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Created by joeyhutchins on 3/27/21.
 */
@Database(entities = [ParkingLocationRecord::class], version = 1, exportSchema = false)
abstract class ParkingLocationDatabase: RoomDatabase() {
    abstract fun parkingLocationDao(): ParkingLocationDao
}

