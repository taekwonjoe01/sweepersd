package com.hutchins.parkingapplication.debugui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hutchins.navui.jetpack.JetpackScreenFragment
import com.hutchins.parkingapplication.R
import com.hutchins.parkingapplication.bluetooth.PairedBluetoothDevice
import com.hutchins.parkingapplication.databinding.DebugSelectDeviceScreenBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Created by joeyhutchins on 3/24/21.
 */
class DebugSelectDeviceScreen : JetpackScreenFragment() {

    private val adapter = BluetoothDeviceAdapter(object : OnPairedBluetoothDeviceSelectedListener {
        override fun onPairedBluetoothDeviceSelected(pairedBluetoothDevice: PairedBluetoothDevice) {
            runBlocking(Dispatchers.IO) { viewModel.onDeviceSelected(pairedBluetoothDevice) }
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
        })
    }
}

class BluetoothDeviceAdapter(val itemClickListener: OnPairedBluetoothDeviceSelectedListener): RecyclerView.Adapter<MyHolder>() {

    var devices: List<PairedBluetoothDevice> = ArrayList()

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): MyHolder {
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
        return devices.size
    }

}

class MyHolder(private val view: View) : RecyclerView.ViewHolder(view) {
    val deviceNameTextView: AppCompatTextView = view.findViewById(R.id.deviceNameTextView)

    fun bind(device: PairedBluetoothDevice, clickListener: OnPairedBluetoothDeviceSelectedListener) {
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
