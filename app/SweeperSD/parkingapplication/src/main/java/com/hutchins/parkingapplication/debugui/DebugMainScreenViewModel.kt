package com.hutchins.parkingapplication.debugui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.hutchins.parkingapplication.bluetooth.BluetoothRecordRepo
import com.hutchins.parkingapplication.parkinglocation.ParkingLocationRepo
import com.hutchins.parkingapplication.permissions.LocationPermissionHelper
import com.hutchins.parkingapplication.permissions.LocationPermissionState
import kotlinx.coroutines.flow.collect

/**
 * Created by joeyhutchins on 3/24/21.
 */
class DebugMainScreenViewModel : ViewModel() {

    val selectedDeviceLiveData = liveData {
        BluetoothRecordRepo.getSelectedPairedBluetoothDevice().collect {
            emit(it?.pairedBluetoothDevice?.name ?: "")
        }
    }
    val lastParkingLocationDateLiveData = liveData {
        ParkingLocationRepo.getLastParkingLocationRecord().collect {
            emit(it?.timestamp)
        }
    }
    val numParkingLocationsLiveData = liveData {
        ParkingLocationRepo.getParkingLocationRecords().collect {
            emit(it.size)
        }
    }
    val permissionStateLiveData = MutableLiveData<LocationPermissionState?>().apply { value = null }
    lateinit var checkBluetoothPermissionsDelegate: LocationPermissionHelper

    fun onStartFragment() {
        permissionStateLiveData.value = checkBluetoothPermissionsDelegate.getLocationPermissionState()
    }
}

