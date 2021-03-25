package com.example.joseph.sweepersd

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.joseph.sweepersd.bluetooth.BluetoothRecordRepo
import com.example.joseph.sweepersd.bluetooth.PairedBluetoothDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Created by joeyhutchins on 3/24/21.
 */
class DebugSelectDeviceViewModel : ViewModel() {

    val availableDevicesLiveData = MutableLiveData<List<PairedBluetoothDevice>>()

    init {
        val bondedDevices = BluetoothAdapter.getDefaultAdapter().bondedDevices

        Log.e("Joey", "number of bonded devices: ${BluetoothAdapter.getDefaultAdapter().bondedDevices.size}")

        for (bondedDevice in bondedDevices) {
            Log.e("Joey", "${bondedDevice.address} ${bondedDevice.name} ${bondedDevice.bondState}")
        }

        GlobalScope.launch {
            Log.e("Joey", "number of records: ${BluetoothRecordRepo.getBluetoothAdapterRecords().size}")
        }

        val pairedBluetoothDevices = bondedDevices.map {
            PairedBluetoothDevice(name = it.name, address = it.address)
        }

        Log.e("Joey", "number of devices: ${pairedBluetoothDevices.size}")

        availableDevicesLiveData.value = pairedBluetoothDevices
    }

    /**
     * return true to stop service, false to keep service alive.
     */
    suspend fun onDeviceSelected(pairedBluetoothDevice: PairedBluetoothDevice) {
        BluetoothRecordRepo.setSelectedPairedBluetoothDevice(pairedBluetoothDevice)
    }
}