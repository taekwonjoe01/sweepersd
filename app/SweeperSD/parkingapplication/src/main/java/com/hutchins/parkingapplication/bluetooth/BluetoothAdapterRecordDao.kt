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
interface BluetoothAdapterRecordDao {
    @get:Query("SELECT * FROM bluetoothAdapterRecords")
    val bluetoothAdapterRecords: Flow<List<BluetoothAdapterRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBluetoothAdapterRecord(bluetoothAdapterRecord: BluetoothAdapterRecord): Long
}
