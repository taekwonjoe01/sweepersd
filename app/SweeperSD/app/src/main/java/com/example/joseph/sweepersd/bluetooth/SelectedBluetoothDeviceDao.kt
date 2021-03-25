package com.example.joseph.sweepersd.bluetooth

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Created by joeyhutchins on 3/10/21.
 */
@Dao
interface SelectedBluetoothDeviceDao {
    @get:Query("SELECT * FROM pairedBluetoothDeviceRecords LIMIT 1")
    val selectedPairedBluetoothDeviceRecord: PairedBluetoothDeviceRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setSelectedPairedBluetoothDevice(pairedBluetoothDeviceRecord: PairedBluetoothDeviceRecord): Long
}
