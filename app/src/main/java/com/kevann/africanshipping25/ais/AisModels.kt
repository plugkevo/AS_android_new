package com.kevann.africanshipping25.ais

// Data model for AIS Hub API response
data class VesselLocation(
    val MMSI: String? = null,
    val IMO: String? = null,
    val Callsign: String? = null,
    val Name: String? = null,
    val Latitude: Double? = null,
    val Longitude: Double? = null,
    val Status: String? = null,
    val Speed: Double? = null,
    val Course: Double? = null,
    val LastUpdate: String? = null,
    val ShipType: String? = null
)

data class AisHubResponse(
    val result: List<VesselLocation>? = null,
    val error: String? = null,
    val status: String? = null
)

// Model for storing ship data in Firestore
data class Ship(
    val id: String = "",
    val name: String = "",
    val number: String = "",
    val imoNumber: String = "",
    val currentLatitude: Double = 0.0,
    val currentLongitude: Double = 0.0,
    val lastLocationUpdate: Long = 0,
    val speed: Double = 0.0,
    val course: Double = 0.0,
    val status: String = "Active",
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

// Model for storing location history
data class ShipLocationSnapshot(
    val id: String = "",
    val shipId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speed: Double = 0.0,
    val course: Double = 0.0,
    val timestamp: Long = 0
)
