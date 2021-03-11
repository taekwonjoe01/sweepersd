package com.example.joseph.sweepersd.bluetooth

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
/**
 * Created by joeyhutchins on 3/10/21.
 */
@Dao
interface BluetoothAdapterRecordDao {
    @get:Query("SELECT * FROM bluetoothAdapterRecords")
    val bluetoothAdapterRecords: List<BluetoothAdapterRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBluetoothAdapterRecord(bluetoothAdapterRecord: BluetoothAdapterRecord): Long
}
