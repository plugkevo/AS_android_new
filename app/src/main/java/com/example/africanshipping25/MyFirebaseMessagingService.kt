package com.example.africanshipping25

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.app.ActivityManager // Import ActivityManager

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFirebaseMsgService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Message data payload: " + remoteMessage.data)
        Log.d(TAG, "Notification payload is null: ${remoteMessage.notification == null}") // Confirming message type

        val notificationTitle = remoteMessage.notification?.title ?: "New Update"
        val notificationBody = remoteMessage.notification?.body ?: "Check your notifications."
        val customData = remoteMessage.data

        val notificationItem = NotificationItem(
            title = notificationTitle,
            body = notificationBody,
            customData = customData
        )

        ViewNotificationsFragment.saveNotification(applicationContext, notificationItem)
        Log.d(TAG, "Notification saved to local storage and unseen count updated.")

        if (isAppInForeground(applicationContext)) {
            val broadcastIntent = Intent("new_fcm_notification").apply {
                putExtra("notification_title", notificationTitle)
                putExtra("notification_body", notificationBody)
                putExtra("custom_data_type", customData["type"])
                putExtra("custom_data_orderId", customData["orderId"])
            }
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
            Log.d(TAG, "Sent local broadcast for new notification banner.")
        }

        sendSystemNotification(notificationTitle, notificationBody, remoteMessage.data)
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {
        Log.d(TAG, "sendRegistrationToServer($token)")
    }

    private fun sendSystemNotification(title: String?, messageBody: String?, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        data.forEach { (key, value) ->
            intent.putExtra(key, value)
        }

        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    // CORRECTED isAppInForeground FUNCTION
    private fun isAppInForeground(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false

        for (processInfo in appProcesses) {
            if (processInfo.pid == android.os.Process.myPid()) {
                return processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
            }
        }
        return false
    }
}