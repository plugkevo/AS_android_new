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
        val textGoodsName: TextView = view.findViewById(R.id.textGoodsName)
        val textSenderName: TextView = view.findViewById(R.id.textSenderName)
        val textDate: TextView = view.findViewById(R.id.textDate)
        val imageMoreVert: ImageView = view.findViewById(R.id.imageMoreVert)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.warehouse_list_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = filteredItems[position] // Use filteredItems here
        holder.textGoodNo.text = "Good No: ${item.goodNo}"
        holder.textGoodsName.text = "Goods: ${item.goodsName}"
        holder.textSenderName.text = "Sender: ${item.senderName}"
        holder.textDate.text = "Date: ${item.date}"

        holder.imageMoreVert.setOnClickListener {
            showUpdateDialog(holder.itemView.context, item)
        }
    }

    override fun getItemCount(): Int = filteredItems.size // Count of filtered items

    private fun showUpdateDialog(context: Context, item: WarehouseItem) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_update_warehouse_items, null)
        val goodNoField = dialogView.findViewById<TextInputEditText>(R.id.editTextUpdateGoodNo)
        val goodsSpinner = dialogView.findViewById<Spinner>(R.id.spinnerUpdateGoodsName)
        val senderField = dialogView.findViewById<TextInputEditText>(R.id.editTextUpdateSenderName)
        val dateField = dialogView.findViewById<TextInputEditText>(R.id.editTextUpdateDate)
        val buttonUpdate = dialogView.findViewById<Button>(R.id.buttonUpdate)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancelUpdate)

        goodNoField.setText(item.goodNo)
        senderField.setText(item.senderName)
        dateField.setText(item.date)

        val goodsOptions = context.resources.getStringArray(R.array.goods_name_options)
        val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, goodsOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        goodsSpinner.adapter = spinnerAdapter
        goodsSpinner.setSelection(goodsOptions.indexOf(item.goodsName))

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
            val updatedItem = item.copy(
                goodNo = goodNoField.text.toString(),
                goodsName = goodsSpinner.selectedItem.toString(),
                senderName = senderField.text.toString(),
                date = dateField.text.toString()
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
                            row.goodsName.lowercase(Locale.ROOT).contains(charSearch.lowercase(Locale.ROOT)) ||
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