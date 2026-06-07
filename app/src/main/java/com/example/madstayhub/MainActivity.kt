package com.example.madstayhub

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.madstayhub.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Dynamically set graph if user is already signed in
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val prefs = getSharedPreferences("StayHubPrefs", android.content.Context.MODE_PRIVATE)
        val role = prefs.getString("user_role", null)

        if (user != null && role != null) {
            val inflater = navController.navInflater
            if (role == "staff" || user.email == "esairfan112@gmail.com") {
                binding.bottomNavigation.visibility = View.GONE
                navController.graph = inflater.inflate(R.navigation.nav_staff)
            } else {
                navController.graph = inflater.inflate(R.navigation.nav_guest)
            }
        }

        // Setup Bottom Navigation
        binding.bottomNavigation.setupWithNavController(navController)

        // Listen for destination changes to hide/show BottomNav
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val currentRole = getSharedPreferences("StayHubPrefs", android.content.Context.MODE_PRIVATE)
                .getString("user_role", null)
            if (currentRole == "staff") {
                binding.bottomNavigation.visibility = View.GONE
            } else {
                when (destination.id) {
                    R.id.splashFragment, R.id.loginFragment, R.id.registerFragment, 
                    R.id.otpFragment, R.id.forgotPasswordFragment -> {
                        binding.bottomNavigation.visibility = View.GONE
                    }
                    else -> {
                        binding.bottomNavigation.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    fun navigateToGuestFlow() {
        val prefs = getSharedPreferences("StayHubPrefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("user_role", "guest").apply()
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        binding.bottomNavigation.visibility = View.VISIBLE
        navController.graph = inflater.inflate(R.navigation.nav_guest)
    }

    fun navigateToStaffFlow() {
        val prefs = getSharedPreferences("StayHubPrefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("user_role", "staff").apply()
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        binding.bottomNavigation.visibility = View.GONE
        navController.graph = inflater.inflate(R.navigation.nav_staff)
    }
    
    fun logout() {
        val prefs = getSharedPreferences("StayHubPrefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().remove("user_role").apply()
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        navController.graph = inflater.inflate(R.navigation.nav_auth)
    }
}
