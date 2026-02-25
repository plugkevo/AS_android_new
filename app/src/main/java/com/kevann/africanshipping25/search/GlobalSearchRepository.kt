package com.kevann.africanshipping25.search

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.kevann.africanshipping25.shipments.StoreGood
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class SearchResultItem(
    val goodsNumber: Long? = null,
    val name: String? = null,
    val storeLocation: String? = null,
    val shipmentId: String = "",
    val shipmentName: String = "",
    val category: String = "", // "store" or "truck"
    val truckLocation: String? = null // for truck goods
)

class GlobalSearchRepository {
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "GlobalSearchRepository"

    /**
     * Search for goods across all shipments and both inventories (store and truck)
     * @param searchQuery The search term
     * @return List of SearchResultItem containing matching goods with shipment info
     */
    suspend fun searchAllGoodsAcrossShipments(searchQuery: String): List<SearchResultItem> {
        return suspendCancellableCoroutine { continuation ->
            val allResults = mutableListOf<SearchResultItem>()
            val shipments = mutableListOf<Pair<String, String>>() // Pair<shipmentId, shipmentName>

            // First, fetch all shipments
            db.collection("shipments")
                .get()
                .addOnSuccessListener { shipmentsSnapshot ->
                    if (shipmentsSnapshot.isEmpty) {
                        Log.d(TAG, "No shipments found")
                        continuation.resume(emptyList())
                        return@addOnSuccessListener
                    }

                    for (shipmentDoc in shipmentsSnapshot) {
                        val shipmentId = shipmentDoc.id
                        val shipmentName = shipmentDoc.getString("name") ?: "Unknown"
                        shipments.add(Pair(shipmentId, shipmentName))
                    }

                    // Now search in all shipments' store and truck inventories
                    var completedQueries = 0
                    val totalQueries = shipments.size * 2 // store + truck for each shipment

                    shipments.forEach { (shipmentId, shipmentName) ->
                        // Search in store_inventory
                        searchInStoreInventory(shipmentId, shipmentName, searchQuery) { storeResults ->
                            allResults.addAll(storeResults)
                            completedQueries++
                            if (completedQueries == totalQueries) {
                                continuation.resume(allResults.sortedByDescending { it.shipmentName })
                            }
                        }

                        // Search in truck_inventory
                        searchInTruckInventory(shipmentId, shipmentName, searchQuery) { truckResults ->
                            allResults.addAll(truckResults)
                            completedQueries++
                            if (completedQueries == totalQueries) {
                                continuation.resume(allResults.sortedByDescending { it.shipmentName })
                            }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error fetching shipments: ${exception.message}")
                    continuation.resume(emptyList())
                }
        }
    }

    private fun searchInStoreInventory(
        shipmentId: String,
        shipmentName: String,
        searchQuery: String,
        callback: (List<SearchResultItem>) -> Unit
    ) {
        db.collection("shipments")
            .document(shipmentId)
            .collection("store_inventory")
            .get()
            .addOnSuccessListener { snapshot ->
                val results = mutableListOf<SearchResultItem>()
                for (doc in snapshot) {
                    val goodsNumber = doc.getLong("goodsNumber")
                    val name = doc.getString("name") ?: ""
                    val storeLocation = doc.getString("storeLocation") ?: ""

                    // Case-insensitive search in name, goods number, and location
                    if (name.contains(searchQuery, ignoreCase = true) ||
                        goodsNumber?.toString()?.contains(searchQuery) == true ||
                        storeLocation.contains(searchQuery, ignoreCase = true)
                    ) {
                        results.add(
                            SearchResultItem(
                                goodsNumber = goodsNumber,
                                name = name,
                                storeLocation = storeLocation,
                                shipmentId = shipmentId,
                                shipmentName = shipmentName,
                                category = "store"
                            )
                        )
                    }
                }
                Log.d(TAG, "Found ${results.size} store goods in shipment $shipmentId")
                callback(results)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error searching store inventory: ${exception.message}")
                callback(emptyList())
            }
    }

    private fun searchInTruckInventory(
        shipmentId: String,
        shipmentName: String,
        searchQuery: String,
        callback: (List<SearchResultItem>) -> Unit
    ) {
        db.collection("shipments")
            .document(shipmentId)
            .collection("truck_inventory")
            .get()
            .addOnSuccessListener { snapshot ->
                val results = mutableListOf<SearchResultItem>()
                for (doc in snapshot) {
                    val goodsNumber = doc.getLong("goodsNumber")
                    val name = doc.getString("name") ?: ""
                    val truckLocation = doc.getString("truckLocation") ?: ""

                    // Case-insensitive search in name, goods number, and location
                    if (name.contains(searchQuery, ignoreCase = true) ||
                        goodsNumber?.toString()?.contains(searchQuery) == true ||
                        truckLocation.contains(searchQuery, ignoreCase = true)
                    ) {
                        results.add(
                            SearchResultItem(
                                goodsNumber = goodsNumber,
                                name = name,
                                truckLocation = truckLocation,
                                shipmentId = shipmentId,
                                shipmentName = shipmentName,
                                category = "truck"
                            )
                        )
                    }
                }
                Log.d(TAG, "Found ${results.size} truck goods in shipment $shipmentId")
                callback(results)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error searching truck inventory: ${exception.message}")
                callback(emptyList())
            }
    }

    /**
     * Get all shipment names for display purposes
     */
    suspend fun getAllShipmentNames(): List<Pair<String, String>> {
        return suspendCancellableCoroutine { continuation ->
            db.collection("shipments")
                .get()
                .addOnSuccessListener { snapshot ->
                    val shipments = mutableListOf<Pair<String, String>>()
                    for (doc in snapshot) {
                        shipments.add(Pair(doc.id, doc.getString("name") ?: "Unknown"))
                    }
                    continuation.resume(shipments)
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error fetching shipment names: ${exception.message}")
                    continuation.resume(emptyList())
                }
        }
    }
}
