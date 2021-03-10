package com.example.joseph.sweepersd

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
class DebugActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)

        val bondedDevices = BluetoothAdapter.getDefaultAdapter().bondedDevices
        Log.e("Joey", "${BluetoothAdapter.getDefaultAdapter().bondedDevices.size}")

        for (bondedDevice in bondedDevices) {
            Log.e("Joey", "${bondedDevice.address} ${bondedDevice.name} ${bondedDevice.bondState}")
        }
    }
}