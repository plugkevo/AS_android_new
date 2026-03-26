package com.kevann.africanshipping25.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

class ConnectivityReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ConnectivityReceiver", "[v0] Broadcast received: ${intent.action}")

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val isConnected = activeNetwork != null

        Log.d("ConnectivityReceiver", "[v0] Network state changed: Connected=$isConnected")

        if (isConnected) {
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

            Log.d("ConnectivityReceiver", "[v0] Network has internet capability: $hasInternet")

            if (hasInternet) {
                Log.d("ConnectivityReceiver", "[v0] Device is online with internet, triggering sync...")
                try {
                    val syncManager = SyncManager(context)
                    Log.d("ConnectivityReceiver", "[v0] SyncManager created, calling syncAllData()")
                    syncManager.syncAllData()
                    Log.d("ConnectivityReceiver", "[v0] Sync triggered successfully")
                } catch (e: Exception) {
                    Log.e("ConnectivityReceiver", "[v0] Error triggering sync: ${e.message}", e)
                }
            }
        }
    }
}
