package com.example.joseph.sweepersd.bluetooth

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/**
 * Created by joeyhutchins on 3/10/21.
 */
@Entity(tableName = "bluetoothAdapterRecords")
data class BluetoothAdapterRecord(
        val timestamp: Long,
        val eventType: BluetoothAdapterEvent
        ) {
    @PrimaryKey var recordId: Long = 0L
}

enum class BluetoothAdapterEvent {
    TURNING_ON,
    ON,
    TURNING_OFF,
    OFF
}

class BluetoothAdapterRecordConverter {
    @TypeConverter
    fun toBluetoothAdapterEvent(value: Int): BluetoothAdapterEvent {
        return when(value) {
            0 -> BluetoothAdapterEvent.TURNING_ON
            1 -> BluetoothAdapterEvent.ON
            2 -> BluetoothAdapterEvent.TURNING_OFF
            3 -> BluetoothAdapterEvent.OFF
            else -> throw IllegalStateException("Invalid enum mapping for BluetoothAdapterEvent $value.")
        }
    }

    @TypeConverter
    fun fromBluetoothAdapterEvent(bluetoothAdapterEvent: BluetoothAdapterEvent): Int {
        return when(bluetoothAdapterEvent) {
            BluetoothAdapterEvent.TURNING_ON -> 0
            BluetoothAdapterEvent.ON -> 1
            BluetoothAdapterEvent.TURNING_OFF -> 2
            BluetoothAdapterEvent.OFF -> 3
        }
    }
}