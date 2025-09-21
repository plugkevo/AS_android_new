package com.example.africanshipping25

import android.app.Activity
import android.content.Context
import android.util.Log
import kotlinx.coroutines.*

class UpdateChecker(private val context: Context) {

    private val updateManager = UpdateManager(context)

    companion object {
        private const val TAG = "UpdateChecker"
        private const val PREF_NAME = "update_preferences"
        private const val KEY_LAST_UPDATE_CHECK = "last_update_check"
        private const val KEY_DISMISSED_VERSION = "dismissed_version"
        private const val UPDATE_CHECK_INTERVAL = 24 * 60 * 60 * 1000L // 24 hours
    }

    fun checkForUpdatesOnAppStart(activity: Activity) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                updateManager.
                checkForUpdates { updateInfo ->
                    updateInfo?.let {
                        if (it.isUpdateRequired) {
                            // Mandatory update - block app
                            updateManager.blockAppUntilUpdate(activity, it)
                        } else {
                            // Optional update - show if not dismissed
                            if (!wasUpdateDismissed(it.latestVersionCode)) {
                                updateManager.showUpdateDialog(activity, it)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for updates", e)
            }
        }
    }

    fun checkForUpdatesManually(activity: Activity) {
        updateManager.checkForUpdates { updateInfo ->
            if (updateInfo != null) {
                updateManager.showUpdateDialog(activity, updateInfo)
            } else {
                // Show "no updates available" message
                android.widget.Toast.makeText(
                    context,
                    "You're using the latest version!",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun shouldCheckForUpdates(): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong(KEY_LAST_UPDATE_CHECK, 0)
        val currentTime = System.currentTimeMillis()

        return (currentTime - lastCheck) > UPDATE_CHECK_INTERVAL
    }

    private fun wasUpdateDismissed(versionCode: Int): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val dismissedVersion = prefs.getInt(KEY_DISMISSED_VERSION, 0)
        return dismissedVersion >= versionCode
    }

    fun markUpdateAsDismissed(versionCode: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_DISMISSED_VERSION, versionCode)
            .putLong(KEY_LAST_UPDATE_CHECK, System.currentTimeMillis())
            .apply()
    }
}