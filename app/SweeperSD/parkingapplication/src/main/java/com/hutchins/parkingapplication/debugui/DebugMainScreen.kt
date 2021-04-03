package com.hutchins.parkingapplication.debugui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.hutchins.navui.jetpack.JetpackScreenFragment
import com.hutchins.parkingapplication.databinding.DebugMainScreenBinding
import com.hutchins.parkingapplication.parkinglocation.ParkingLocationRecord
import com.hutchins.parkingapplication.permissions.LocationPermissionHelper
import com.hutchins.parkingapplication.permissions.LocationPermissionState
import java.text.DateFormat
import java.util.*

/**
 * Created by joeyhutchins on 3/10/21.
 */
class DebugMainScreen : JetpackScreenFragment() {

    private val viewModel: DebugMainScreenViewModel by viewModels()
    private lateinit var binding: DebugMainScreenBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DebugMainScreenBinding.inflate(inflater, container, false).apply {
            changeBluetoothDeviceButton.setOnClickListener {
                findNavController().navigate(DebugMainScreenDirections.actionDebugMainScreenToDebugSelectDeviceScreen())
            }
            seeParkingLocationDetailsButton.setOnClickListener {
                findNavController().navigate(DebugMainScreenDirections.actionDebugMainScreenToDebugParkingLocationDetailsScreen(parkingLocationRecordId = viewModel.lastParkingLocationLiveData.value!!.recordId))
            }
            seeParkingLocationsListButton.setOnClickListener {
                findNavController().navigate(DebugMainScreenDirections.actionDebugMainScreenToDebugParkingLocationListScreen())
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.checkBluetoothPermissionsDelegate = LocationPermissionHelper(requireContext())
        viewModel.permissionStateLiveData.observe(viewLifecycleOwner, {
            binding.permissionStateTextView.text = when (it) {
                LocationPermissionState.GRANTED -> "Granted"
                LocationPermissionState.FOREGROUND_ONLY -> "Foreground Only"
                LocationPermissionState.DENIED -> "Denied"
                null -> ""
            }
        })
        viewModel.selectedDeviceLiveData.observe(viewLifecycleOwner, {
            binding.bluetoothDeviceTextView.text = it
        })
        viewModel.lastParkingLocationLiveData.observe(viewLifecycleOwner, { parkingLocationRecord: ParkingLocationRecord? ->
            binding.lastParkingLocationTimeTextView.text = parkingLocationRecord?.let {
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(it.timestamp))
            } ?: "Never"

            binding.seeParkingLocationDetailsButton.visibility = if (parkingLocationRecord == null) View.INVISIBLE else View.VISIBLE
        })
        viewModel.numParkingLocationsLiveData.observe(viewLifecycleOwner, {
            binding.numParkingLocationsTextView.text = it.toString()
        })
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStartFragment()
    }
}