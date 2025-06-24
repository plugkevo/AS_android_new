package com.example.africanshipping25

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
        const val PREFS_NAME = "com.example.africanshipping25.NOTIFICATIONS_PREFS"

        // Function to save a notification (called from MyFirebaseMessagingService)
        fun saveNotification(context: Context, notification: NotificationItem) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(NOTIFICATIONS_PREF_KEY, "[]")
            val type = object : TypeToken<MutableList<NotificationItem>>() {}.type
            val currentNotifications: MutableList<NotificationItem> = Gson().fromJson(json, type) ?: mutableListOf()

            currentNotifications.add(0, notification) // Add to the beginning

            // Optional: Limit the number of stored notifications
            while (currentNotifications.size > 50) { // Keep last 50 notifications
                currentNotifications.removeAt(currentNotifications.size - 1)
            }

            val editor = prefs.edit()
            editor.putString(NOTIFICATIONS_PREF_KEY, Gson().toJson(currentNotifications))
            editor.apply()

            Log.d("ViewNotifFragment", "Notification saved: ${notification.title}")

            // Notify the fragment if it's active
            // This is a simplified approach. For robust communication, consider LocalBroadcastManager
            // or ViewModel with LiveData/Flow.
            // For now, we'll rely on the fragment reloading in onResume.
        }

        // Function to load all notifications (called by the fragment)
        fun loadNotifications(context: Context): List<NotificationItem> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(NOTIFICATIONS_PREF_KEY, "[]")
            val type = object : TypeToken<MutableList<NotificationItem>>() {}.type
            val notifications: MutableList<NotificationItem> = Gson().fromJson(json, type) ?: mutableListOf()
            return notifications
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_view_notifications, container, false)

        notificationsRecyclerView = view.findViewById(R.id.notificationsRecyclerView)
        noNotificationsTextView = view.findViewById(R.id.noNotificationsTextView)

        // Initialize RecyclerView
        notificationsRecyclerView.layoutManager = LinearLayoutManager(context)
        notificationsAdapter = NotificationsAdapter(notificationList)
        notificationsRecyclerView.adapter = notificationsAdapter

        return view
    }

    override fun onResume() {
        super.onResume()
        // Load notifications every time the fragment becomes visible
        loadAndDisplayNotifications()
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