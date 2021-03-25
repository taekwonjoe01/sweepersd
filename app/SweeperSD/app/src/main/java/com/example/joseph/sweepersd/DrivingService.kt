package com.example.joseph.sweepersd

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * Created by joeyhutchins on 3/25/21.
 */
class DrivingService : Service() {

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
                .setSmallIcon(R.drawable.alert_sweeping_app_icon)
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