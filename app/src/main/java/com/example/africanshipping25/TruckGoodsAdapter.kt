package com.example.africanshipping25

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TruckGoodsAdapter(
    private var truckGoodsList: List<TruckGood>,
    private val onItemClick: (TruckGood) -> Unit // Add click listener parameter
) :
    RecyclerView.Adapter<TruckGoodsAdapter.TruckGoodsViewHolder>() {

    // ViewHolder class to hold the views for each item
    class TruckGoodsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val goodsNumberTextView: TextView = itemView.findViewById(R.id.goodsNumberTextView)
        val moreOptionsImageView: ImageView = itemView.findViewById(R.id.moreOptionsImageView)
        // You can add more views here if your item layout has more elements
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TruckGoodsViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.truck_goods_list_view, parent, false) // Use your item layout
        return TruckGoodsViewHolder(itemView)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: TruckGoodsViewHolder, position: Int) {
        val currentItem = truckGoodsList[position]
        holder.goodsNumberTextView.text = currentItem.goodsNumber
        holder.moreOptionsImageView.setOnClickListener {
            onItemClick(currentItem) // Pass the clicked item to the listener
        }
        // Bind other data to views here
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = truckGoodsList.size

    // Helper method to update the data in the adapter
    fun updateData(newTruckGoodsList: List<TruckGood>) {
        truckGoodsList = newTruckGoodsList
        notifyDataSetChanged() // Notify the adapter that the data has changed
    }
}
