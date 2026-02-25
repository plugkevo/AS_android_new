package com.kevann.africanshipping25.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kevann.africanshipping25.R

class GlobalSearchResultsAdapter(
    private val results: MutableList<SearchResultItem>,
    private val onItemClick: (SearchResultItem) -> Unit
) : RecyclerView.Adapter<GlobalSearchResultsAdapter.SearchResultViewHolder>() {

    class SearchResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val goodsNameTextView: TextView = itemView.findViewById(R.id.goods_name)
        private val shipmentNameTextView: TextView = itemView.findViewById(R.id.shipment_name)
        private val locationTextView: TextView = itemView.findViewById(R.id.goods_location)
        private val categoryBadge: TextView = itemView.findViewById(R.id.category_badge)
        private val goodsNumberTextView: TextView = itemView.findViewById(R.id.goods_number)

        fun bind(item: SearchResultItem, onItemClick: (SearchResultItem) -> Unit) {
            goodsNameTextView.text = item.name ?: "Unknown"
            shipmentNameTextView.text = item.shipmentName
            goodsNumberTextView.text = "#${item.goodsNumber ?: "N/A"}"

            // Display location based on category
            val location = if (item.category == "store") {
                item.storeLocation ?: "Unknown"
            } else {
                item.truckLocation ?: "Unknown"
            }
            locationTextView.text = location

            // Set category badge
            categoryBadge.apply {
                text = if (item.category == "store") "STORE" else "TRUCK"
                setBackgroundResource(
                    if (item.category == "store") R.color.category_store else R.color.category_truck
                )
            }

            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_global_search_result, parent, false)
        return SearchResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        holder.bind(results[position], onItemClick)
    }

    override fun getItemCount() = results.size

    fun updateResults(newResults: List<SearchResultItem>) {
        results.clear()
        results.addAll(newResults)
        notifyDataSetChanged()
    }
}
