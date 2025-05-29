// com.example.africanshipping25/HomeShipmentAdapter.kt
package com.example.africanshipping25

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

// Renamed the class to HomeShipmentAdapter
class HomeShipmentAdapter(private val shipments: MutableList<Shipment>) :
    RecyclerView.Adapter<HomeShipmentAdapter.HomeShipmentViewHolder>() { // Updated ViewHolder reference

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeShipmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.shipment_list_view, parent, false)
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

        holder.itemView.setOnClickListener {
            Toast.makeText(holder.itemView.context, "Shipment Clicked: ${shipment.name}", Toast.LENGTH_SHORT).show()
        }

        holder.btnUpdate.setOnClickListener {
            Toast.makeText(holder.itemView.context, "Update clicked for: ${shipment.name}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int = shipments.size

    fun updateShipments(newShipments: List<Shipment>) {
        shipments.clear()
        shipments.addAll(newShipments)
        notifyDataSetChanged()
    }

    // Renamed the ViewHolder to HomeShipmentViewHolder
    class HomeShipmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtShipmentName: TextView = itemView.findViewById(R.id.txtShipmentName)
        val txtLocation: TextView = itemView.findViewById(R.id.txtLocation)
        val txtStatus: TextView = itemView.findViewById(R.id.txtStatus)
        val txtDate: TextView = itemView.findViewById(R.id.txtDate)
        val imgPackage: ImageView = itemView.findViewById(R.id.imgPackage)
        val btnUpdate: ImageView = itemView.findViewById(R.id.btnUpdate)
    }
}