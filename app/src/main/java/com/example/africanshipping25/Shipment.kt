package com.example.africanshipping25

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
    var longitude: Double? = null
    // Add more fields as needed (e.g., status, tracking number, etc.)
)