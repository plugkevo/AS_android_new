package com.kevann.africanshipping25.shipments

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kevann.africanshipping25.R
import com.kevann.africanshipping25.translation.GoogleTranslationHelper
import java.text.SimpleDateFormat
import java.util.*

class CheckpointAdapter(
    private var checkpointList: List<Checkpoint>,
    private val onItemClick: (Checkpoint) -> Unit = {},
    private val translationHelper: GoogleTranslationHelper? = null,
    private val context: Context? = null
) : RecyclerView.Adapter<CheckpointAdapter.CheckpointViewHolder>() {

    class CheckpointViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCheckpointLocation: TextView = itemView.findViewById(R.id.tvCheckpointLocation)
        val tvCheckpointStatus: TextView = itemView.findViewById(R.id.tvCheckpointStatus)
        val tvCheckpointCoordinates: TextView = itemView.findViewById(R.id.tvCheckpointCoordinates)
        val tvCheckpointTimestamp: TextView = itemView.findViewById(R.id.tvCheckpointTimestamp)
        val tvCheckpointNotes: TextView = itemView.findViewById(R.id.tvCheckpointNotes)
        val viewTimeline: View = itemView.findViewById(R.id.viewTimeline)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckpointViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_checkpoint, parent, false)
        return CheckpointViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CheckpointViewHolder, position: Int) {
        val currentCheckpoint = checkpointList[position]
        val sharedPreferences = context?.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentLanguage = sharedPreferences?.getString("language", "English") ?: "English"

        holder.tvCheckpointLocation.text = currentCheckpoint.locationName

        // Translate status
        if (translationHelper != null) {
            translationHelper.translateText(currentCheckpoint.status, currentLanguage) { translatedStatus ->
                holder.tvCheckpointStatus.text = translatedStatus
            }
        } else {
            holder.tvCheckpointStatus.text = currentCheckpoint.status
        }

        holder.tvCheckpointCoordinates.text = "Lat: ${String.format("%.4f", currentCheckpoint.latitude)}, Lng: ${String.format("%.4f", currentCheckpoint.longitude)}"

        // Format timestamp
        currentCheckpoint.timestamp?.let { date ->
            val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            holder.tvCheckpointTimestamp.text = formatter.format(date)
        } ?: run {
            holder.tvCheckpointTimestamp.text = "N/A"
        }

        // Show notes only if they exist
        if (currentCheckpoint.notes.isNotEmpty()) {
            holder.tvCheckpointNotes.text = currentCheckpoint.notes
            holder.tvCheckpointNotes.visibility = View.VISIBLE
        } else {
            holder.tvCheckpointNotes.visibility = View.GONE
        }

        // Hide timeline for last item
        if (position == checkpointList.size - 1) {
            holder.viewTimeline.visibility = View.GONE
        } else {
            holder.viewTimeline.visibility = View.VISIBLE
        }

        holder.itemView.setOnClickListener {
            onItemClick(currentCheckpoint)
        }
    }

    override fun getItemCount() = checkpointList.size

    fun updateData(newCheckpointList: List<Checkpoint>) {
        checkpointList = newCheckpointList
        notifyDataSetChanged()
    }
}
