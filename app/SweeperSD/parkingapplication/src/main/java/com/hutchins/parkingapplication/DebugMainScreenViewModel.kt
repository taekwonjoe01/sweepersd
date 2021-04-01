package com.hutchins.parkingapplication

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hutchins.parkingapplication.bluetooth.BluetoothRecordRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by joeyhutchins on 3/24/21.
 */
class DebugMainScreenViewModel : ViewModel() {

    val selectedDeviceLiveData = MutableLiveData<String>()

    fun loadSelectedDevice() {
        viewModelScope.launch(Dispatchers.IO) {
            selectedDeviceLiveData.postValue(BluetoothRecordRepo.getSelectedPairedBluetoothDevice()?.name ?: "None")
        }
    }
}