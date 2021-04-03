package com.hutchins.parkingapplication.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

/**
 * Created by joeyhutchins on 4/2/21.
 */
class LocationPermissionHelper(private val context: Context) {
    fun getLocationPermissionState(): LocationPermissionState {
        val hasFineLocationPermission = ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        return if (!hasFineLocationPermission) {
            LocationPermissionState.DENIED
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && ActivityCompat.checkSelfPermission(context,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // This is the state where we "Allow All the Time" is not checked.
                LocationPermissionState.FOREGROUND_ONLY
            } else {
                LocationPermissionState.GRANTED
            }
        }
    }
}