package com.example.joseph.sweepersd

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.joseph.sweepersd.bluetooth.PairedBluetoothDevice
import com.example.joseph.sweepersd.databinding.DebugMainScreenBinding
import com.example.joseph.sweepersd.databinding.DebugSelectDeviceScreenBinding
import com.hutchins.navui.jetpack.JetpackScreenFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Created by joeyhutchins on 3/24/21.
 */
class DebugSelectDeviceScreen : JetpackScreenFragment() {

    private val adapter = BluetoothDeviceAdapter(object : OnPairedBluetoothDeviceSelectedListener {
        override fun onPairedBluetoothDeviceSelected(pairedBluetoothDevice: PairedBluetoothDevice) {
            runBlocking(Dispatchers.IO) { viewModel.onDeviceSelected(pairedBluetoothDevice) }

            // TODO: Only stop the service if we know the newly selected device is not bonded.
            //  Can't know this on application startup.
            Log.e("Joey", "calling startService to actually stop the service.")
            requireContext().startForegroundService(Intent(context, DrivingService::class.java).apply { setAction("Stop Service") })
            findNavController().popBackStack()
        }
    })
    private lateinit var binding: DebugSelectDeviceScreenBinding
    private val viewModel: DebugSelectDeviceViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DebugSelectDeviceScreenBinding.inflate(inflater, container, false).apply {
            deviceRecycleView.layoutManager = LinearLayoutManager(requireContext())
            deviceRecycleView.adapter = adapter
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.availableDevicesLiveData.observe(viewLifecycleOwner, {
            adapter.devices = it
            adapter.notifyDataSetChanged()

            Log.e("Joey", it.toString())
        })
    }
}

class BluetoothDeviceAdapter(val itemClickListener: OnPairedBluetoothDeviceSelectedListener): RecyclerView.Adapter<MyHolder>() {

    var devices: List<PairedBluetoothDevice> = ArrayList()

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): MyHolder {
        Log.e("Joey", "onCreateViewHolder")
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.layout_select_device_list_item, viewGroup, false)

        return MyHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: MyHolder, position: Int) {
        viewHolder.bind(devices[position], itemClickListener)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        Log.e("Joey", "devices size is ${devices.size}")
        return devices.size
    }

}

class MyHolder(private val view: View) : RecyclerView.ViewHolder(view) {
    val deviceNameTextView: AppCompatTextView = view.findViewById(R.id.deviceNameTextView)

    fun bind(device: PairedBluetoothDevice, clickListener: OnPairedBluetoothDeviceSelectedListener) {
        Log.e("Joey", "bind")
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        deviceNameTextView.text = device.name

        this.view.setOnClickListener {
            clickListener.onPairedBluetoothDeviceSelected(device)
        }
    }
}

interface OnPairedBluetoothDeviceSelectedListener {
    fun onPairedBluetoothDeviceSelected(pairedBluetoothDevice: PairedBluetoothDevice)
}
