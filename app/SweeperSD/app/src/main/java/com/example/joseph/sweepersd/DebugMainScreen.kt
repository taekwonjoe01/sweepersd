package com.example.joseph.sweepersd

import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.joseph.sweepersd.databinding.ActivityDebugBinding
import com.hutchins.navui.jetpack.JetpackScreenFragment

/**
 * Created by joeyhutchins on 3/10/21.
 */
class DebugMainScreen : JetpackScreenFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = ActivityDebugBinding.inflate(inflater, container, false).apply {

        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val bondedDevices = BluetoothAdapter.getDefaultAdapter().bondedDevices
        Log.e("Joey", "${BluetoothAdapter.getDefaultAdapter().bondedDevices.size}")

        for (bondedDevice in bondedDevices) {
            Log.e("Joey", "${bondedDevice.address} ${bondedDevice.name} ${bondedDevice.bondState}")
        }

        super.onViewCreated(view, savedInstanceState)
    }
}