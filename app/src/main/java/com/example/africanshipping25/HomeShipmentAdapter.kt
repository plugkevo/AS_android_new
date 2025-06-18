// com.example.africanshipping25/HomeShipmentAdapter.kt
package com.example.africanshipping25

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

// Updated constructor to accept OnShipmentItemClickListener
class HomeShipmentAdapter(
    private val shipments: MutableList<Shipment>,
    private val itemClickListener: ShipmentAdapter.OnShipmentItemClickListener // Added listener
) : RecyclerView.Adapter<HomeShipmentAdapter.HomeShipmentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeShipmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.shipment_list_view, parent, false) // Using shipment_list_view
        return HomeShipmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: HomeShipmentViewHolder, position: Int) {
        val shipment = shipments[position]
        holder.txtShipmentName.text = shipment.name
        holder.txtLocation.text = "${shipment.origin} to ${shipment.destination}"
        holder.txtStatus.text = shipment.status

        shipment.createdAt?.let {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            holder.txtDate.text = dateFormat.format(it)
        } ?: run {
            holder.txtDate.text = "N/A"
        }

        val context = holder.itemView.context
        when (shipment.status) {
            "Active" -> holder.txtStatus.setTextColor(context.getColor(R.color.orange))
            "In Transit" -> holder.txtStatus.setTextColor(context.getColor(R.color.blue))
            "Processing" -> holder.txtStatus.setTextColor(context.getColor(R.color.error))
            "Delivered" -> holder.txtStatus.setTextColor(context.getColor(R.color.green))
            else -> holder.txtStatus.setTextColor(context.getColor(android.R.color.black))
        }

        // Call the itemClickListener's method when the item is clicked
        holder.itemView.setOnClickListener {
            itemClickListener.onShipmentItemClick(shipment)
        }

        // Removed the btnUpdate listener as per the goal of only opening details here.
        // If you need update functionality on the home screen, you'll need a separate listener for it.
        // holder.btnUpdate.setOnClickListener {
        //     Toast.makeText(holder.itemView.context, "Update clicked for: ${shipment.name}", Toast.LENGTH_SHORT).show()
        // }
    }

    override fun getItemCount(): Int = shipments.size

    fun updateShipments(newShipments: List<Shipment>) {
        shipments.clear()
        shipments.addAll(newShipments)
        notifyDataSetChanged()
    }

    class HomeShipmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtShipmentName: TextView = itemView.findViewById(R.id.txtShipmentName)
        val txtLocation: TextView = itemView.findViewById(R.id.txtLocation)
        val txtStatus: TextView = itemView.findViewById(R.id.txtStatus)
        val txtDate: TextView = itemView.findViewById(R.id.txtDate)
        val imgPackage: ImageView = itemView.findViewById(R.id.imgPackage)
        val btnUpdate: ImageView = itemView.findViewById(R.id.btnUpdate) // Keep this if your layout has it, even if not used for clicks
    }
}