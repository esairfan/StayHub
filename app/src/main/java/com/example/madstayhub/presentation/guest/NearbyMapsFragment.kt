package com.example.madstayhub.presentation.guest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.madstayhub.R
import com.example.madstayhub.databinding.FragmentNearbyMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.chip.Chip

class NearbyMapsFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentNearbyMapsBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null
    private val hotelLocation = LatLng(33.6844, 73.0479) // Default Islamabad location

    data class Place(val name: String, val location: LatLng, val category: String)

    private val places = listOf(
        Place("Monval Italian Restaurant", LatLng(33.6855, 73.0490), "Restaurants"),
        Place("Tuscany Courtyard", LatLng(33.6830, 73.0460), "Restaurants"),
        Place("Habib Bank ATM", LatLng(33.6840, 73.0485), "ATMs"),
        Place("Standard Chartered ATM", LatLng(33.6860, 73.0470), "ATMs"),
        Place("D.Watson Chemist & Pharmacy", LatLng(33.6845, 73.0495), "Pharmacy"),
        Place("Shaheen Chemist", LatLng(33.6820, 73.0450), "Pharmacy"),
        Place("Shifa International Hospital", LatLng(33.6890, 73.0420), "Hospitals"),
        Place("Kulsum International Hospital", LatLng(33.6810, 73.0510), "Hospitals")
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNearbyMapsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        binding.toolbar.title = "Explore Nearby"

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            val chip = checkedId?.let { view?.findViewById<Chip>(it) }
            val category = chip?.text?.toString() ?: "Restaurants"
            showMarkers(category)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(hotelLocation, 15f))
        googleMap?.addMarker(MarkerOptions().position(hotelLocation).title("StayHub Hotel"))

        showMarkers("Restaurants")
    }

    private fun showMarkers(category: String) {
        googleMap?.clear()
        
        googleMap?.addMarker(MarkerOptions().position(hotelLocation).title("StayHub Hotel"))
        
        places.filter { it.category == category }.forEach { place ->
            googleMap?.addMarker(
                MarkerOptions()
                    .position(place.location)
                    .title(place.name)
                    .snippet(place.category)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}