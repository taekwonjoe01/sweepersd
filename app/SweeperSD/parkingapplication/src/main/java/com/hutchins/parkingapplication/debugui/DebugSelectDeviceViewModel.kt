package com.hutchins.parkingapplication.debugui

import android.bluetooth.BluetoothAdapter
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hutchins.parkingapplication.bluetooth.BluetoothRecordRepo
import com.hutchins.parkingapplication.bluetooth.PairedBluetoothDevice

/**
 * Created by joeyhutchins on 3/24/21.
 */
class DebugSelectDeviceViewModel : ViewModel() {

    val availableDevicesLiveData = MutableLiveData<List<PairedBluetoothDevice>>()

    init {
        val bondedDevices = BluetoothAdapter.getDefaultAdapter().bondedDevices

        Log.d(TAG, "number of bonded devices: ${BluetoothAdapter.getDefaultAdapter().bondedDevices.size}")
        for (bondedDevice in bondedDevices) {
            Log.d(TAG, "${bondedDevice.address} ${bondedDevice.name} ${bondedDevice.bondState}")
        }
        val pairedBluetoothDevices = bondedDevices.map {
            PairedBluetoothDevice(name = it.name, address = it.address)
        }

        availableDevicesLiveData.value = pairedBluetoothDevices
    }

    /**
     * return true to stop service, false to keep service alive.
     */
    suspend fun onDeviceSelected(pairedBluetoothDevice: PairedBluetoothDevice) {
        BluetoothRecordRepo.setSelectedPairedBluetoothDevice(pairedBluetoothDevice)
    }

    companion object {
        const val TAG = "DebugSelectDeviceViewModel"
    }
}