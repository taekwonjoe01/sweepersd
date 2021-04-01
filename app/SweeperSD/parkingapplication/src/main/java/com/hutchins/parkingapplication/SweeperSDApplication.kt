package com.hutchins.parkingapplication

import android.app.Application
import com.hutchins.parkingapplication.bluetooth.BluetoothRecordRepo
import com.hutchins.parkingapplication.parkinglocation.ParkingLocationRepo

@Suppress("unused")
class SweeperSDApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        BluetoothRecordRepo.applicationContext = this
        ParkingLocationRepo.applicationContext = this
    }
}