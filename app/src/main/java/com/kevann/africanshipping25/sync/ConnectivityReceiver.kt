package com.kevann.africanshipping25.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.util.Log

class ConnectivityReceiver(private val onConnectivityChange: (Boolean) -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val isConnected = activeNetwork != null

        Log.d("ConnectivityReceiver", "Network state changed: Connected=$isConnected")
        onConnectivityChange(isConnected)

        if (isConnected) {
            // Trigger sync when device comes online
            SyncManager(context).syncAllData()
        }
    }

    fun register(context: Context) {
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        context.registerReceiver(this, filter, Context.RECEIVER_EXPORTED)
    }

    fun unregister(context: Context) {
        try {
            context.unregisterReceiver(this)
        } catch (e: Exception) {
            Log.e("ConnectivityReceiver", "Error unregistering receiver: ${e.message}")
        }
    }
}
