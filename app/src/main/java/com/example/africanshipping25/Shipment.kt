package com.example.africanshipping25

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Shipment(
    var id: String = "",
    var name: String = "",
    var origin: String = "",
    var destination: String = "",
    var weight: Double = 0.0,
    var details: String ="",
    var status: String="",
    var date:String= "",
    var latitude: Double? = null,
    var longitude: Double? = null,
    @ServerTimestamp // This annotation will automatically convert Firestore Timestamp to Date
    var createdAt: Date? = null // Using nullable Date for the timestamp
    // Add more fields as needed (e.g., status, tracking number, etc.)
)