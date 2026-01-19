package com.kevann.africanshipping25.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kevann.africanshipping25.R  // Add this import

class NotificationsAdapter(private val notifications: MutableList<NotificationItem>) :
    RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.notificationTitle)
        val body: TextView = itemView.findViewById(R.id.notificationBody)
        val customData: TextView = itemView.findViewById(R.id.notificationCustomData)
        val timestamp: TextView = itemView.findViewById(R.id.notificationTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.title.text = notification.title
        holder.body.text = notification.body
        holder.customData.text = "Custom Data: ${notification.getCustomDataString()}"
        holder.timestamp.text = notification.getFormattedTimestamp()
    }

    override fun getItemCount(): Int = notifications.size

    fun addNotification(notification: NotificationItem) {
        // Add to the beginning so newest notifications are at the top
        notifications.add(0, notification)
        notifyItemInserted(0)
    }

    fun setNotifications(newNotifications: List<NotificationItem>) {
        notifications.clear()
        notifications.addAll(newNotifications)
        notifyDataSetChanged()
    }
}