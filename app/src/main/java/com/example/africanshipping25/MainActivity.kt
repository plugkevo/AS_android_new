package com.example.africanshipping25

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth // Import Firebase Auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.jvm.java

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var btnlogout: ImageView
    // You've declared firestore, but it's not initialized or used in your provided code
    // private lateinit var firestore: FirebaseFirestore

    // Declare FirebaseAuth
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_blue)


        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize views
        bottomNavigation = findViewById(R.id.bottom_navigation)
        val fabNewShipment = findViewById<FloatingActionButton>(R.id.fab_new_shipment)
        btnlogout = findViewById(R.id.ic_logout)

        // Set OnClickListener for the logout ImageView
        btnlogout.setOnClickListener {
            // Call the logout function
            performLogout()
        }

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

    private fun performLogout() {
        auth.signOut() // Sign out the current user

        // After logging out, navigate to your login/splash/onboarding activity
        val intent = Intent(this, login::class.java) // Replace LoginActivity with your actual login activity
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clears activity stack
        startActivity(intent)
        finish() // Finish MainActivity so the user can't navigate back to it
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
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
                R.id.nav_loading_list -> {
                    loadFragment(LoadingFragment())
                    return@setOnItemSelectedListener true
                }
                R.id.nav_payment -> {
                    loadFragment(PaymentFragment())
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