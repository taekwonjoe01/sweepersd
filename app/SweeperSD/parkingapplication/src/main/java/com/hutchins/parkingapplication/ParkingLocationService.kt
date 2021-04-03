package com.hutchins.parkingapplication

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.util.Log
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.hutchins.parkingapplication.parkinglocation.ParkingLocationRecord
import com.hutchins.parkingapplication.parkinglocation.ParkingLocationRepo
import com.hutchins.parkingapplication.permissions.LocationPermissionHelper
import com.hutchins.parkingapplication.permissions.LocationPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Created by joeyhutchins on 3/25/21.
 */
class ParkingLocationService : Service() {
    @Suppress("SpellCheckingInspection")
    @SuppressLint("MissingPermission") // Because it is being checked, the linter just can't see it!
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (LocationPermissionHelper(this).getLocationPermissionState() == LocationPermissionState.GRANTED) {
            Log.i(TAG, "ParkingLocationService permission is granted. Starting location request...")
            val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
            val cancellationTokenSource = CancellationTokenSource()

            val getCurrentLocationTask = fusedLocationProviderClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
            getCurrentLocationTask.addOnCompleteListener {
                val location: Location? = it.result
                location?.let {
                    GlobalScope.launch(Dispatchers.IO) {
                        val record = ParkingLocationRecord.fromLocation(it)
                        ParkingLocationRepo.addParkingLocationRecord(record)
                        Log.i(TAG, "Location is $record")
                        stopSelf()
                    }
                } ?: kotlin.run {
                    Log.w(TAG, "Failed to get location??")
                    stopSelf()
                }
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val TAG = "ParkingLocationService"
    }
}