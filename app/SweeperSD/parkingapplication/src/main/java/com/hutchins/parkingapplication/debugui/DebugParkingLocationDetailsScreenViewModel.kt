package com.hutchins.parkingapplication.debugui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import com.hutchins.parkingapplication.parkinglocation.ParkingLocationRecord
import com.hutchins.parkingapplication.parkinglocation.ParkingLocationRepo
import kotlinx.coroutines.flow.collect

/**
 * Created by joeyhutchins on 3/24/21.
 */
class DebugParkingLocationDetailsScreenViewModel(private val recordId: Long) : ViewModel() {
    val parkingLocationRecordLiveData = liveData {
        ParkingLocationRepo.getParkingLocationRecord(recordId).collect { maybeRecord: ParkingLocationRecord? ->
            maybeRecord?.let {
                emit(it)
            } ?: kotlin.run {
                throw IllegalStateException("Trying to show details of record that does not exist!")
            }
        }
    }
}

class DebugParkingLocationDetailsScreenViewModelFactory(private val recordId: Long) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DebugParkingLocationDetailsScreenViewModel(recordId) as T
    }
}

