package com.example.joseph.sweepersd.bluetooth

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Created by joeyhutchins on 3/10/21.
 */
@Dao
interface BluetoothDeviceRecordDao {
    @get:Query("SELECT * FROM bluetoothDeviceEventRecords")
    val bluetoothDeviceRecords: List<BluetoothDeviceEventRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBluetoothDeviceRecord(bluetoothAdapterRecord: BluetoothDeviceEventRecord): Long
}
