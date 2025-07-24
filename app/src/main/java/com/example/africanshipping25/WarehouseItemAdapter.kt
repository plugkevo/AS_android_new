package com.example.africanshipping25

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout // Import TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WarehouseItemAdapter(
    private val items: List<WarehouseItem>, // This will be our original, unfiltered list
    private val onItemUpdated: (WarehouseItem) -> Unit
) : RecyclerView.Adapter<WarehouseItemAdapter.ViewHolder>(), Filterable {

    private var filteredItems: MutableList<WarehouseItem> = items.toMutableList()
    private val originalItems: List<WarehouseItem> = items // Keep a copy of the original list

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textGoodNo: TextView = view.findViewById(R.id.textGoodNo)
        // Removed textGoodsName: TextView = view.findViewById(R.id.textGoodsName)
        val textSenderName: TextView = view.findViewById(R.id.textSenderName)
        val textDate: TextView = view.findViewById(R.id.textDate)
        val imageMoreVert: ImageView = view.findViewById(R.id.imageMoreVert)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Ensure 'warehouse_list_view' layout also no longer contains a TextView for 'goodsName'
        val view = LayoutInflater.from(parent.context).inflate(R.layout.warehouse_list_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = filteredItems[position] // Use filteredItems here
        holder.textGoodNo.text = "Good No: ${item.goodNo}"
        // Removed holder.textGoodsName.text = "Goods: ${item.goodsName}"
        holder.textSenderName.text = "Sender: ${item.senderName}"
        holder.textDate.text = "Date: ${item.date}"

        holder.imageMoreVert.setOnClickListener {
            showUpdateDialog(holder.itemView.context, item)
        }
    }

    override fun getItemCount(): Int = filteredItems.size // Count of filtered items

    private fun showUpdateDialog(context: Context, item: WarehouseItem) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_update_warehouse_items, null)

        // ** Get a reference to the TextInputLayout wrapping editTextUpdateGoodNo **
        val goodNoInputLayout = dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutUpdateGoodNo)
        val goodNoField = dialogView.findViewById<TextInputEditText>(R.id.editTextUpdateGoodNo)
        val senderField = dialogView.findViewById<TextInputEditText>(R.id.editTextUpdateSenderName)
        val dateField = dialogView.findViewById<TextInputEditText>(R.id.editTextUpdateDate)
        val buttonUpdate = dialogView.findViewById<Button>(R.id.buttonUpdate)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancelUpdate)

        goodNoField.setText(item.goodNo)
        senderField.setText(item.senderName)
        dateField.setText(item.date)

        dateField.setOnClickListener {
            val c = Calendar.getInstance()
            if (dateField.text?.isNotEmpty() == true) {
                try {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    c.time = sdf.parse(dateField.text.toString()) ?: Calendar.getInstance().time
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val dpd = DatePickerDialog(context, { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                dateField.setText(sdf.format(selectedDate.time))
            }, year, month, day)
            dpd.show()
        }

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        buttonUpdate.setOnClickListener {
            val updatedGoodNo = goodNoField.text.toString().trim() // Get trimmed text
            val updatedSenderName = senderField.text.toString().trim()
            val updatedDate = dateField.text.toString().trim()

            // Clear any previous error messages on the goodNoField's TextInputLayout
            goodNoInputLayout.error = null

            // --- ADD 4-CHARACTER VALIDATION FOR GOODS NUMBER HERE ---
            if (updatedGoodNo.isEmpty()) {
                goodNoInputLayout.error = "Goods number cannot be empty."
                return@setOnClickListener // Stop the update process
            }

            if (updatedGoodNo.length != 4) {
                goodNoInputLayout.error = "Goods number must be 4 characters."
                return@setOnClickListener // Stop the update process
            }
            // --- END VALIDATION ---

            // Basic validation for other fields (optional but recommended)
            if (updatedSenderName.isEmpty()) {
                senderField.error = "Sender name cannot be empty."
                return@setOnClickListener
            }
            if (updatedDate.isEmpty()) {
                dateField.error = "Date cannot be empty."
                return@setOnClickListener
            }


            val updatedItem = item.copy(
                goodNo = updatedGoodNo,
                senderName = updatedSenderName,
                date = updatedDate
            )
            onItemUpdated(updatedItem)
            dialog.dismiss()
        }

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charSearch = constraint.toString()
                filteredItems = if (charSearch.isEmpty()) {
                    originalItems.toMutableList()
                } else {
                    val resultList = mutableListOf<WarehouseItem>()
                    for (row in originalItems) {
                        // Apply lowercase() instead of toLowerCase(Locale.ROOT)
                        if (row.goodNo.lowercase(Locale.ROOT).contains(charSearch.lowercase(Locale.ROOT)) ||
                            row.senderName.lowercase(Locale.ROOT).contains(charSearch.lowercase(Locale.ROOT)) ||
                            row.date.lowercase(Locale.ROOT).contains(charSearch.lowercase(Locale.ROOT))
                        ) {
                            resultList.add(row)
                        }
                    }
                    resultList
                }
                val filterResults = FilterResults()
                filterResults.values = filteredItems
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredItems = results?.values as MutableList<WarehouseItem>
                notifyDataSetChanged()
            }
        }
    }
}