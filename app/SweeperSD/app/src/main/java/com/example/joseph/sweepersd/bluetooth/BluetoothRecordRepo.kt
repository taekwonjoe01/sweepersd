package com.example.joseph.sweepersd.bluetooth

import android.content.Context
import android.util.Log
import androidx.room.Room

/**
 * Created by joeyhutchins on 3/10/21.
 */
object BluetoothRecordRepo {
    lateinit var applicationContext: Context

    private val bluetoothRecordDatabase: BluetoothRecordDatabase by lazy {
        Log.e("Joey", "Creating database")
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

    suspend fun getBluetoothDeviceRecords(): List<BluetoothDeviceEventRecord> {
        return bluetoothRecordDatabase.bluetoothDeviceRecordDao().bluetoothDeviceRecords
    }

    suspend fun addBluetoothDeviceRecord(bluetoothDeviceRecord: BluetoothDeviceEventRecord): Long {
        return bluetoothRecordDatabase.bluetoothDeviceRecordDao().insertBluetoothDeviceRecord(bluetoothDeviceRecord)
    }

    suspend fun getSelectedPairedBluetoothDevice(): PairedBluetoothDevice? {
        return bluetoothRecordDatabase.selectedBluetoothDeviceRecordDao().selectedPairedBluetoothDeviceRecord?.pairedBluetoothDevice
    }

    suspend fun setSelectedPairedBluetoothDevice(pairedBluetoothDevice: PairedBluetoothDevice): Long {
        return bluetoothRecordDatabase.selectedBluetoothDeviceRecordDao().setSelectedPairedBluetoothDevice(PairedBluetoothDeviceRecord(pairedBluetoothDevice, System.currentTimeMillis()))
    }
}