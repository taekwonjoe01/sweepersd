package com.hutchins.parkingapplication

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.hutchins.parkingapplication.debugui.DebugMainScreen
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

    override fun onCreate() {
        super.onCreate()
        start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        start()
        getParkingLocation()

        return START_STICKY
    }

    @Suppress("SpellCheckingInspection")
    private fun start() {
        val pendingIntent: PendingIntent =
                Intent(this, DebugMainScreen::class.java).let { notificationIntent ->
                    PendingIntent.getActivity(this, 1, notificationIntent, 0)
                }
        val channelId =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createNotificationChannel("Drive Monitoring", "Drive Monitoring")
                } else {
                    // If earlier version channel ID is not used
                    // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                    "Setting Parking Location"
                }
        val notification: Notification = Notification.Builder(this, channelId)
                .setContentTitle("Setting Parking Location")
                .setContentText("You parked - getting and saving your parking location.")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .setTicker("I don't know what TicketText is.")
                .build()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, notification)
        }
    }

    @SuppressLint("MissingPermission") // Because it is being checked, the linter just can't see it!
    private fun getParkingLocation() {
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
    }

    //@RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val chan = NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_LOW)
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val TAG = "ParkingLocationService"
    }
}