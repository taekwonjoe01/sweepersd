package com.example.joseph.sweepersd.bluetooth

import android.content.Context
import androidx.room.Room

/**
 * Created by joeyhutchins on 3/10/21.
 */
object BluetoothRecordRepo {
    lateinit var applicationContext: Context

    private val bluetoothRecordDatabase: BluetoothRecordDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            BluetoothRecordDatabase::class.java, "bluetoothRecordDatabase"
        ).build()
    }

    suspend fun getBluetoothAdapterRecords(): List<BluetoothAdapterRecord> {
        return bluetoothRecordDatabase.bluetoothAdapterRecordDao().bluetoothAdapterRecords
    }

    suspend fun addBluetoothAdapterRecord(bluetoothAdapterRecord: BluetoothAdapterRecord): Long {
        return bluetoothRecordDatabase.bluetoothAdapterRecordDao().insertBluetoothAdapterRecord(bluetoothAdapterRecord)
    }

    suspend fun getBluetoothDeviceRecords(): List<BluetoothDeviceRecord> {
        return bluetoothRecordDatabase.bluetoothDeviceRecordDao().bluetoothDeviceRecords
    }

    suspend fun addBluetoothDeviceRecord(bluetoothDeviceRecord: BluetoothDeviceRecord): Long {
        return bluetoothRecordDatabase.bluetoothDeviceRecordDao().insertBluetoothDeviceRecord(bluetoothDeviceRecord)
    }
}