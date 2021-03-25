package com.example.joseph.sweepersd

import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.joseph.sweepersd.bluetooth.BluetoothRecordRepo
import com.example.joseph.sweepersd.databinding.DebugMainScreenBinding
import com.example.joseph.sweepersd.databinding.DebugSelectDeviceScreenBinding
import com.hutchins.navui.jetpack.JetpackScreenFragment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Created by joeyhutchins on 3/10/21.
 */
class DebugMainScreen : JetpackScreenFragment() {

    val viewModel: DebugMainScreenViewModel by viewModels()
    lateinit var binding: DebugMainScreenBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DebugMainScreenBinding.inflate(inflater, container, false).apply {
            changeBluetoothDeviceButton.setOnClickListener {
                findNavController().navigate(DebugMainScreenDirections.actionDebugMainScreenToDebugSelectDeviceScreen())
            }


        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.selectedDeviceLiveData.observe(viewLifecycleOwner, {
            binding.bluetoothDeviceTextView.text = it
        })
        viewModel.loadSelectedDevice()
        super.onViewCreated(view, savedInstanceState)
    }
}