package com.hutchins.parkingapplication.bluetooth

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Created by joeyhutchins on 3/10/21.
 */
@Dao
interface BluetoothDeviceRecordDao {
    @get:Query("SELECT * FROM bluetoothDeviceEventRecords")
    val bluetoothDeviceRecords: Flow<List<BluetoothDeviceEventRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBluetoothDeviceRecord(bluetoothAdapterRecord: BluetoothDeviceEventRecord): Long
}
