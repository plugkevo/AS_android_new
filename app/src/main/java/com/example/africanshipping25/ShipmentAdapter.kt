package com.example.africanshipping25

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class ShipmentAdapter(
    private val shipmentList: MutableList<Shipment>,
    private val updateListener: OnShipmentUpdateListener,
    private val itemClickListener: OnShipmentItemClickListener
) : RecyclerView.Adapter<ShipmentAdapter.ShipmentViewHolder>() {

    interface OnShipmentItemClickListener {
        fun onShipmentItemClick(shipment: Shipment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShipmentViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.shipment_list_view, parent, false) // Using list_item_shipment
        return ShipmentViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ShipmentViewHolder, position: Int) {
        val currentShipment = shipmentList[position]
        holder.tvName.text = currentShipment.name
        holder.tvOriginDestination.text = "${currentShipment.origin} to ${currentShipment.destination}"
        holder.tvStatus.text = currentShipment.status ?: "Pending"

        // --- UPDATED: Displaying ONLY Date from createdAt timestamp ---
        currentShipment.createdAt?.let { date ->
            // Format for date (e.g., "May 29, 2025")
            val dateFormat = SimpleDateFormat("MMM dd,yyyy", Locale.getDefault())
            holder.tvDate.text = dateFormat.format(date)
        } ?: run {
            holder.tvDate.text = "N/A" // Fallback if createdAt is null
        }
        // --- END UPDATED ---

        // Set text color based on status
        val context = holder.itemView.context
        when (currentShipment.status) {
            "Active" -> holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.orange))
            "In Transit" -> holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.blue))
            "Processing" -> holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.error))
            "Delivered" -> holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.green))
            else -> holder.tvStatus.setTextColor(Color.BLACK)
        }

        holder.btnUpdate.setOnClickListener {
            updateListener.onUpdateShipment(currentShipment)
        }

        holder.itemView.setOnClickListener {
            itemClickListener.onShipmentItemClick(currentShipment)
        }
    }

    override fun getItemCount(): Int {
        return shipmentList.size
    }

    // Method to update the data in the adapter
    fun updateShipments(newShipments: List<Shipment>) {
        shipmentList.clear()
        shipmentList.addAll(newShipments)
        notifyDataSetChanged()
    }

    class ShipmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.txtShipmentName)
        val tvOriginDestination: TextView = itemView.findViewById(R.id.txtLocation)
        val tvStatus: TextView = itemView.findViewById(R.id.txtStatus)
        val tvDate: TextView = itemView.findViewById(R.id.txtDate)
        val btnUpdate: ImageView = itemView.findViewById(R.id.btnUpdate)
        // Removed tvCreatedTime as it's no longer needed
    }
}