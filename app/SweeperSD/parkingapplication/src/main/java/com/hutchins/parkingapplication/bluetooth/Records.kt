package com.hutchins.parkingapplication.bluetooth

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/**
 * Created by joeyhutchins on 3/24/21.
 */
data class PairedBluetoothDevice(val name: String, val address: String)

@Entity(tableName = "pairedBluetoothDeviceRecords")
data class PairedBluetoothDeviceRecord(
        @Embedded val pairedBluetoothDevice: PairedBluetoothDevice,
        val timestamp: Long) {
    // autogenerate false so that this is the only record in the database.
    @PrimaryKey(autoGenerate = false) var recordId: Long = 0L
}

@Entity(tableName = "bluetoothDeviceEventRecords")
data class BluetoothDeviceEventRecord(
        @Embedded val pairedBluetoothDevice: PairedBluetoothDevice,
        val timestamp: Long,
        val eventType: BluetoothDeviceEvent
) {
    @PrimaryKey(autoGenerate = true) var recordId: Long = 0L
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

@Entity(tableName = "bluetoothAdapterRecords")
data class BluetoothAdapterRecord(
        val timestamp: Long,
        val eventType: BluetoothAdapterEvent
) {
    @PrimaryKey(autoGenerate = true) var recordId: Long = 0L
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