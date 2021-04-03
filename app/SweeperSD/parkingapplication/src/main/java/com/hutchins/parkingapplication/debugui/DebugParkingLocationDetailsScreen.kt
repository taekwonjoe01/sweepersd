package com.hutchins.parkingapplication.debugui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.hutchins.navui.jetpack.JetpackScreenFragment
import com.hutchins.parkingapplication.databinding.DebugParkingLocationDetailsScreenBinding
import com.hutchins.parkingapplication.parkinglocation.ParkingLocationRecord
import java.text.DateFormat
import java.util.*

/**
 * Created by joeyhutchins on 3/10/21.
 */
class DebugParkingLocationDetailsScreen : JetpackScreenFragment() {

    //private lateinit var viewModelDetails: DebugParkingLocationDetailsScreenViewModel = ViewModelProvider(this, DebugParkingLocationDetailsScreenViewModelFactory())
    private val navArgs : DebugParkingLocationDetailsScreenArgs by navArgs()
    private lateinit var viewModel: DebugParkingLocationDetailsScreenViewModel
    private lateinit var binding: DebugParkingLocationDetailsScreenBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DebugParkingLocationDetailsScreenBinding.inflate(inflater, container, false).apply {

        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this, DebugParkingLocationDetailsScreenViewModelFactory(navArgs.parkingLocationRecordId)).get(DebugParkingLocationDetailsScreenViewModel::class.java)
        viewModel.parkingLocationRecordLiveData.observe(viewLifecycleOwner, { parkingLocationRecord: ParkingLocationRecord ->
            binding.parkingLocationTimeTextView.text = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(parkingLocationRecord.time))
            binding.parkingLocationLatitudeTextView.text = parkingLocationRecord.latitude.toString()
            binding.parkingLocationLongitudeTextView.text = parkingLocationRecord.longitude.toString()
            binding.parkingLocationLatLongAccuracyTextView.text = parkingLocationRecord.latLonAccuracy.toString()
            binding.parkingLocationAltitudeTextView.text = parkingLocationRecord.altitude.toString()
            binding.parkingLocationAltitudeAccuracyTextView.text = parkingLocationRecord.altitudeAccuracy.toString()
            binding.parkingLocationBearingTextView.text = parkingLocationRecord.bearing.toString()
            binding.parkingLocationBearingAccuracyTextView.text = parkingLocationRecord.bearingAccuracy.toString()
            binding.parkingLocationSpeedTextView.text = parkingLocationRecord.speed.toString()
            binding.parkingLocationSpeedAccuracyTextView.text = parkingLocationRecord.speedAccuracy.toString()
            jetpackNavUIController.setToolbarSubtitle(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(parkingLocationRecord.timestamp)))
        })
    }
}