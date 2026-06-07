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

data class FoodItem(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val category: String,
    val imageResId: Int = android.R.drawable.ic_menu_gallery
)

@HiltViewModel
class RoomServiceViewModel @Inject constructor() : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _menuItems = MutableStateFlow<List<FoodItem>>(emptyList())
    val menuItems: StateFlow<List<FoodItem>> = _menuItems.asStateFlow()

    private val _cart = MutableStateFlow<Map<FoodItem, Int>>(emptyMap())
    val cart: StateFlow<Map<FoodItem, Int>> = _cart.asStateFlow()

    private val _orderPlacedId = MutableStateFlow<String?>(null)
    val orderPlacedId: StateFlow<String?> = _orderPlacedId.asStateFlow()

    private val _orderError = MutableStateFlow<String?>(null)
    val orderError: StateFlow<String?> = _orderError.asStateFlow()

    init {
        loadMenu()
    }

    private fun loadMenu() {
        val list = listOf(
            FoodItem("1", "Gourmet Burger", "Juicy beef patty with cheese and fries", 850.0, "Main Course", R.drawable.burger),
            FoodItem("2", "Club Sandwich", "Classic double-decker sandwich with fries", 650.0, "Main Course", R.drawable.club_sandwich),
            FoodItem("3", "Pancake Stack", "Fluffy pancakes served with maple syrup", 450.0, "Breakfast", R.drawable.pancake_stack),
            FoodItem("4", "English Breakfast", "Eggs, bacon, sausages, toast, and beans", 750.0, "Breakfast", R.drawable.english_breakfast),
            FoodItem("5", "Fresh Orange Juice", "Freshly squeezed sweet orange juice", 200.0, "Beverages", R.drawable.orange_juice),
            FoodItem("6", "Iced Latte", "Chilled espresso with creamy milk", 250.0, "Beverages", R.drawable.iced_latte),
            FoodItem("7", "Chocolate Lava Cake", "Warm cake with a molten chocolate center", 350.0, "Desserts", R.drawable.choclate_lava),
            FoodItem("8", "New York Cheesecake", "Creamy cheesecake with strawberry topping", 400.0, "Desserts", R.drawable.new_yorkcheese_cake)
        )
        _menuItems.value = list
    }

    fun addToCart(item: FoodItem) {
        val current = _cart.value.toMutableMap()
        current[item] = current.getOrDefault(item, 0) + 1
        _cart.value = current
    }

    fun decreaseQuantity(item: FoodItem) {
        val current = _cart.value.toMutableMap()
        val qty = current.getOrDefault(item, 0)
        if (qty <= 1) {
            current.remove(item)
        } else {
            current[item] = qty - 1
        }
        _cart.value = current
    }

    fun clearCart() {
        _cart.value = emptyMap()
        _orderPlacedId.value = null
    }

    fun placeOrder(roomNumber: String, guestName: String) {
        val uid = auth.currentUser?.uid ?: return
        val currentCart = _cart.value
        if (currentCart.isEmpty()) return

        val itemsList = currentCart.map { (item, qty) ->
            hashMapOf(
                "itemId" to item.id,
                "itemName" to item.name,
                "quantity" to qty,
                "price" to item.price
            )
        }

        val totalAmount = currentCart.entries.sumOf { it.key.price * it.value }

        val orderDoc = hashMapOf(
            "guestUid" to uid,
            "guestName" to guestName,
            "roomNumber" to roomNumber,
            "items" to itemsList,
            "totalAmount" to totalAmount,
            "status" to "ordered",
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        _orderPlacedId.value = null
        _orderError.value = null

        val ref = db.collection("room_service_orders").document()
        ref.set(orderDoc)
            .addOnFailureListener { e ->
                _orderError.value = e.message
            }
        
        _orderPlacedId.value = ref.id
    }

    fun resetOrderState() {
        _orderPlacedId.value = null
        _orderError.value = null
    }
}
