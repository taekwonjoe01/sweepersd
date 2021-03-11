package com.example.joseph.sweepersd.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Created by joeyhutchins on 3/10/21.
 */
class BluetoothBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.e("Joey", "onReceive")
        when (intent.action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val bluetoothAdapterEvent = when (val adapterState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                    BluetoothAdapter.STATE_ON -> BluetoothAdapterEvent.ON
                    BluetoothAdapter.STATE_OFF -> BluetoothAdapterEvent.OFF
                    BluetoothAdapter.STATE_TURNING_ON -> BluetoothAdapterEvent.TURNING_ON
                    BluetoothAdapter.STATE_TURNING_OFF -> BluetoothAdapterEvent.TURNING_OFF
                    else -> throw IllegalStateException("Unexpected bluetooth adapter event: ${adapterState}.")
                }

                val record = BluetoothAdapterRecord(System.currentTimeMillis(), bluetoothAdapterEvent)

                runBlocking(Dispatchers.IO) {
                    BluetoothRecordRepo.addBluetoothAdapterRecord(record)
                }
            }
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val bluetoothDevice: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                val record = BluetoothDeviceRecord(System.currentTimeMillis(), bluetoothDevice.address, bluetoothDevice.name, BluetoothDeviceEvent.CONNECTED)
                runBlocking(Dispatchers.IO) {
                    BluetoothRecordRepo.addBluetoothDeviceRecord(record)
                }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val bluetoothDevice: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                val record = BluetoothDeviceRecord(System.currentTimeMillis(), bluetoothDevice.address, bluetoothDevice.name, BluetoothDeviceEvent.DISCONNECTED)
                runBlocking(Dispatchers.IO) {
                    BluetoothRecordRepo.addBluetoothDeviceRecord(record)
                }
            }
            else -> {
                // TODO: Analytics logging.
                Log.e(TAG, "Unexpected bluetooth event action type: ${intent.action}.")
            }
        }
    }
    companion object {
        @Suppress("SpellCheckingInspection")
        const val TAG = "BluetoothBroadcastRecei"
    }
}