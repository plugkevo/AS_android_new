package com.kevann.africanshipping25.shipments

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kevann.africanshipping25.R  // Add this import


// Adapter for the RecyclerView
class StoreGoodsAdapter(private val storeGoodsList: MutableList<StoreGood>, private val onItemClick: (StoreGood) -> Unit) :  //added parameter
    RecyclerView.Adapter<StoreGoodsAdapter.StoreGoodsViewHolder>() {

    class StoreGoodsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val goodsNameTextView: TextView = itemView.findViewById(R.id.goodsNameTextView)
        val storeLocationTextView: TextView = itemView.findViewById(R.id.storeLocationTextView)
        val goodsNumberTextView: TextView = itemView.findViewById(R.id.goodsNumberTextView)
        val moreOptionsImageView: ImageView = itemView.findViewById(R.id.moreOptionsImageView) //find view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoreGoodsViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.store_goods_list_view, parent, false)
        return StoreGoodsViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: StoreGoodsViewHolder, position: Int) {
        val currentItem = storeGoodsList[position]
        Log.d("StoreGoodsAdapter", "onBindViewHolder: position = $position, currentItem = ${currentItem.name}, ${currentItem.storeLocation}, ${currentItem.goodsNumber}")

        holder.goodsNameTextView.text = "Name: ${currentItem.name}"
        holder.storeLocationTextView.text = "Location: ${currentItem.storeLocation}"
        holder.goodsNumberTextView.text = "Number: ${currentItem.goodsNumber}" // Display as Long
        holder.moreOptionsImageView.setOnClickListener {
            onItemClick(currentItem)
        }
    }


    override fun getItemCount() = storeGoodsList.size

    fun updateData(newList: List<StoreGood>) { //update
        storeGoodsList.clear()
        storeGoodsList.addAll(newList)
        notifyDataSetChanged()
    }
}