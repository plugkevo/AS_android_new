package com.example.africanshipping25

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.*
import java.io.File

class UpdateManager(private val context: Context) {

    private val remoteConfig = FirebaseRemoteConfig.getInstance()
    private val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
    private var downloadId: Long = -1
    private var updateDialog: AlertDialog? = null

    companion object {
        private const val TAG = "UpdateManager"
        private const val REMOTE_CONFIG_CACHE_EXPIRATION = 0L

        // Remote Config Keys
        private const val KEY_LATEST_VERSION_CODE = "latest_version_code"
        private const val KEY_LATEST_VERSION_NAME = "latest_version_name"
        private const val KEY_UPDATE_REQUIRED = "update_required"
        private const val KEY_UPDATE_MESSAGE = "update_message"
        private const val KEY_APK_DOWNLOAD_URL = "apk_download_url"
        private const val KEY_FEATURES_LIST = "new_features_list"

        // File paths
        private const val APK_FILE_NAME = "african_shipping_update.apk"
    }

    init {
        setupRemoteConfig()
    }

    private fun setupRemoteConfig() {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(REMOTE_CONFIG_CACHE_EXPIRATION)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)

        // Set default values
        val defaults = mapOf(
            KEY_LATEST_VERSION_CODE to getCurrentVersionCode(),
            KEY_LATEST_VERSION_NAME to getCurrentVersionName(),
            KEY_UPDATE_REQUIRED to false,
            KEY_UPDATE_MESSAGE to "A new update is available with exciting features!",
            KEY_APK_DOWNLOAD_URL to "",
            KEY_FEATURES_LIST to "• Bug fixes and improvements"
        )
        remoteConfig.setDefaultsAsync(defaults)
    }

    fun checkForUpdates(callback: (UpdateInfo?) -> Unit) {
        Log.d(TAG, "Checking for updates...")
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val latestVersionCode = remoteConfig.getLong(KEY_LATEST_VERSION_CODE).toInt()
                    val currentVersionCode = getCurrentVersionCode()

                    Log.d(TAG, "Current version: $currentVersionCode, Latest version: $latestVersionCode")

                    if (latestVersionCode > currentVersionCode) {
                        val updateInfo = UpdateInfo(
                            latestVersionCode = latestVersionCode,
                            latestVersionName = remoteConfig.getString(KEY_LATEST_VERSION_NAME),
                            isUpdateRequired = remoteConfig.getBoolean(KEY_UPDATE_REQUIRED),
                            updateMessage = remoteConfig.getString(KEY_UPDATE_MESSAGE),
                            downloadUrl = remoteConfig.getString(KEY_APK_DOWNLOAD_URL),
                            featuresList = remoteConfig.getString(KEY_FEATURES_LIST)
                        )
                        Log.d(TAG, "Update available: ${updateInfo.latestVersionName}")
                        callback(updateInfo)
                    } else {
                        Log.d(TAG, "No update available")
                        callback(null)
                    }
                } else {
                    Log.e(TAG, "Failed to fetch remote config", task.exception)
                    callback(null)
                }
            }
    }

    fun showUpdateDialog(activity: Activity, updateInfo: UpdateInfo) {
        val dialogBuilder = AlertDialog.Builder(activity)
            .setTitle("Update Available")
            .setMessage(buildUpdateMessage(updateInfo))
            .setCancelable(!updateInfo.isUpdateRequired)

        if (updateInfo.isUpdateRequired) {
            dialogBuilder.setPositiveButton("Update Now") { _, _ ->
                startDownload(activity, updateInfo)
            }
        } else {
            dialogBuilder.setPositiveButton("Update") { _, _ ->
                startDownload(activity, updateInfo)
            }
            dialogBuilder.setNegativeButton("Later") { dialog, _ ->
                dialog.dismiss()
            }
        }

        updateDialog = dialogBuilder.create()
        updateDialog?.show()
    }

    private fun buildUpdateMessage(updateInfo: UpdateInfo): String {
        return """
            New version ${updateInfo.latestVersionName} is available!
            
            ${updateInfo.updateMessage}
            
            What's New:
            ${updateInfo.featuresList}
            
            ${if (updateInfo.isUpdateRequired) "\n⚠️ This update is required to continue using the app." else ""}
        """.trimIndent()
    }

    private fun startDownload(activity: Activity, updateInfo: UpdateInfo) {
        Log.d(TAG, "Starting download process...")

        try {
            // Check if we need to request install permission for Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "Checking install permission for Android 8.0+")
                if (!activity.packageManager.canRequestPackageInstalls()) {
                    Log.d(TAG, "Install permission not granted, requesting...")
                    requestInstallPermission(activity, updateInfo)
                    return
                }
                Log.d(TAG, "Install permission already granted")
            }

            val downloadUrl = updateInfo.downloadUrl
            if (downloadUrl.isEmpty()) {
                Log.e(TAG, "Download URL is empty")
                Toast.makeText(context, "Download URL not available", Toast.LENGTH_SHORT).show()
                return
            }

            Log.d(TAG, "Download URL: $downloadUrl")

            // Show download progress dialog
            showDownloadProgressDialog(activity)

            // Start download using DownloadManager
            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle("African Shipping Update")
                .setDescription("Downloading latest version...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, APK_FILE_NAME)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = downloadManager.enqueue(request)

            Log.d(TAG, "Download started with ID: $downloadId")

            // Register receiver to handle download completion
            registerDownloadReceiver(activity)

        } catch (e: Exception) {
            Log.e(TAG, "Error starting download", e)
            Toast.makeText(context, "Error starting download: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showDownloadProgressDialog(activity: Activity) {
        updateDialog = AlertDialog.Builder(activity)
            .setTitle("Downloading Update")
            .setMessage("Please wait while the update is being downloaded...")
            .setCancelable(false)
            .create()

        updateDialog?.show()
    }

    private fun registerDownloadReceiver(activity: Activity) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    Log.d(TAG, "Download completed")
                    updateDialog?.dismiss()
                    installApk(activity)
                    context?.unregisterReceiver(this)
                }
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
    }

    private fun installApk(activity: Activity) {
        try {
            val apkFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), APK_FILE_NAME)

            Log.d(TAG, "APK file path: ${apkFile.absolutePath}")
            Log.d(TAG, "APK file exists: ${apkFile.exists()}")

            if (!apkFile.exists()) {
                Toast.makeText(context, "Downloaded file not found", Toast.LENGTH_SHORT).show()
                return
            }

            val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
            } else {
                Uri.fromFile(apkFile)
            }

            Log.d(TAG, "APK URI: $apkUri")

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            activity.startActivity(installIntent)

        } catch (e: Exception) {
            Log.e(TAG, "Error installing APK", e)
            Toast.makeText(context, "Error installing update: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestInstallPermission(activity: Activity, updateInfo: UpdateInfo) {
        AlertDialog.Builder(activity)
            .setTitle("Permission Required")
            .setMessage("To install updates, please allow installation from unknown sources.")
            .setPositiveButton("Settings") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData(Uri.parse("package:${context.packageName}"))
                    activity.startActivityForResult(intent, 1001)
                } catch (e: Exception) {
                    Log.e(TAG, "Error opening settings", e)
                    Toast.makeText(context, "Please enable 'Install unknown apps' in Settings", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                if (updateInfo.isUpdateRequired) {
                    // If update is mandatory, show the dialog again
                    showUpdateDialog(activity, updateInfo)
                }
            }
            .show()
    }

    private fun getCurrentVersionCode(): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting version code", e)
            1
        }
    }

    private fun getCurrentVersionName(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting version name", e)
            "1.0"
        }
    }

    fun blockAppUntilUpdate(activity: Activity, updateInfo: UpdateInfo) {
        AlertDialog.Builder(activity)
            .setTitle("Update Required")
            .setMessage("This version is no longer supported. Please update to continue using African Shipping.")
            .setPositiveButton("Update Now") { _, _ ->
                startDownload(activity, updateInfo)
            }
            .setCancelable(false)
            .show()
    }
}

data class UpdateInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val isUpdateRequired: Boolean,
    val updateMessage: String,
    val downloadUrl: String,
    val featuresList: String
)