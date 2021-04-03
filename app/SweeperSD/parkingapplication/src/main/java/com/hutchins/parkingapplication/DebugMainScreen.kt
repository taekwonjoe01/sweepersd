package com.hutchins.parkingapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.hutchins.navui.jetpack.JetpackScreenFragment
import com.hutchins.parkingapplication.databinding.DebugMainScreenBinding
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
            seeLastParkingLocationButton.setOnClickListener {
                Toast.makeText(requireContext(), "TODO", Toast.LENGTH_SHORT).show()
            }
            seeParkingLocationsListButton.setOnClickListener {
                Toast.makeText(requireContext(), "TODO", Toast.LENGTH_SHORT).show()
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
        viewModel.lastParkingLocationDateLiveData.observe(viewLifecycleOwner, { timestamp: Long? ->
            binding.lastParkingLocationTimeTextView.text = timestamp?.let {
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(it))
            } ?: "Never"

            binding.seeLastParkingLocationButton.visibility = if (timestamp == null) View.INVISIBLE else View.VISIBLE
        })
        viewModel.numParkingLocationsLiveData.observe(viewLifecycleOwner, {
            binding.numParkingLocationsTextView.text = it.toString()
        })
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStartFragment()
    }
}