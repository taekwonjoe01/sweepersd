package com.example.joseph.sweepersd.bluetooth

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/**
 * Created by joeyhutchins on 3/10/21.
 */
@Entity(tableName = "bluetoothDeviceRecords")
data class BluetoothDeviceRecord(
        val timestamp: Long,
        val deviceAddress: String,
        val deviceName: String,
        val eventType: BluetoothDeviceEvent
) {
    @PrimaryKey var recordId: Long = 0L
}

enum class BluetoothDeviceEvent {
    CONNECTED,
    DISCONNECTED
}

class BluetoothDeviceRecordConverter {
    @TypeConverter
    fun toBluetoothDeviceEvent(value: Int): BluetoothDeviceEvent {
        return when(value) {
            0 -> BluetoothDeviceEvent.CONNECTED
            1 -> BluetoothDeviceEvent.DISCONNECTED
            else -> throw IllegalStateException("Invalid enum mapping for BluetoothAdapterEvent $value.")
        }
    }

    @TypeConverter
    fun fromBluetoothDeviceEvent(bluetoothAdapterEvent: BluetoothDeviceEvent): Int {
        return when(bluetoothAdapterEvent) {
            BluetoothDeviceEvent.CONNECTED -> 0
            BluetoothDeviceEvent.DISCONNECTED -> 1
        }
    }
}