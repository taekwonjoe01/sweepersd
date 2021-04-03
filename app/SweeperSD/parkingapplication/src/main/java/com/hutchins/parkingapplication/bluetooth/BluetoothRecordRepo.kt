package com.hutchins.parkingapplication.bluetooth

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow

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

    suspend fun getBluetoothAdapterRecords(): Flow<List<BluetoothAdapterRecord>> {
        return bluetoothRecordDatabase.bluetoothAdapterRecordDao().bluetoothAdapterRecords
    }

    suspend fun addBluetoothAdapterRecord(bluetoothAdapterRecord: BluetoothAdapterRecord): Long {
        return bluetoothRecordDatabase.bluetoothAdapterRecordDao().insertBluetoothAdapterRecord(bluetoothAdapterRecord)
    }

    suspend fun getBluetoothDeviceRecords(): Flow<List<BluetoothDeviceEventRecord>> {
        return bluetoothRecordDatabase.bluetoothDeviceRecordDao().bluetoothDeviceRecords
    }

    suspend fun addBluetoothDeviceRecord(bluetoothDeviceRecord: BluetoothDeviceEventRecord): Long {
        return bluetoothRecordDatabase.bluetoothDeviceRecordDao().insertBluetoothDeviceRecord(bluetoothDeviceRecord)
    }

    suspend fun getSelectedPairedBluetoothDevice(): Flow<PairedBluetoothDeviceRecord?> {
        return bluetoothRecordDatabase.selectedBluetoothDeviceRecordDao().selectedPairedBluetoothDeviceRecord
    }

    suspend fun setSelectedPairedBluetoothDevice(pairedBluetoothDevice: PairedBluetoothDevice): Long {
        return bluetoothRecordDatabase.selectedBluetoothDeviceRecordDao().setSelectedPairedBluetoothDevice(PairedBluetoothDeviceRecord(pairedBluetoothDevice, System.currentTimeMillis()))
    }
}