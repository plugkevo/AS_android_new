package com.kevann.africanshipping25



import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

data class TrackingCheckpoint(
    val id: String = "",
    val location: LatLng = LatLng(0.0, 0.0),
    val locationName: String = "",
    val status: String = "",
    val timestamp: Date? = null,
    val notes: String = ""
)

data class ShipmentTrackingInfo(
    val shipmentId: String = "",
    val currentLocation: LatLng? = null,
    val currentLocationName: String = "",
    val currentStatus: String = "",
    val lastUpdated: Date? = null,
    val checkpoints: List<TrackingCheckpoint> = emptyList(),
    val origin: LatLng? = null,
    val destination: LatLng? = null,
    val originName: String = "",
    val destinationName: String = ""
)

class ShipmentTrackingService {

    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "ShipmentTrackingService"

    interface TrackingCallback {
        fun onSuccess(trackingInfo: ShipmentTrackingInfo)
        fun onError(error: String)
    }

    interface CheckpointCallback {
        fun onSuccess()
        fun onError(error: String)
    }

    // Fetch tracking info for a shipment
    fun getShipmentTracking(shipmentId: String, callback: TrackingCallback) {
        // First get the shipment document
        firestore.collection("shipments").document(shipmentId)
            .get()
            .addOnSuccessListener { shipmentDoc ->
                if (!shipmentDoc.exists()) {
                    callback.onError("Shipment not found")
                    return@addOnSuccessListener
                }

                val originLat = shipmentDoc.getDouble("originLat") ?: 0.0
                val originLng = shipmentDoc.getDouble("originLng") ?: 0.0
                val destLat = shipmentDoc.getDouble("destLat") ?: 0.0
                val destLng = shipmentDoc.getDouble("destLng") ?: 0.0
                val originName = shipmentDoc.getString("origin") ?: ""
                val destinationName = shipmentDoc.getString("destination") ?: ""
                val currentStatus = shipmentDoc.getString("status") ?: "Unknown"

                // Now get the checkpoints
                firestore.collection("shipments").document(shipmentId)
                    .collection("tracking_checkpoints")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .get()
                    .addOnSuccessListener { checkpointDocs ->
                        val checkpoints = mutableListOf<TrackingCheckpoint>()

                        for (doc in checkpointDocs) {
                            val lat = doc.getDouble("latitude") ?: 0.0
                            val lng = doc.getDouble("longitude") ?: 0.0
                            val checkpoint = TrackingCheckpoint(
                                id = doc.id,
                                location = LatLng(lat, lng),
                                locationName = doc.getString("locationName") ?: "",
                                status = doc.getString("status") ?: "",
                                timestamp = doc.getDate("timestamp"),
                                notes = doc.getString("notes") ?: ""
                            )
                            checkpoints.add(checkpoint)
                        }

                        // Get current location from last checkpoint or use origin
                        val currentLocation = if (checkpoints.isNotEmpty()) {
                            checkpoints.last().location
                        } else {
                            LatLng(originLat, originLng)
                        }

                        val currentLocationName = if (checkpoints.isNotEmpty()) {
                            checkpoints.last().locationName
                        } else {
                            originName
                        }

                        val lastUpdated = if (checkpoints.isNotEmpty()) {
                            checkpoints.last().timestamp
                        } else {
                            null
                        }

                        val trackingInfo = ShipmentTrackingInfo(
                            shipmentId = shipmentId,
                            currentLocation = currentLocation,
                            currentLocationName = currentLocationName,
                            currentStatus = currentStatus,
                            lastUpdated = lastUpdated,
                            checkpoints = checkpoints,
                            origin = LatLng(originLat, originLng),
                            destination = LatLng(destLat, destLng),
                            originName = originName,
                            destinationName = destinationName
                        )

                        callback.onSuccess(trackingInfo)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error fetching checkpoints", e)
                        callback.onError("Error fetching checkpoints: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching shipment", e)
                callback.onError("Error fetching shipment: ${e.message}")
            }
    }

    // Add a new tracking checkpoint
    fun addCheckpoint(
        shipmentId: String,
        latitude: Double,
        longitude: Double,
        locationName: String,
        status: String,
        notes: String = "",
        callback: CheckpointCallback
    ) {
        val checkpointData = hashMapOf(
            "latitude" to latitude,
            "longitude" to longitude,
            "locationName" to locationName,
            "status" to status,
            "notes" to notes,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        firestore.collection("shipments").document(shipmentId)
            .collection("tracking_checkpoints")
            .add(checkpointData)
            .addOnSuccessListener {
                // Also update the shipment's main status
                firestore.collection("shipments").document(shipmentId)
                    .update(
                        mapOf(
                            "status" to status,
                            "currentLocationName" to locationName,
                            "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )
                    )
                    .addOnSuccessListener {
                        callback.onSuccess()
                    }
                    .addOnFailureListener { e ->
                        callback.onError("Checkpoint added but status update failed: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding checkpoint", e)
                callback.onError("Error adding checkpoint: ${e.message}")
            }
    }

    // Get predefined route checkpoints for common shipping routes
    fun getPredefinedRouteCheckpoints(origin: String, destination: String): List<LatLng> {
        // Define common African shipping routes
        val routes = mapOf(
            "Mombasa-Kampala" to listOf(
                LatLng(-4.0435, 39.6682),  // Mombasa Port
                LatLng(-3.3972, 38.5561),  // Voi
                LatLng(-2.2717, 37.8282),  // Mtito Andei
                LatLng(-1.2921, 36.8219),  // Nairobi
                LatLng(-0.0917, 34.7680),  // Kisumu
                LatLng(0.0512, 34.5954),   // Busia Border
                LatLng(0.3476, 32.5825)    // Kampala
            ),
            "Dar es Salaam-Kigali" to listOf(
                LatLng(-6.7924, 39.2083),  // Dar es Salaam Port
                LatLng(-6.1630, 35.7516),  // Dodoma
                LatLng(-4.8769, 29.6260),  // Kigoma
                LatLng(-2.5000, 28.8603),  // Burundi Border
                LatLng(-1.9706, 30.1044)   // Kigali
            ),
            "Durban-Johannesburg" to listOf(
                LatLng(-29.8587, 31.0218),  // Durban Port
                LatLng(-29.1216, 26.2140),  // Bloemfontein
                LatLng(-26.2041, 28.0473)   // Johannesburg
            ),
            "Lagos-Accra" to listOf(
                LatLng(6.4541, 3.3947),    // Lagos Port
                LatLng(6.1319, 1.2228),    // Lome
                LatLng(5.5560, -0.1969)    // Accra
            )
        )

        // Check if we have a predefined route
        val routeKey = "$origin-$destination"
        val reverseRouteKey = "$destination-$origin"

        return when {
            routes.containsKey(routeKey) -> routes[routeKey]!!
            routes.containsKey(reverseRouteKey) -> routes[reverseRouteKey]!!.reversed()
            else -> emptyList()
        }
    }

    // Calculate estimated progress percentage
    fun calculateProgress(checkpoints: List<TrackingCheckpoint>, totalRoutePoints: Int): Int {
        if (totalRoutePoints == 0) return 0
        return ((checkpoints.size.toFloat() / totalRoutePoints.toFloat()) * 100).toInt().coerceIn(0, 100)
    }

    // Format timestamp for display
    fun formatTimestamp(date: Date?): String {
        if (date == null) return "Unknown"
        val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return formatter.format(date)
    }
}