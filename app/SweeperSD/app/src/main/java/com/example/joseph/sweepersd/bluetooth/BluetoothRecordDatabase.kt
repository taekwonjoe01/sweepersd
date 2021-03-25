package com.example.joseph.sweepersd.bluetooth

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Created by joeyhutchins on 3/10/21.
 */
@Database(entities = [BluetoothAdapterRecord::class, BluetoothDeviceEventRecord::class, PairedBluetoothDeviceRecord::class], version = 1, exportSchema = false)
@TypeConverters(BluetoothAdapterRecordConverter::class, BluetoothDeviceRecordConverter::class)
abstract class BluetoothRecordDatabase : RoomDatabase() {
    abstract fun bluetoothAdapterRecordDao(): BluetoothAdapterRecordDao
    abstract fun bluetoothDeviceRecordDao(): BluetoothDeviceRecordDao
    abstract fun selectedBluetoothDeviceRecordDao(): SelectedBluetoothDeviceDao
}