package com.example.africanshipping25

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NotificationItem(
    val id: Long = System.currentTimeMillis(), // Unique ID for the notification
    val title: String,
    val body: String,
    val customData: Map<String, String>, // Store all custom key-value pairs
    val timestamp: Long = System.currentTimeMillis()
)
{
    fun getFormattedTimestamp(): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getCustomDataString(): String {
        return customData.entries.joinToString(", ") { "${it.key}: ${it.value}" }
    }
}