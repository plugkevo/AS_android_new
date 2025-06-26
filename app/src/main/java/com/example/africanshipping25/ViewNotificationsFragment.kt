package com.example.africanshipping25

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager // Import this
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Collections

class ViewNotificationsFragment : Fragment() {

    private lateinit var notificationsRecyclerView: RecyclerView
    private lateinit var noNotificationsTextView: TextView
    private lateinit var notificationsAdapter: NotificationsAdapter
    private val notificationList = mutableListOf<NotificationItem>()

    companion object {
        const val NOTIFICATIONS_PREF_KEY = "app_notifications"
        const val UNSEEN_COUNT_PREF_KEY = "unseen_notifications_count" // New key for unseen count
        const val PREFS_NAME = "com.example.africanshipping25.NOTIFICATIONS_PREFS"
        const val ACTION_UNSEEN_COUNT_UPDATED = "com.example.africanshipping25.UNSEEN_COUNT_UPDATED" // New broadcast action

        // Function to save a notification (called from MyFirebaseMessagingService)
        fun saveNotification(context: Context, notification: NotificationItem) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(NOTIFICATIONS_PREF_KEY, "[]")
            val type = object : TypeToken<MutableList<NotificationItem>>() {}.type
            val currentNotifications: MutableList<NotificationItem> = Gson().fromJson(json, type) ?: mutableListOf()

            // Check if this notification already exists (e.g., to prevent duplicates from multiple calls/tests)
            // You might use a more robust ID system if timestamps aren't unique enough for your needs
            if (!currentNotifications.any { it.id == notification.id }) {
                currentNotifications.add(0, notification) // Add to the beginning

                // Optional: Limit the number of stored notifications
                while (currentNotifications.size > 50) { // Keep last 50 notifications
                    currentNotifications.removeAt(currentNotifications.size - 1)
                }

                val editor = prefs.edit()
                editor.putString(NOTIFICATIONS_PREF_KEY, Gson().toJson(currentNotifications))
                editor.apply()

                Log.d("ViewNotifFragment", "Notification saved: ${notification.title}")

                // Increment unseen count if the new notification is indeed unseen
                if (!notification.isSeen) {
                    incrementUnseenCount(context)
                }
            } else {
                Log.d("ViewNotifFragment", "Notification with ID ${notification.id} already exists. Not saving duplicate.")
            }
        }

        // Function to load all notifications (called by the fragment)
        fun loadNotifications(context: Context): List<NotificationItem> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(NOTIFICATIONS_PREF_KEY, "[]")
            val type = object : TypeToken<MutableList<NotificationItem>>() {}.type
            val notifications: MutableList<NotificationItem> = Gson().fromJson(json, type) ?: mutableListOf()
            return notifications
        }

        // --- NEW METHODS FOR UNSEEN COUNT MANAGEMENT ---
        private fun incrementUnseenCount(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentCount = prefs.getInt(UNSEEN_COUNT_PREF_KEY, 0)
            prefs.edit().putInt(UNSEEN_COUNT_PREF_KEY, currentCount + 1).apply()
            Log.d("ViewNotifFragment", "Unseen count incremented to ${currentCount + 1}")
            // Notify MainActivity to update the badge
            LocalBroadcastManager.getInstance(context)
                .sendBroadcast(Intent(ACTION_UNSEEN_COUNT_UPDATED))
        }

        fun getUnseenCount(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(UNSEEN_COUNT_PREF_KEY, 0)
        }

        fun resetUnseenCount(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putInt(UNSEEN_COUNT_PREF_KEY, 0)
            editor.apply()
            Log.d("ViewNotifFragment", "Unseen count reset to 0.")
            // Notify MainActivity to update the badge
            LocalBroadcastManager.getInstance(context)
                .sendBroadcast(Intent(ACTION_UNSEEN_COUNT_UPDATED))
        }

        // Method to mark all current notifications as seen
        fun markAllAsSeen(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(NOTIFICATIONS_PREF_KEY, "[]")
            val type = object : TypeToken<MutableList<NotificationItem>>() {}.type
            val currentNotifications: MutableList<NotificationItem> = Gson().fromJson(json, type) ?: mutableListOf()

            var changed = false
            currentNotifications.forEach {
                if (!it.isSeen) {
                    it.isSeen = true
                    changed = true
                }
            }

            if (changed) {
                val editor = prefs.edit()
                editor.putString(NOTIFICATIONS_PREF_KEY, Gson().toJson(currentNotifications))
                editor.apply()
                Log.d("ViewNotifFragment", "All notifications marked as seen in storage.")
            }
        }
        // --- END NEW METHODS ---
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_view_notifications, container, false)

        notificationsRecyclerView = view.findViewById(R.id.notificationsRecyclerView)
        noNotificationsTextView = view.findViewById(R.id.noNotificationsTextView)

        notificationsRecyclerView.layoutManager = LinearLayoutManager(context)
        notificationsAdapter = NotificationsAdapter(notificationList)
        notificationsRecyclerView.adapter = notificationsAdapter

        return view
    }

    override fun onResume() {
        super.onResume()
        // Load notifications every time the fragment becomes visible
        loadAndDisplayNotifications()

        // When the notification list fragment becomes visible,
        // it implies the user is checking notifications.
        // So, mark all as seen and reset the unseen count.
        context?.let {
            Companion.markAllAsSeen(it)
            Companion.resetUnseenCount(it)
        }
    }

    private fun loadAndDisplayNotifications() {
        val loadedNotifications = Companion.loadNotifications(requireContext())
        notificationList.clear()
        notificationList.addAll(loadedNotifications)
        notificationsAdapter.notifyDataSetChanged()

        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (notificationList.isEmpty()) {
            notificationsRecyclerView.visibility = View.GONE
            noNotificationsTextView.visibility = View.VISIBLE
        } else {
            notificationsRecyclerView.visibility = View.VISIBLE
            noNotificationsTextView.visibility = View.GONE
        }
    }
}