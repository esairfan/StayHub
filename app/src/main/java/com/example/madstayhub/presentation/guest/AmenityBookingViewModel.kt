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
        db.collection("amenities").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            
            if (snapshot.isEmpty) {
                // Populate default amenities if Firestore is empty
                val defaults = listOf(
                    hashMapOf("name" to "Swimming Pool", "hours" to "06:00 AM - 10:00 PM"),
                    hashMapOf("name" to "Luxury Gym & Wellness Center", "hours" to "24 Hours Open"),
                    hashMapOf("name" to "Rejuvenating Spa & Massage", "hours" to "09:00 AM - 09:00 PM"),
                    hashMapOf("name" to "Executive Conference Room", "hours" to "08:00 AM - 08:00 PM")
                )
                for (item in defaults) {
                    db.collection("amenities").document().set(item)
                }
                return@addSnapshotListener
            }

            val list = snapshot.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null
                val hours = doc.getString("hours") ?: "00:00 - 00:00"
                
                // Map icons based on names for visual premium aesthetics
                val icon = when {
                    name.contains("pool", true) -> R.drawable.swimming_pool
                    name.contains("gym", true) || name.contains("wellness", true) -> R.drawable.gym
                    name.contains("spa", true) || name.contains("massage", true) -> R.drawable.spa
                    name.contains("conference", true) || name.contains("meeting", true) || name.contains("room", true) -> R.drawable.conferrence_room
                    else -> R.drawable.swimming_pool
                }
                AmenityItem(doc.id, name, hours, icon)
            }
            _amenities.value = list
        }
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
