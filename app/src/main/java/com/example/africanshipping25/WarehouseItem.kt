package com.example.africanshipping25

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class WarehouseItem(
    @get:Exclude @set:Exclude var id: String = "", // Firestore doc ID, marked to be ignored by Firestore's toObject/fromObject
    val goodNo: String = "",
    val senderName: String = "",
    val date: String = "",
    @ServerTimestamp // Annotation to automatically set this field to server timestamp on creation/update
    val timestamp: Date? = null // Added to retrieve the timestamp if desired
)