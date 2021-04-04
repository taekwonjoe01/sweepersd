package com.hutchins.parkingapplication.debugui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import com.hutchins.parkingapplication.parkinglocation.ParkingLocationRecord
import com.hutchins.parkingapplication.parkinglocation.ParkingLocationRepo
import kotlinx.coroutines.flow.collect

/**
 * Created by joeyhutchins on 4/4/21.
 */
class DebugMapScreenViewModel(private val recordIds: List<Long>): ViewModel() {
    val parkingLocationRecordsLiveData: LiveData<List<ParkingLocationRecord>> = liveData {
        ParkingLocationRepo.getParkingLocationRecords(recordIds).collect {
            emit(it)
        }
    }
}

class DebugMapScreenViewModelFactory(private val recordIds: List<Long>) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DebugMapScreenViewModel(recordIds) as T
    }
}