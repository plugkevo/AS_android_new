package com.example.africanshipping25

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*
import java.io.File
import java.text.DecimalFormat

class UpdateManager(private val context: Context) {

    private val remoteConfig = FirebaseRemoteConfig.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var downloadId: Long = -1
    private var updateDialog: AlertDialog? = null
    private var progressHandler: Handler? = null
    private var progressRunnable: Runnable? = null

    // Progress dialog views
    private var progressBar: ProgressBar? = null
    private var progressText: TextView? = null
    private var downloadSpeedText: TextView? = null
    private var timeRemainingText: TextView? = null
    private var downloadSizeText: TextView? = null

    // Progress tracking
    private var downloadStartTime: Long = 0
    private var lastDownloadedBytes: Long = 0
    private var lastUpdateTime: Long = 0

    companion object {
        private const val TAG = "UpdateManager"
        private const val REMOTE_CONFIG_CACHE_EXPIRATION = 0L
        private const val PROGRESS_UPDATE_INTERVAL = 1000L // Update every second

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
        progressHandler = Handler(Looper.getMainLooper())
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

    private fun convertFirebaseUrlToHttps(gsUrl: String, callback: (String?) -> Unit) {
        try {
            Log.d(TAG, "Converting Firebase Storage URL: $gsUrl")

            if (!gsUrl.startsWith("gs://")) {
                Log.e(TAG, "Invalid Firebase Storage URL format: $gsUrl")
                callback(null)
                return
            }

            val storageRef = storage.getReferenceFromUrl(gsUrl)
            Log.d(TAG, "Storage reference created: ${storageRef.path}")

            storageRef.downloadUrl
                .addOnSuccessListener { uri ->
                    val httpsUrl = uri.toString()
                    Log.d(TAG, "Successfully converted to HTTPS URL: $httpsUrl")
                    callback(httpsUrl)
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to get download URL from Firebase Storage", exception)
                    val alternativeUrl = convertGsUrlToHttpsDirectly(gsUrl)
                    if (alternativeUrl != null) {
                        Log.d(TAG, "Alternative URL created: $alternativeUrl")
                        callback(alternativeUrl)
                    } else {
                        callback(null)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in convertFirebaseUrlToHttps", e)
            callback(null)
        }
    }

    private fun convertGsUrlToHttpsDirectly(gsUrl: String): String? {
        return try {
            val gsUri = Uri.parse(gsUrl)
            val bucket = gsUri.authority
            val path = gsUri.path?.removePrefix("/")

            if (bucket != null && path != null) {
                val encodedPath = path.replace("/", "%2F")
                "https://firebasestorage.googleapis.com/v0/b/$bucket/o/$encodedPath?alt=media"
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in direct URL conversion", e)
            null
        }
    }

    private fun startDownload(activity: Activity, updateInfo: UpdateInfo) {
        Log.d(TAG, "Starting download process...")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!activity.packageManager.canRequestPackageInstalls()) {
                    requestInstallPermission(activity, updateInfo)
                    return
                }
            }

            val downloadUrl = updateInfo.downloadUrl.trim()
            if (downloadUrl.isEmpty()) {
                Log.e(TAG, "Download URL is empty")
                Toast.makeText(context, "Download URL not available", Toast.LENGTH_SHORT).show()
                return
            }

            Log.d(TAG, "Original Download URL: '$downloadUrl'")

            // Show progress dialog
            showDownloadProgressDialog(activity)

            when {
                downloadUrl.startsWith("gs://") -> {
                    Log.d(TAG, "Detected Firebase Storage URL, converting to HTTPS...")
                    convertFirebaseUrlToHttps(downloadUrl) { httpsUrl ->
                        if (httpsUrl != null) {
                            performDownload(httpsUrl)
                        } else {
                            updateDialog?.dismiss()
                            showUrlConversionError(activity, downloadUrl)
                        }
                    }
                }
                downloadUrl.startsWith("http://") || downloadUrl.startsWith("https://") -> {
                    performDownload(downloadUrl)
                }
                else -> {
                    updateDialog?.dismiss()
                    Toast.makeText(context, "Error: Invalid download URL format", Toast.LENGTH_LONG).show()
                }
            }

        } catch (e: Exception) {
            updateDialog?.dismiss()
            Log.e(TAG, "Error starting download", e)
            Toast.makeText(context, "Error starting download: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showDownloadProgressDialog(activity: Activity) {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_download_progress, null)

        progressBar = dialogView.findViewById(R.id.progressBar)
        progressText = dialogView.findViewById(R.id.progressText)
        downloadSpeedText = dialogView.findViewById(R.id.downloadSpeedText)
        timeRemainingText = dialogView.findViewById(R.id.timeRemainingText)
        downloadSizeText = dialogView.findViewById(R.id.downloadSizeText)

        updateDialog = AlertDialog.Builder(activity)
            .setTitle("Downloading Update")
            .setView(dialogView)
            .setCancelable(false)
            .create()

        updateDialog?.show()
    }

    private fun performDownload(httpsUrl: String) {
        try {
            Log.d(TAG, "Starting download with HTTPS URL: $httpsUrl")

            val request = DownloadManager.Request(Uri.parse(httpsUrl))
                .setTitle("African Shipping Update")
                .setDescription("Downloading latest version...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, APK_FILE_NAME)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = downloadManager.enqueue(request)
            downloadStartTime = System.currentTimeMillis()
            lastUpdateTime = downloadStartTime

            Log.d(TAG, "Download started with ID: $downloadId")

            // Start progress monitoring
            startProgressMonitoring()

            // Register receiver to handle download completion
            registerDownloadReceiver(context as Activity)

        } catch (e: Exception) {
            updateDialog?.dismiss()
            Log.e(TAG, "Error performing download", e)
            Toast.makeText(context, "Error starting download: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startProgressMonitoring() {
        progressRunnable = object : Runnable {
            override fun run() {
                updateDownloadProgress()
                progressHandler?.postDelayed(this, PROGRESS_UPDATE_INTERVAL)
            }
        }
        progressHandler?.post(progressRunnable!!)
    }

    private fun stopProgressMonitoring() {
        progressRunnable?.let { progressHandler?.removeCallbacks(it) }
    }

    private fun updateDownloadProgress() {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)

        try {
            val cursor: Cursor = downloadManager.query(query)
            if (cursor.moveToFirst()) {
                val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val bytesTotal = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))

                if (bytesTotal > 0) {
                    val progress = ((bytesDownloaded * 100L) / bytesTotal).toInt()
                    val currentTime = System.currentTimeMillis()

                    // Update progress bar and percentage
                    progressBar?.progress = progress
                    progressText?.text = "$progress%"

                    // Calculate download speed
                    val timeDiff = currentTime - lastUpdateTime
                    if (timeDiff > 0) {
                        val bytesDiff = bytesDownloaded - lastDownloadedBytes
                        val speedBytesPerSecond = (bytesDiff * 1000) / timeDiff
                        val speedText = formatSpeed(speedBytesPerSecond)
                        downloadSpeedText?.text = "Speed: $speedText"

                        // Calculate time remaining
                        if (speedBytesPerSecond > 0) {
                            val remainingBytes = bytesTotal - bytesDownloaded
                            val timeRemainingSeconds = remainingBytes / speedBytesPerSecond
                            val timeRemainingText = formatTime(timeRemainingSeconds)
                            this.timeRemainingText?.text = "Time remaining: $timeRemainingText"
                        }

                        lastDownloadedBytes = bytesDownloaded
                        lastUpdateTime = currentTime
                    }

                    // Update download size info
                    val downloadedText = formatFileSize(bytesDownloaded)
                    val totalText = formatFileSize(bytesTotal)
                    downloadSizeText?.text = "$downloadedText / $totalText"

                    Log.d(TAG, "Download progress: $progress% ($bytesDownloaded/$bytesTotal bytes)")
                }

                // Check if download is complete or failed
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        Log.d(TAG, "Download completed successfully")
                        stopProgressMonitoring()
                    }
                    DownloadManager.STATUS_FAILED -> {
                        Log.e(TAG, "Download failed")
                        stopProgressMonitoring()
                        updateDialog?.dismiss()
                        Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating download progress", e)
        }
    }

    private fun formatSpeed(bytesPerSecond: Long): String {
        val df = DecimalFormat("#.##")
        return when {
            bytesPerSecond >= 1024 * 1024 -> "${df.format(bytesPerSecond / (1024.0 * 1024.0))} MB/s"
            bytesPerSecond >= 1024 -> "${df.format(bytesPerSecond / 1024.0)} KB/s"
            else -> "$bytesPerSecond B/s"
        }
    }

    private fun formatFileSize(bytes: Long): String {
        val df = DecimalFormat("#.##")
        return when {
            bytes >= 1024 * 1024 -> "${df.format(bytes / (1024.0 * 1024.0))} MB"
            bytes >= 1024 -> "${df.format(bytes / 1024.0)} KB"
            else -> "$bytes B"
        }
    }

    private fun formatTime(seconds: Long): String {
        return when {
            seconds >= 3600 -> {
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                "${hours}h ${minutes}m"
            }
            seconds >= 60 -> {
                val minutes = seconds / 60
                val secs = seconds % 60
                "${minutes}m ${secs}s"
            }
            else -> "${seconds}s"
        }
    }

    private fun showUrlConversionError(activity: Activity, originalUrl: String) {
        AlertDialog.Builder(activity)
            .setTitle("Download Error")
            .setMessage("""
                Unable to get download URL from Firebase Storage.
                
                This could be due to:
                • Firebase Storage security rules
                • File doesn't exist at the specified location
                • Network connectivity issues
                
                Original URL: $originalUrl
            """.trimIndent())
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("Retry") { _, _ ->
                val updateInfo = UpdateInfo(0, "", false, "", originalUrl, "")
                startDownload(activity, updateInfo)
            }
            .show()
    }

    private fun registerDownloadReceiver(activity: Activity) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    Log.d(TAG, "Download completed")
                    stopProgressMonitoring()
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