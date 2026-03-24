package com.kevann.africanshipping25.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.kevann.africanshipping25.database.OfflineDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SyncManager(private val context: Context) {
    private val db = OfflineDatabase.getInstance(context)
    private val firestore = FirebaseFirestore.getInstance()
    private val tag = "SyncManager"

    fun syncAllData() {
        if (isNetworkAvailable()) {
            CoroutineScope(Dispatchers.IO).launch {
                syncTruckGoods()
                syncStoreGoods()
                syncLoadingLists()
            }
        }
    }

    private suspend fun syncTruckGoods() {
        try {
            val unsyncedGoods = db.truckGoodsDao().getUnsyncedTruckGoods()
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
                        CoroutineScope(Dispatchers.IO).launch {
                            db.truckGoodsDao().markAsSynced(good.id)
                        }
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

    private suspend fun syncStoreGoods() {
        try {
            val unsyncedGoods = db.storeGoodsDao().getUnsyncedStoreGoods()
            for (good in unsyncedGoods) {
                firestore.collection("shipments")
                    .document(good.shipmentId)
                    .collection("store_inventory")
                    .add(mapOf(
                        "name" to good.name,
                        "storeLocation" to good.storeLocation,
                        "quantity" to good.quantity,
                        "createdAt" to System.currentTimeMillis()
                    ))
                    .addOnSuccessListener {
                        CoroutineScope(Dispatchers.IO).launch {
                            db.storeGoodsDao().markAsSynced(good.id)
                        }
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

    private suspend fun syncLoadingLists() {
        try {
            val unsyncedLists = db.loadingListDao().getUnsyncedLoadingLists()
            for (list in unsyncedLists) {
                firestore.collection("loading_lists")
                    .add(mapOf(
                        "name" to list.name,
                        "origin" to list.origin,
                        "destination" to list.destination,
                        "extraDetails" to list.extraDetails,
                        "status" to list.status,
                        "createdAt" to System.currentTimeMillis()
                    ))
                    .addOnSuccessListener {
                        CoroutineScope(Dispatchers.IO).launch {
                            db.loadingListDao().markAsSynced(list.id)
                        }
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

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
