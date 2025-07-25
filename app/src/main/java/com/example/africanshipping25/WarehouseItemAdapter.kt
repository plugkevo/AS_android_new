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
        val textSenderName: TextView = view.findViewById(R.id.textSenderName)
        val textPhoneNumber: TextView = view.findViewById(R.id.textPhoneNumber) // Added phone number TextView
        val textDate: TextView = view.findViewById(R.id.textDate)
        val imageMoreVert: ImageView = view.findViewById(R.id.imageMoreVert)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.warehouse_list_view, parent, false) // Changed to warehouse_item_card
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = filteredItems[position] // Use filteredItems here
        holder.textGoodNo.text = "Good No: ${item.goodNo}"
        holder.textSenderName.text = "Sender: ${item.senderName}"
        holder.textPhoneNumber.text = "Phone: ${item.phoneNumber}" // Set phone number text
        holder.textDate.text = "Date: ${item.date}"

        holder.imageMoreVert.setOnClickListener {
            showUpdateDialog(holder.itemView.context, item)
        }
    }

    override fun getItemCount(): Int = filteredItems.size // Count of filtered items

    private fun showUpdateDialog(context: Context, item: WarehouseItem) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_update_warehouse_items, null)

        val goodNoInputLayout = dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutUpdateGoodNo)
        val goodNoField = dialogView.findViewById<TextInputEditText>(R.id.editTextUpdateGoodNo)
        val senderField = dialogView.findViewById<TextInputEditText>(R.id.editTextUpdateSenderName)
        val phoneNumberField = dialogView.findViewById<TextInputEditText>(R.id.editTextUpdatePhoneNumber) // Get phone number field
        val dateField = dialogView.findViewById<TextInputEditText>(R.id.editTextUpdateDate)

        val buttonUpdate = dialogView.findViewById<Button>(R.id.buttonUpdate)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancelUpdate)

        goodNoField.setText(item.goodNo)
        senderField.setText(item.senderName)
        phoneNumberField.setText(item.phoneNumber) // Set initial phone number
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
            val updatedGoodNo = goodNoField.text.toString().trim()
            val updatedSenderName = senderField.text.toString().trim()
            val updatedPhoneNumber = phoneNumberField.text.toString().trim() // Get updated phone number
            val updatedDate = dateField.text.toString().trim()

            // Clear any previous error messages
            goodNoInputLayout.error = null
            val phoneNumberInputLayout = dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutUpdatePhoneNumber)
            phoneNumberInputLayout.error = null // Clear error for phone number

            // --- VALIDATION ---
            if (updatedGoodNo.isEmpty()) {
                goodNoInputLayout.error = "Goods number cannot be empty."
                return@setOnClickListener
            }
            if (updatedGoodNo.length != 4) {
                goodNoInputLayout.error = "Goods number must be 4 characters."
                return@setOnClickListener
            }
            if (updatedSenderName.isEmpty()) {
                senderField.error = "Sender name cannot be empty."
                return@setOnClickListener
            }
            if (updatedPhoneNumber.isEmpty()) { // Validate phone number
                phoneNumberInputLayout.error = "Phone number cannot be empty."
                return@setOnClickListener
            }
            // Optional: Add more specific phone number validation (e.g., length, format)
            // if (updatedPhoneNumber.length < 10) {
            //     phoneNumberInputLayout.error = "Phone number is too short."
            //     return@setOnClickListener
            // }
            if (updatedDate.isEmpty()) {
                dateField.error = "Date cannot be empty."
                return@setOnClickListener
            }
            // --- END VALIDATION ---

            val updatedItem = item.copy(
                goodNo = updatedGoodNo,
                senderName = updatedSenderName,
                phoneNumber = updatedPhoneNumber, // Include phone number in the updated item
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
                            row.phoneNumber.lowercase(Locale.ROOT).contains(charSearch.lowercase(Locale.ROOT)) || // Include phone number in filter
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