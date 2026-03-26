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
            Log.d(tag, "[v0] Device online, starting sync...")
            syncTruckGoods()
            syncStoreGoods()
            syncLoadingLists()
            syncWarehouseGoods()
        } else {
            Log.d(tag, "[v0] Device offline, sync skipped")
        }
    }

    private fun syncTruckGoods() {
        try {
            val unsyncedGoods = OfflineDataStore.getUnsyncedTruckGoods(context)
            Log.d(tag, "[v0] Found ${unsyncedGoods.size} unsynced truck goods")
            for (good in unsyncedGoods) {
                Log.d(tag, "[v0] Syncing truck good: ID=${good.id}, shipmentId=${good.shipmentId}, name=${good.name}")
                firestore.collection("shipments")
                    .document(good.shipmentId)
                    .collection("truck_inventory")
                    .add(mapOf(
                        "name" to good.name,
                        "goodsNumber" to good.goodsNumber,
                        "createdAt" to System.currentTimeMillis()
                    ))
                    .addOnSuccessListener {
                        OfflineDataStore.markTruckGoodAsSynced(good.id, context)
                        Log.d(tag, "[v0] Truck good synced successfully: ${good.id}")
                    }
                    .addOnFailureListener { e ->
                        Log.e(tag, "[v0] Error syncing truck good: ${e.message}", e)
                    }
            }
            if (unsyncedGoods.isEmpty()) {
                Log.d(tag, "[v0] No unsynced truck goods to sync")
            }
        } catch (e: Exception) {
            Log.e(tag, "[v0] Error syncing truck goods: ${e.message}", e)
        }
    }

    private fun syncStoreGoods() {
        try {
            val unsyncedGoods = OfflineDataStore.getUnsyncedStoreGoods(context)
            Log.d(tag, "[v0] Found ${unsyncedGoods.size} unsynced store goods")
            for (good in unsyncedGoods) {
                Log.d(tag, "[v0] Syncing store good: ID=${good.id}, shipmentId=${good.shipmentId}, location=${good.storeLocation}")
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
                        OfflineDataStore.markStoreGoodAsSynced(good.id, context)
                        Log.d(tag, "[v0] Store good synced successfully: ${good.id}")
                    }
                    .addOnFailureListener { e ->
                        Log.e(tag, "[v0] Error syncing store good: ${e.message}", e)
                    }
            }
            if (unsyncedGoods.isEmpty()) {
                Log.d(tag, "[v0] No unsynced store goods to sync")
            }
        } catch (e: Exception) {
            Log.e(tag, "[v0] Error syncing store goods: ${e.message}", e)
        }
    }

    private fun syncLoadingLists() {
        try {
            val unsyncedLists = OfflineDataStore.getUnsyncedLoadingLists(context)
            Log.d(tag, "[v0] Found ${unsyncedLists.size} unsynced loading lists")
            for (list in unsyncedLists) {
                Log.d(tag, "[v0] Syncing loading list: ID=${list.id}, name=${list.name}")
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
                        OfflineDataStore.markLoadingListAsSynced(list.id, context)
                        Log.d(tag, "[v0] Loading list synced successfully: ${list.id}")
                    }
                    .addOnFailureListener { e ->
                        Log.e(tag, "[v0] Error syncing loading list: ${e.message}", e)
                    }
            }
            if (unsyncedLists.isEmpty()) {
                Log.d(tag, "[v0] No unsynced loading lists to sync")
            }
        } catch (e: Exception) {
            Log.e(tag, "[v0] Error syncing loading lists: ${e.message}", e)
        }
    }

    private fun syncWarehouseGoods() {
        try {
            val unsyncedGoods = OfflineDataStore.getUnsyncedWarehouseGoods(context)
            Log.d(tag, "[v0] Found ${unsyncedGoods.size} unsynced warehouse goods")
            for (good in unsyncedGoods) {
                Log.d(tag, "[v0] Syncing warehouse good: ID=${good.id}, loadingListId=${good.loadingListId}, goodNo=${good.goodNo}")
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
                        OfflineDataStore.markWarehouseGoodAsSynced(good.id, context)
                        Log.d(tag, "[v0] Warehouse good synced successfully: ${good.id}")
                    }
                    .addOnFailureListener { e ->
                        Log.e(tag, "[v0] Error syncing warehouse good: ${e.message}", e)
                    }
            }
            if (unsyncedGoods.isEmpty()) {
                Log.d(tag, "[v0] No unsynced warehouse goods to sync")
            }
        } catch (e: Exception) {
            Log.e(tag, "[v0] Error syncing warehouse goods: ${e.message}", e)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
