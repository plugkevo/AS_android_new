package com.example.africanshipping25

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

interface OnShipmentUpdateListener {
    fun onUpdateShipment(shipment: Shipment)
}

class ShipmentAdapter(
    private val shipmentList: MutableList<Shipment>,
    private val updateListener: OnShipmentUpdateListener
) :
    RecyclerView.Adapter<ShipmentAdapter.ShipmentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShipmentViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.shipment_list_view, parent, false)
        return ShipmentViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ShipmentViewHolder, position: Int) {
        val currentShipment = shipmentList[position]
        holder.tvName.text = currentShipment.name
        holder.tvOriginDestination.text = "${currentShipment.origin} to ${currentShipment.destination}"
        holder.tvStatus.text = currentShipment.status ?: "Pending" // Assuming you've added status
        holder.tvDate.text = currentShipment.date ?: "N/A"     // Assuming you've added date

        holder.btnUpdate.setOnClickListener {
            updateListener.onUpdateShipment(currentShipment)
        }
    }

    override fun getItemCount(): Int {
        return shipmentList.size
    }

    class ShipmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.txtShipmentName)
        val tvOriginDestination: TextView = itemView.findViewById(R.id.txtLocation)
        val tvStatus: TextView = itemView.findViewById(R.id.txtStatus)
        val tvDate: TextView = itemView.findViewById(R.id.txtDate)
        val btnUpdate: ImageView = itemView.findViewById(R.id.btnUpdate)
    }
}