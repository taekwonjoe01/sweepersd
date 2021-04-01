package com.hutchins.parkingapplication.parkinglocation

import android.location.Location
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Created by joeyhutchins on 3/27/21.
 */
@Entity(tableName = "parkingLocationRecords")
data class ParkingLocationRecord(
        val timestamp: Long,
        val latitude: Double,
        val longitude: Double,
        val latLonAccuracy: Float,
        val altitude: Double,
        val altitudeAccuracy: Float,
        val bearing: Float,
        val bearingAccuracy: Float,
        val time: Long,
        val speed: Float,
        val speedAccuracy: Float
) {
    @PrimaryKey(autoGenerate = true) var recordId: Long = 0L

    companion object {
        fun fromLocation(location: Location): ParkingLocationRecord {
            return ParkingLocationRecord(
                    timestamp = System.currentTimeMillis(),
                    latitude = location.latitude,
                    longitude = location.longitude,
                    latLonAccuracy = location.accuracy,
                    altitude = location.altitude,
                    altitudeAccuracy = location.verticalAccuracyMeters,
                    bearing = location.bearing,
                    bearingAccuracy = location.bearingAccuracyDegrees,
                    speed = location.speed,
                    speedAccuracy = location.speedAccuracyMetersPerSecond,
                    time = location.time,
            )
        }
    }
}

