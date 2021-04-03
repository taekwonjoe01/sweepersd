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
import com.hutchins.parkingapplication.databinding.DebugParkingLocationListScreenBinding
import com.hutchins.parkingapplication.parkinglocation.ParkingLocationRecord
import java.text.DateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by joeyhutchins on 4/3/21.
 */
class DebugParkingLocationListScreen: JetpackScreenFragment() {

    private val adapter = ParkingLocationAdapter(object : ParkingLocationClickListener {
        override fun onParkingLocationSelected(parkingLocationRecord: ParkingLocationRecord) {
            findNavController().navigate(DebugParkingLocationListScreenDirections.actionDebugParkingLocationListScreenToDebugParkingLocationDetailsScreen(parkingLocationRecordId = parkingLocationRecord.recordId))
        }
    })
    private lateinit var binding: DebugParkingLocationListScreenBinding
    private val viewModel: DebugParkingLocationListScreenViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DebugParkingLocationListScreenBinding.inflate(inflater, container, false).apply {
            parkingLocationRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            parkingLocationRecyclerView.adapter = adapter
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.parkingLocationsLiveData.observe(viewLifecycleOwner, {
            adapter.parkingLocations = it
            adapter.notifyDataSetChanged()
        })
    }
}

class ParkingLocationAdapter(private val itemClickListener: ParkingLocationClickListener): RecyclerView.Adapter<ParkingLocationViewHolder>() {

    var parkingLocations: List<ParkingLocationRecord> = ArrayList()

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ParkingLocationViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.layout_parking_location_list_item, viewGroup, false)

        return ParkingLocationViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ParkingLocationViewHolder, position: Int) {
        viewHolder.bind(parkingLocations[position], itemClickListener)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return parkingLocations.size
    }

}

class ParkingLocationViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
    private val parkingLocationDateTextView: AppCompatTextView = view.findViewById(R.id.parkingLocationDateTextView)

    fun bind(parkingLocation: ParkingLocationRecord, clickListener: ParkingLocationClickListener) {
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        parkingLocationDateTextView.text = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(parkingLocation.timestamp))

        this.view.setOnClickListener {
            clickListener.onParkingLocationSelected(parkingLocation)
        }
    }
}

interface ParkingLocationClickListener {
    fun onParkingLocationSelected(parkingLocationRecord: ParkingLocationRecord)
}