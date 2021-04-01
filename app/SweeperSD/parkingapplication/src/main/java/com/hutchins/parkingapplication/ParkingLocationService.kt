package com.hutchins.parkingapplication

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.hutchins.parkingapplication.parkinglocation.ParkingLocationRecord
import com.hutchins.parkingapplication.parkinglocation.ParkingLocationRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Created by joeyhutchins on 3/25/21.
 */
class ParkingLocationService : Service() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e("Joey", "ParkingLocationService onStartCommand ${intent?.action}")

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val locationRequest = LocationRequest.create()
        locationRequest.interval = 15000L

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            stopSelf()
            return START_STICKY
        }
        val cancellationTokenSource = CancellationTokenSource()

        val getCurrentLocationTask = fusedLocationProviderClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
        getCurrentLocationTask.addOnCompleteListener {
            val location: Location? = it.result
            location?.let {
                GlobalScope.launch(Dispatchers.IO) {
                    val record = ParkingLocationRecord.fromLocation(it)
                    ParkingLocationRepo.addParkingLocationRecord(record)
                    Log.e("Joey", "Location is ${record} ")
                    stopSelf()
                }
            } ?: kotlin.run {
                Log.e("Joey", "Failed to get location??")
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.e("Joey", "ParkingLocationService onDestroy")
        super.onDestroy()
    }
}