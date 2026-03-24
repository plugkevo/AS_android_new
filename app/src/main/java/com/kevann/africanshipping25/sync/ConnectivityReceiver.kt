package com.kevann.africanshipping25.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log

class ConnectivityReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val isConnected = activeNetwork != null

        Log.d("ConnectivityReceiver", "Network state changed: Connected=$isConnected")

        if (isConnected) {
            // Trigger sync when device comes online
            Log.d("ConnectivityReceiver", "Device is online, triggering sync...")
            SyncManager(context).syncAllData()
        }
    }
}
