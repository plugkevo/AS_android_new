// LoadingListAdapter.kt
package com.kevann.africanshipping25.loadinglists

import com.kevann.africanshipping25.R  // Add this import

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.kevann.africanshipping25.loadinglists.LoadingListItem

// Interface for handling item clicks and more options clicks
interface OnLoadingListItemClickListener {
    fun onLoadingListItemClick(loadingList: LoadingListItem)
    fun onMoreOptionsClick(loadingList: LoadingListItem, anchorView: View)
}

class LoadingListAdapter(
    private val loadingLists: MutableList<LoadingListItem>,
    private val itemClickListener: OnLoadingListItemClickListener // Pass the listener here
) : RecyclerView.Adapter<LoadingListAdapter.LoadingListViewHolder>() {

    // ViewHolder class to hold references to the views for each item
    class LoadingListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Reuse IDs from shipment_item.xml if you're using it for loading list items
        val tvLoadingListName: TextView = itemView.findViewById(R.id.tv_shipment_name)
        val tvOrigin: TextView = itemView.findViewById(R.id.tv_origin)
        val tvDestination: TextView = itemView.findViewById(R.id.tv_destination)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        val btnMore: ImageButton = itemView.findViewById(R.id.btn_more) // The three dots button
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoadingListViewHolder {
        // Inflate the layout for each loading list item
        // IMPORTANT: Ensure R.layout.shipment_item is the correct XML for your loading list item visuals
        // If you have a dedicated layout for loading list items (e.g., loading_list_item.xml), use that instead.
        val view = LayoutInflater.from(parent.context).inflate(R.layout.loading_list_view, parent, false)
        return LoadingListViewHolder(view)
    }

    override fun onBindViewHolder(holder: LoadingListViewHolder, position: Int) {
        val loadingList = loadingLists[position]
        holder.tvLoadingListName.text = loadingList.name
        holder.tvOrigin.text = loadingList.origin
        holder.tvDestination.text = loadingList.destination
        holder.tvStatus.text = loadingList.status

        // Example of status coloring (you can expand this logic or use a helper)
        when (loadingList.status) {
            "New" -> holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.colorAccent)) // Use your custom color
            "Open" -> holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.holo_green_light))
            "Closed" -> holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.holo_red_dark))
            else -> holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.darker_gray))
        }

        // Set click listener for the entire item view
        holder.itemView.setOnClickListener {
            itemClickListener.onLoadingListItemClick(loadingList)
        }

        // Set click listener for the "more options" button
        holder.btnMore.setOnClickListener {
            itemClickListener.onMoreOptionsClick(loadingList, it)
        }
    }

    override fun getItemCount(): Int = loadingLists.size

    // Helper function to update the data and refresh the RecyclerView
    fun updateData(newList: List<LoadingListItem>) {
        loadingLists.clear()
        loadingLists.addAll(newList)
        notifyDataSetChanged()
    }
}