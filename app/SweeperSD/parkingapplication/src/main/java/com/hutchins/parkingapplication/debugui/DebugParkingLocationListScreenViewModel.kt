package com.hutchins.parkingapplication.debugui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.hutchins.parkingapplication.parkinglocation.ParkingLocationRepo
import kotlinx.coroutines.flow.collect

/**
 * Created by joeyhutchins on 4/3/21.
 */
class DebugParkingLocationListScreenViewModel: ViewModel() {
    val parkingLocationsLiveData = liveData {
        ParkingLocationRepo.getParkingLocationRecords().collect {
            emit(it)
        }
    }
}