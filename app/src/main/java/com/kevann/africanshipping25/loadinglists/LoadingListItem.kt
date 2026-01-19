package com.kevann.africanshipping25.loadinglists

data class LoadingListItem(
    var id: String = "", // Firestore document ID
    val name: String = "",
    val origin: String = "",
    val destination: String = "",
    val extraDetails: String = "", // Matches the field name from your dialog submission
    val status: String = "Open" // Default status for loading lists
)