package com.example.joseph.sweepersd

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Created by joeyhutchins on 3/10/21.
 */
class BluetoothBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.e("Joey", "onReceive")
    }
}