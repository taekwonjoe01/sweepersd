package com.hutchins.parkingapplication.debugui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.hutchins.navui.jetpack.JetpackScreenFragment
import com.hutchins.parkingapplication.R
import com.hutchins.parkingapplication.databinding.DebugMapScreenBinding
import com.hutchins.parkingapplication.parkinglocation.ParkingLocationRecord
import java.text.DateFormat
import java.util.*


/**
 * Created by joeyhutchins on 4/4/21.
 */
class DebugMapScreen: JetpackScreenFragment(), OnMapReadyCallback {
    private val navArgs : DebugMapScreenArgs by navArgs()
    private lateinit var viewModel: DebugMapScreenViewModel
    private lateinit var binding: DebugMapScreenBinding
    private lateinit var supportMapFragment: SupportMapFragment

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DebugMapScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        this.supportMapFragment = childFragmentManager.findFragmentById(R.id.supportMapFragment) as SupportMapFragment
        supportMapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.google_map_style))
        viewModel = ViewModelProvider(this, DebugMapScreenViewModelFactory(navArgs.parkingLocationRecordIds.toList())).get(DebugMapScreenViewModel::class.java)
        viewModel.parkingLocationRecordsLiveData.observe(viewLifecycleOwner, { parkingLocationRecords: List<ParkingLocationRecord> ->
            googleMap.clear()

            val markers = parkingLocationRecords.map {
                val latLng = LatLng(it.latitude, it.longitude)
                MarkerOptions()
                        .position(latLng)
                        .title(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(it.timestamp)))
            }

            val builder = LatLngBounds.Builder()
            for (marker in markers) {
                builder.include(marker.position)
            }
            val bounds = builder.build()
            for (marker in markers) {
                googleMap.addMarker(marker)
            }
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0))
        })
    }
}