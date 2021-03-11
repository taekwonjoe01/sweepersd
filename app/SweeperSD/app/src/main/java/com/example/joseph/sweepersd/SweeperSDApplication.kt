package com.example.joseph.sweepersd

import android.app.Application
import com.example.joseph.sweepersd.bluetooth.BluetoothRecordRepo

class SweeperSDApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        BluetoothRecordRepo.applicationContext = this
    }
}