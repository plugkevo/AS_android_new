package com.example.africanshipping25

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var etTrackingNumber: EditText
    private lateinit var btnTrack: Button
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        etTrackingNumber = findViewById(R.id.et_tracking_number)
        btnTrack = findViewById(R.id.btn_track)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnTrack.setOnClickListener {
            val shipmentName = etTrackingNumber.text.toString().trim()
            if (shipmentName.isNotEmpty()) {
                fetchShipmentCoordinates(shipmentName)
            } else {
                Toast.makeText(this, "Please enter a shipment name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Optionally set a default location when the map is ready
        val defaultLocation = LatLng(-1.286389, 36.817223) // Nairobi as default
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))
    }

    private fun fetchShipmentCoordinates(shipmentName: String) {
        firestore.collection("shipments")
            .whereEqualTo("name", shipmentName)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {
                        val latitude = document.getDouble("latitude")
                        val longitude = document.getDouble("longitude")
                        val name = document.getString("name")

                        if (latitude != null && longitude != null && name != null) {
                            val shipmentLocation = LatLng(latitude, longitude)
                            mMap.addMarker(MarkerOptions().position(shipmentLocation).title("Shipment: $name"))
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(shipmentLocation, 15f))
                        } else {
                            Toast.makeText(this, "Coordinates not found for $shipmentName", Toast.LENGTH_SHORT).show()
                        }
                        // Assuming only one shipment will match the name, you can break here if needed
                        return@addOnSuccessListener
                    }
                } else {
                    Toast.makeText(this, "Shipment '$shipmentName' not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching shipment: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}