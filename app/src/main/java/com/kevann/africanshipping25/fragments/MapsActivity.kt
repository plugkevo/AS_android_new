package com.kevann.africanshipping25.fragments


import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kevann.africanshipping25.R
import com.kevann.africanshipping25.ShipmentTrackingInfo
import com.kevann.africanshipping25.ShipmentTrackingService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var etTrackingNumber: EditText
    private lateinit var btnTrack: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var trackingInfoCard: CardView
    private lateinit var tvShipmentName: TextView
    private lateinit var tvCurrentStatus: TextView
    private lateinit var tvCurrentLocation: TextView
    private lateinit var tvLastUpdated: TextView
    private lateinit var tvOrigin: TextView
    private lateinit var tvDestination: TextView
    private lateinit var progressTrack: ProgressBar
    private lateinit var tvProgressPercent: TextView

    private val firestore = FirebaseFirestore.getInstance()
    private val trackingService = ShipmentTrackingService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        initViews()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnTrack.setOnClickListener {
            val shipmentName = etTrackingNumber.text.toString().trim()
            if (shipmentName.isNotEmpty()) {
                fetchShipmentTracking(shipmentName)
            } else {
                Toast.makeText(this, "Please enter a shipment name", Toast.LENGTH_SHORT).show()
            }
        }

        // Check if shipment ID was passed via intent
        intent.getStringExtra("shipmentId")?.let { shipmentId ->
            etTrackingNumber.setText(shipmentId)
            fetchShipmentTrackingById(shipmentId)
        }

        intent.getStringExtra("shipmentName")?.let { shipmentName ->
            etTrackingNumber.setText(shipmentName)
            fetchShipmentTracking(shipmentName)
        }
    }

    private fun initViews() {
        etTrackingNumber = findViewById(R.id.et_tracking_number)
        btnTrack = findViewById(R.id.btn_track)
        progressBar = findViewById(R.id.progress_bar)
        trackingInfoCard = findViewById(R.id.tracking_info_card)
        tvShipmentName = findViewById(R.id.tv_shipment_name)
        tvCurrentStatus = findViewById(R.id.tv_current_status)
        tvCurrentLocation = findViewById(R.id.tv_current_location)
        tvLastUpdated = findViewById(R.id.tv_last_updated)
        tvOrigin = findViewById(R.id.tv_origin)
        tvDestination = findViewById(R.id.tv_destination)
        progressTrack = findViewById(R.id.progress_track)
        tvProgressPercent = findViewById(R.id.tv_progress_percent)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Set default location (Africa centered)
        val defaultLocation = LatLng(-1.286389, 36.817223) // Nairobi
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 5f))

        // Enable zoom controls
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = true
    }

    private fun fetchShipmentTracking(shipmentName: String) {
        showLoading()

        // First find the shipment by name
        firestore.collection("shipments")
            .whereEqualTo("name", shipmentName)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.documents.first()
                    val shipmentId = document.id
                    loadTrackingData(shipmentId, shipmentName)
                } else {
                    hideLoading()
                    Toast.makeText(this, "Shipment '$shipmentName' not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                hideLoading()
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchShipmentTrackingById(shipmentId: String) {
        showLoading()

        firestore.collection("shipments").document(shipmentId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val shipmentName = document.getString("name") ?: shipmentId
                    loadTrackingData(shipmentId, shipmentName)
                } else {
                    hideLoading()
                    Toast.makeText(this, "Shipment not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                hideLoading()
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadTrackingData(shipmentId: String, shipmentName: String) {
        trackingService.getShipmentTracking(shipmentId, object : ShipmentTrackingService.TrackingCallback {
            override fun onSuccess(trackingInfo: ShipmentTrackingInfo) {
                runOnUiThread {
                    hideLoading()
                    displayTrackingOnMap(trackingInfo, shipmentName)
                    updateTrackingInfoCard(trackingInfo, shipmentName)
                }
            }

            override fun onError(error: String) {
                runOnUiThread {
                    // Fall back to basic tracking if no checkpoints exist
                    loadBasicShipmentLocation(shipmentId, shipmentName)
                }
            }
        })
    }

    private fun loadBasicShipmentLocation(shipmentId: String, shipmentName: String) {
        firestore.collection("shipments").document(shipmentId)
            .get()
            .addOnSuccessListener { document ->
                hideLoading()

                val latitude = document.getDouble("latitude")
                    ?: document.getDouble("originLat")
                    ?: 0.0
                val longitude = document.getDouble("longitude")
                    ?: document.getDouble("originLng")
                    ?: 0.0
                val status = document.getString("status") ?: "In Transit"
                val origin = document.getString("origin") ?: "Unknown"
                val destination = document.getString("destination") ?: "Unknown"

                if (latitude != 0.0 && longitude != 0.0) {
                    mMap.clear()
                    val location = LatLng(latitude, longitude)

                    mMap.addMarker(
                        MarkerOptions()
                            .position(location)
                            .title("Shipment: $shipmentName")
                            .snippet("Status: $status")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    )

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 12f))

                    // Update info card with basic info
                    trackingInfoCard.visibility = View.VISIBLE
                    tvShipmentName.text = shipmentName
                    tvCurrentStatus.text = status
                    tvCurrentLocation.text = "Current: $origin"
                    tvLastUpdated.text = "Last Updated: N/A"
                    tvOrigin.text = "From: $origin"
                    tvDestination.text = "To: $destination"
                    progressTrack.progress = 0
                    tvProgressPercent.text = "0%"
                } else {
                    Toast.makeText(this, "No location data for this shipment", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                hideLoading()
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayTrackingOnMap(trackingInfo: ShipmentTrackingInfo, shipmentName: String) {
        mMap.clear()

        val boundsBuilder = LatLngBounds.Builder()
        var hasPoints = false

        // Add origin marker (Green)
        trackingInfo.origin?.let { origin ->
            if (origin.latitude != 0.0 && origin.longitude != 0.0) {
                mMap.addMarker(
                    MarkerOptions()
                        .position(origin)
                        .title("Origin: ${trackingInfo.originName}")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                )
                boundsBuilder.include(origin)
                hasPoints = true
            }
        }

        // Add destination marker (Red)
        trackingInfo.destination?.let { dest ->
            if (dest.latitude != 0.0 && dest.longitude != 0.0) {
                mMap.addMarker(
                    MarkerOptions()
                        .position(dest)
                        .title("Destination: ${trackingInfo.destinationName}")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )
                boundsBuilder.include(dest)
                hasPoints = true
            }
        }

        // Add checkpoint markers (Blue) and build route
        val routePoints = mutableListOf<LatLng>()
        trackingInfo.origin?.let { if (it.latitude != 0.0) routePoints.add(it) }

        trackingInfo.checkpoints.forEachIndexed { index, checkpoint ->
            mMap.addMarker(
                MarkerOptions()
                    .position(checkpoint.location)
                    .title("Checkpoint ${index + 1}: ${checkpoint.locationName}")
                    .snippet("Status: ${checkpoint.status}\n${formatTimestamp(checkpoint.timestamp)}")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
            routePoints.add(checkpoint.location)
            boundsBuilder.include(checkpoint.location)
            hasPoints = true
        }

        // Add current location marker (Orange) - last known position
        trackingInfo.currentLocation?.let { currentLoc ->
            if (currentLoc.latitude != 0.0 && currentLoc.longitude != 0.0) {
                mMap.addMarker(
                    MarkerOptions()
                        .position(currentLoc)
                        .title("Current Location")
                        .snippet("${trackingInfo.currentLocationName}\nStatus: ${trackingInfo.currentStatus}")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                )
            }
        }

        // Draw completed route (solid blue line)
        if (routePoints.size >= 2) {
            mMap.addPolyline(
                PolylineOptions()
                    .addAll(routePoints)
                    .width(10f)
                    .color(Color.BLUE)
            )
        }

        // Draw remaining route to destination (dashed gray line)
        if (routePoints.isNotEmpty() && trackingInfo.destination != null) {
            val remainingRoute = listOf(routePoints.last(), trackingInfo.destination)
            mMap.addPolyline(
                PolylineOptions()
                    .addAll(remainingRoute)
                    .width(8f)
                    .color(Color.GRAY)
                    .pattern(listOf(
                        com.google.android.gms.maps.model.Dash(30f),
                        com.google.android.gms.maps.model.Gap(20f)
                    ))
            )
        }

        // Move camera to show all markers
        if (hasPoints) {
            try {
                val bounds = boundsBuilder.build()
                val padding = 150 // pixels
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
            } catch (e: Exception) {
                // If bounds building fails, center on current location
                trackingInfo.currentLocation?.let { loc ->
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 10f))
                }
            }
        }
    }

    private fun updateTrackingInfoCard(trackingInfo: ShipmentTrackingInfo, shipmentName: String) {
        trackingInfoCard.visibility = View.VISIBLE

        tvShipmentName.text = shipmentName
        tvCurrentStatus.text = trackingInfo.currentStatus
        tvCurrentLocation.text = "Current: ${trackingInfo.currentLocationName}"
        tvLastUpdated.text = "Last Updated: ${formatTimestamp(trackingInfo.lastUpdated)}"
        tvOrigin.text = "From: ${trackingInfo.originName}"
        tvDestination.text = "To: ${trackingInfo.destinationName}"

        // Calculate progress
        val totalCheckpoints = 5 // Estimate total checkpoints for a typical journey
        val completedCheckpoints = trackingInfo.checkpoints.size
        val progress = ((completedCheckpoints.toFloat() / totalCheckpoints.toFloat()) * 100).toInt().coerceIn(0, 100)

        progressTrack.progress = progress
        tvProgressPercent.text = "$progress%"

        // Update status color
        when (trackingInfo.currentStatus.lowercase()) {
            "delivered" -> tvCurrentStatus.setTextColor(Color.parseColor("#4CAF50"))
            "in transit" -> tvCurrentStatus.setTextColor(Color.parseColor("#2196F3"))
            "pending" -> tvCurrentStatus.setTextColor(Color.parseColor("#FF9800"))
            "delayed" -> tvCurrentStatus.setTextColor(Color.parseColor("#F44336"))
            else -> tvCurrentStatus.setTextColor(Color.parseColor("#757575"))
        }
    }

    private fun formatTimestamp(date: Date?): String {
        if (date == null) return "N/A"
        val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return formatter.format(date)
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        btnTrack.isEnabled = false
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        btnTrack.isEnabled = true
    }
}