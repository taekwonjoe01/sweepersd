package com.example.joseph.sweepersd

import android.app.Application
import com.example.joseph.sweepersd.bluetooth.BluetoothRecordRepo

@Suppress("unused")
class SweeperSDApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        BluetoothRecordRepo.applicationContext = this
    }
}