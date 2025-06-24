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
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.concurrent.TimeUnit

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFirebaseMsgService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)

            // Extract relevant notification details
            val notificationTitle = remoteMessage.notification?.title ?: "New Message"
            val notificationBody = remoteMessage.notification?.body ?: "You have a new update."
            val customData = remoteMessage.data

            // 1. Create a NotificationItem from the received message
            val notificationItem = NotificationItem(
                title = notificationTitle,
                body = notificationBody,
                customData = customData
                // ID and timestamp are auto-generated in data class
            )

            // 2. Save the notification locally
            ViewNotificationsFragment.saveNotification(applicationContext, notificationItem)
            Log.d(TAG, "Notification saved to local storage.")


            // 3. (Optional) Display a system notification if desired
            // You might choose NOT to show a system notification if the app is in foreground
            // and you're updating the UI directly. If the app is in background/killed,
            // a system notification is usually what you want.

            // The logic below ensures a system notification is shown in all cases
            // based on the Firebase Console payload. If you only send data payload
            // from console, you'll need to construct the title/body here.
            remoteMessage.notification?.let { notification ->
                Log.d(TAG, "Message Notification Body: ${notification.body}")
                sendSystemNotification(notification.title, notification.body, remoteMessage.data)
            } ?: run {
                // If there's NO notification payload (only data payload from console),
                // you can still show a system notification based on the custom data
                sendSystemNotification(notificationTitle, notificationBody, remoteMessage.data)
            }
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {
        Log.d(TAG, "sendRegistrationToServer($token)")
        // Implement logic to send this token to your backend if you manage users
    }

    private fun sendSystemNotification(title: String?, messageBody: String?, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java) // Your main activity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        // Add custom data to the intent for when the user taps the system notification
        data.forEach { (key, value) ->
            intent.putExtra(key, value)
        }

        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.as_logo) // Ensure this icon exists
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                "General Notifications", // User-visible channel name
                NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt() /* unique ID for each notification */, notificationBuilder.build())
    }
}