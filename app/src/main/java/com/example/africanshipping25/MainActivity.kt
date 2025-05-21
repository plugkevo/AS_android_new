package com.example.africanshipping25


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views

        bottomNavigation = findViewById(R.id.bottom_navigation)
        val fabNewShipment = findViewById<FloatingActionButton>(R.id.fab_new_shipment)

        // Set user name dynamically



        fabNewShipment.setOnClickListener {
            // Navigate to create shipment screen
            openNewShipmentForm()
        }

        // Set up bottom navigation
        setupBottomNavigation()

        // Load the default fragment (Home)
        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.nav_home
        }

    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    return@setOnItemSelectedListener true
                }
                R.id.nav_shipments -> {
                    loadFragment(ShipmentsFragment())
                    return@setOnItemSelectedListener true
                }
                R.id.nav_track -> {
                    loadFragment(PaymentFragment())
                    return@setOnItemSelectedListener true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    return@setOnItemSelectedListener true
                }
            }
            false
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun openNewShipmentForm() {
        // You can either start a new activity or show a dialog/fragment
        // For now, we'll just show a simple dialog
        val dialog = NewShipmentDialogFragment()
        dialog.show(supportFragmentManager, "NewShipmentDialog")
    }
}