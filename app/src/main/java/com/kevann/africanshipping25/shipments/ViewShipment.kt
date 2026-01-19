package com.kevann.africanshipping25.shipments

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kevann.africanshipping25.R  // Add this import


class ViewShipment : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private var currentShipmentId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_shipment)

        bottomNavigationView = findViewById(R.id.bottomNavigation)

        // Retrieve the shipmentId passed from ShipmentsFragment
        currentShipmentId = intent.getStringExtra("shipmentId")

        // Set up bottom navigation
        setupBottomNavigation()

        // Load the default fragment based on whether a shipmentId is passed
        if (savedInstanceState == null) {
            if (!currentShipmentId.isNullOrEmpty()) {
                loadFragment(enter_truck_goods.newInstance(currentShipmentId!!))
            } else {
                Toast.makeText(this, "No Shipment ID was passed.", Toast.LENGTH_SHORT).show()
                // You might also want to load a very basic default fragment here

            }
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.enterTruckGoods -> {
                    currentShipmentId?.let { //passing id
                        loadFragment(enter_truck_goods.newInstance(it))
                        return@setOnItemSelectedListener true
                    }
                    Toast.makeText(this, "No Shipment selected.", Toast.LENGTH_SHORT).show()
                    return@setOnItemSelectedListener false
                }
                R.id.viewTruckGoods -> {
                    currentShipmentId?.let {
                    // Load your view truck goods fragment without passing shipmentId initially
                        loadFragment(view_truck_goods.newInstance(it))

                        return@setOnItemSelectedListener true
                    }
                    Toast.makeText(this, "No Shipment selected.", Toast.LENGTH_SHORT).show()
                    return@setOnItemSelectedListener false
                }
                R.id.enterStoreGoods -> {
                    currentShipmentId?.let {
                        loadFragment(enter_store_goods.newInstance(it))
                        return@setOnItemSelectedListener true
                    }
                    Toast.makeText(this, "No Shipment selected.", Toast.LENGTH_SHORT).show()
                    return@setOnItemSelectedListener false
                }
                R.id.viewStoreGoods -> {
                    currentShipmentId?.let {
                        // Load your view truck goods fragment without passing shipmentId initially
                        loadFragment(view_store_goods.newInstance(it))

                        return@setOnItemSelectedListener true
                    }
                    Toast.makeText(this, "No Shipment selected.", Toast.LENGTH_SHORT).show()
                    return@setOnItemSelectedListener false
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment) // Ensure this ID exists in activity_view_shipment.xml
            .commit()
    }
}