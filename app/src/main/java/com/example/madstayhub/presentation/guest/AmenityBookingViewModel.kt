package com.example.madstayhub.presentation.guest

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

import com.example.madstayhub.R

data class AmenityItem(
    val id: String,
    val name: String,
    val hours: String,
    val iconResId: Int = R.drawable.swimming_pool
)

@HiltViewModel
class AmenityBookingViewModel @Inject constructor() : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _amenities = MutableStateFlow<List<AmenityItem>>(emptyList())
    val amenities: StateFlow<List<AmenityItem>> = _amenities.asStateFlow()

    private val _selectedAmenity = MutableStateFlow<AmenityItem?>(null)
    val selectedAmenity: StateFlow<AmenityItem?> = _selectedAmenity.asStateFlow()

    private val _bookingConfirmed = MutableStateFlow(false)
    val bookingConfirmed: StateFlow<Boolean> = _bookingConfirmed.asStateFlow()

    init {
        loadAmenities()
    }

    private fun loadAmenities() {
        val list = listOf(
            AmenityItem("1", "Swimming Pool", "06:00 AM - 10:00 PM", R.drawable.swimming_pool),
            AmenityItem("2", "Luxury Gym & Wellness Center", "24 Hours Open", R.drawable.gym),
            AmenityItem("3", "Rejuvenating Spa & Massage", "09:00 AM - 09:00 PM", R.drawable.spa),
            AmenityItem("4", "Executive Conference Room", "08:00 AM - 08:00 PM", R.drawable.conferrence_room)
        )
        _amenities.value = list
    }

    fun selectAmenity(amenity: AmenityItem) {
        _selectedAmenity.value = amenity
    }

    fun bookAmenity(date: String, timeSlot: String, roomNumber: String, guestName: String) {
        val uid = auth.currentUser?.uid ?: return
        val amenity = _selectedAmenity.value ?: return

        val bookingDoc = hashMapOf(
            "guestUid" to uid,
            "guestName" to guestName,
            "roomNumber" to roomNumber,
            "amenityId" to amenity.id,
            "amenityName" to amenity.name,
            "date" to date,
            "timeSlot" to timeSlot,
            "status" to "booked",
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        // Write optimistically
        db.collection("amenity_bookings").document().set(bookingDoc)
        _bookingConfirmed.value = true
    }

    fun resetBookingState() {
        _bookingConfirmed.value = false
        _selectedAmenity.value = null
    }
}
