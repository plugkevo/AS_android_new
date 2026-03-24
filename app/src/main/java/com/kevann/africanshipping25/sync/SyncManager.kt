package com.kevann.africanshipping25.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.kevann.africanshipping25.database.OfflineDataStore

class SyncManager(private val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val tag = "SyncManager"

    init {
        OfflineDataStore.init(context)
    }

    fun syncAllData() {
        if (isNetworkAvailable()) {
            Log.d(tag, "Device online, starting sync...")
            syncTruckGoods()
            syncStoreGoods()
            syncLoadingLists()
            syncWarehouseGoods()
        }
    }

    private fun syncTruckGoods() {
        try {
            val unsyncedGoods = OfflineDataStore.getUnsyncedTruckGoods()
            for (good in unsyncedGoods) {
                firestore.collection("shipments")
                    .document(good.shipmentId)
                    .collection("truck_inventory")
                    .add(mapOf(
                        "name" to good.name,
                        "goodsNumber" to good.goodsNumber,
                        "createdAt" to System.currentTimeMillis()
                    ))
                    .addOnSuccessListener {
                        OfflineDataStore.markTruckGoodAsSynced(good.id)
                        Log.d(tag, "Truck good synced: ${good.id}")
                    }
                    .addOnFailureListener { e ->
                        Log.e(tag, "Error syncing truck good: ${e.message}")
                    }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error syncing truck goods: ${e.message}")
        }
    }

    private fun syncStoreGoods() {
        try {
            val unsyncedGoods = OfflineDataStore.getUnsyncedStoreGoods()
            for (good in unsyncedGoods) {
                firestore.collection("shipments")
                    .document(good.shipmentId)
                    .collection("store_inventory")
                    .add(mapOf(
                        "name" to good.name,
                        "storeLocation" to good.storeLocation,
                        "goodsNumber" to good.goodsNumber,
                        "createdAt" to System.currentTimeMillis()
                    ))
                    .addOnSuccessListener {
                        OfflineDataStore.markStoreGoodAsSynced(good.id)
                        Log.d(tag, "Store good synced: ${good.id}")
                    }
                    .addOnFailureListener { e ->
                        Log.e(tag, "Error syncing store good: ${e.message}")
                    }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error syncing store goods: ${e.message}")
        }
    }

    private fun syncLoadingLists() {
        try {
            val unsyncedLists = OfflineDataStore.getUnsyncedLoadingLists()
            for (list in unsyncedLists) {
                firestore.collection("loading_lists")
                    .add(mapOf(
                        "name" to list.name,
                        "origin" to list.origin,
                        "destination" to list.destination,
                        "extraDetails" to list.extraDetails,
                        "status" to list.status,
                        "createdAt" to FieldValue.serverTimestamp()
                    ))
                    .addOnSuccessListener {
                        OfflineDataStore.markLoadingListAsSynced(list.id)
                        Log.d(tag, "Loading list synced: ${list.id}")
                    }
                    .addOnFailureListener { e ->
                        Log.e(tag, "Error syncing loading list: ${e.message}")
                    }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error syncing loading lists: ${e.message}")
        }
    }

    private fun syncWarehouseGoods() {
        try {
            val unsyncedGoods = OfflineDataStore.getUnsyncedWarehouseGoods()
            for (good in unsyncedGoods) {
                firestore.collection("loading_lists")
                    .document(good.loadingListId)
                    .collection("warehouseItems")
                    .add(mapOf(
                        "goodNo" to good.goodNo,
                        "senderName" to good.senderName,
                        "phoneNumber" to good.phoneNumber,
                        "date" to good.date,
                        "createdAt" to System.currentTimeMillis()
                    ))
                    .addOnSuccessListener {
                        OfflineDataStore.markWarehouseGoodAsSynced(good.id)
                        Log.d(tag, "Warehouse good synced: ${good.id}")
                    }
                    .addOnFailureListener { e ->
                        Log.e(tag, "Error syncing warehouse good: ${e.message}")
                    }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error syncing warehouse goods: ${e.message}")
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
