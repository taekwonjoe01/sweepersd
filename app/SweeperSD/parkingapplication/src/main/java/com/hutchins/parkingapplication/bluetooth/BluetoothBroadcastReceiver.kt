package com.hutchins.parkingapplication.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hutchins.parkingapplication.ParkingLocationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Created by joeyhutchins on 3/10/21.
 */
class BluetoothBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
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
                val record = BluetoothDeviceEventRecord(PairedBluetoothDevice(bluetoothDevice.name, bluetoothDevice.address), System.currentTimeMillis(), BluetoothDeviceEvent.CONNECTED)
                runBlocking(Dispatchers.IO) {
                    BluetoothRecordRepo.addBluetoothDeviceRecord(record)
                }
                runBlocking(Dispatchers.IO) {
                    BluetoothRecordRepo.getSelectedPairedBluetoothDevice().first()?.let {
                        if (it.pairedBluetoothDevice.name == bluetoothDevice.name) {
                            //Log.e("Joey", "calling startForegroundService.")
//                            context.startForegroundService(Intent(context, DrivingService::class.java))
                        }
                    }
                }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val bluetoothDevice: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                val record = BluetoothDeviceEventRecord(PairedBluetoothDevice(bluetoothDevice.name, bluetoothDevice.address), System.currentTimeMillis(), BluetoothDeviceEvent.DISCONNECTED)
                runBlocking(Dispatchers.IO) {
                    BluetoothRecordRepo.addBluetoothDeviceRecord(record)
                }
                runBlocking(Dispatchers.IO) {
                    val shouldStopService = BluetoothRecordRepo.getSelectedPairedBluetoothDevice().first()?.pairedBluetoothDevice?.name == bluetoothDevice.name ?: true
                    if (shouldStopService) {
                        // send message to service to stop itself.
                        Log.i(TAG, "Starting ParkingLocationService")
//                        context.startForegroundService(Intent(context, DrivingService::class.java).apply { setAction("Stop Service") })
                        context.startForegroundService(Intent(context, ParkingLocationService::class.java))
                    }
                }
            }
            BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> {
                Log.d(TAG, "ACTION_CONNECTION_STATE_CHANGED ${intent.extras}")
//                val connectionState = when (val state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1)) {
//                    BluetoothAdapter.STATE_CONNECTED -> "Connected"
//                    BluetoothAdapter.STATE_CONNECTING -> "Connecting"
//                    BluetoothAdapter.STATE_DISCONNECTED -> "Disconnected"
//                    BluetoothAdapter.STATE_DISCONNECTING -> "Disconnecting"
//                    else -> throw IllegalStateException("Unexpected bluetooth adapter connection state: ${state}.")
//                }
//                Log.e(TAG, "Connection State Changed ${connectionState}")
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