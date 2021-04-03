package com.hutchins.parkingapplication

import android.bluetooth.BluetoothAdapter
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hutchins.parkingapplication.bluetooth.BluetoothRecordRepo
import com.hutchins.parkingapplication.bluetooth.PairedBluetoothDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
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

        GlobalScope.launch(Dispatchers.IO) {
            Log.e("Joey", "number of records: ${BluetoothRecordRepo.getBluetoothAdapterRecords().first().size}")
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