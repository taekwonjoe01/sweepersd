package com.hutchins.parkingapplication

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices

/**
 * Created by joeyhutchins on 3/25/21.
 */
class DrivingService : Service() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate() {
        super.onCreate()
        start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e("Joey", "onStartCommand ${intent?.action}")
        val stop = intent?.action == "Stop Service"
        if (stop) {
            Log.e("Joey", "stopping service")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else {
            Log.e("Joey", "starting foreground")
            start()
        }

        return START_STICKY
    }

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
                    "Drive Monitoring"
                }
        val notification: Notification = Notification.Builder(this, channelId)
                .setContentTitle("Tracking Your Drive")
                .setContentText("You are Driving - Tracking your location.")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .setTicker("I don't know what TicketText is.")
                .build()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.e("Joey", "Calling startForeground")
            startForeground(1, notification, FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {

            Log.e("Joey", "Calling startForeground")
            startForeground(1, notification)
        }

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
            return
        }
//        fusedLocationProviderClient.locationAvailability. {
//            if (it.isLocationAvailable) {
//
//            } else {
//
//            }
//        }
//        fusedLocationProviderClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
//            override fun onLocationAvailability(p0: LocationAvailability) {
//                super.onLocationAvailability(p0)
//            }
//
//            override fun onLocationResult(p0: LocationResult) {
//                super.onLocationResult(p0)
//            }
//        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val chan = NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_LOW)
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
}