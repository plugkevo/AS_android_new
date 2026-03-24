package com.kevann.africanshipping25.ais

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kevann.africanshipping25.R
import java.text.SimpleDateFormat
import java.util.*

class ShipsAdapter(
    private val ships: List<Ship>,
    private val onActionClick: (Ship, String) -> Unit
) : RecyclerView.Adapter<ShipsAdapter.ShipViewHolder>() {

    inner class ShipViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvShipName = itemView.findViewById<TextView>(R.id.tv_ship_name)
        private val tvShipNumber = itemView.findViewById<TextView>(R.id.tv_ship_number)
        private val tvIMONumber = itemView.findViewById<TextView>(R.id.tv_imo_number)
        private val tvLocation = itemView.findViewById<TextView>(R.id.tv_location)
        private val tvSpeed = itemView.findViewById<TextView>(R.id.tv_speed)
        private val tvLastUpdate = itemView.findViewById<TextView>(R.id.tv_last_update)
        private val btnEdit = itemView.findViewById<Button>(R.id.btn_edit)
        private val btnDelete = itemView.findViewById<Button>(R.id.btn_delete)
        private val btnRefresh = itemView.findViewById<Button>(R.id.btn_refresh)

        fun bind(ship: Ship) {
            tvShipName.text = "Name: ${ship.name}"
            tvShipNumber.text = "Number: ${ship.number}"
            tvIMONumber.text = "IMO: ${ship.imoNumber}"
            tvLocation.text = "Location: ${String.format("%.4f", ship.currentLatitude)}, ${String.format("%.4f", ship.currentLongitude)}"
            tvSpeed.text = "Speed: ${String.format("%.2f", ship.speed)} knots"

            // Format last update time
            if (ship.lastLocationUpdate > 0) {
                val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                val lastUpdateDate = Date(ship.lastLocationUpdate)
                tvLastUpdate.text = "Updated: ${dateFormat.format(lastUpdateDate)}"
            } else {
                tvLastUpdate.text = "Updated: Never"
            }

            btnEdit.setOnClickListener { onActionClick(ship, "edit") }
            btnDelete.setOnClickListener { onActionClick(ship, "delete") }
            btnRefresh.setOnClickListener { onActionClick(ship, "refresh") }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShipViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ship, parent, false)
        return ShipViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShipViewHolder, position: Int) {
        holder.bind(ships[position])
    }

    override fun getItemCount(): Int = ships.size
}
