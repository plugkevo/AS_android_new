package com.kevann.africanshipping25.loadinglists

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kevann.africanshipping25.loadinglists.LoadingListGoodsItem
import com.kevann.africanshipping25.translation.GoogleTranslationManager
import com.kevann.africanshipping25.translation.GoogleTranslationHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.kevann.africanshipping25.R

class LoadingListItemAdapter(
    private val items: List<LoadingListGoodsItem>, // This will be our original, unfiltered list
    private val onItemUpdated: (LoadingListGoodsItem) -> Unit,
    private var translatedLabels: Map<String, String> = emptyMap()
) : RecyclerView.Adapter<LoadingListItemAdapter.ViewHolder>(), Filterable {

    private var filteredItems: MutableList<LoadingListGoodsItem> = items.toMutableList()
    private val originalItems: List<LoadingListGoodsItem> = items // Keep a copy of the original list

    private lateinit var translationManager: GoogleTranslationManager
    private lateinit var translationHelper: GoogleTranslationHelper
    private lateinit var sharedPreferences: SharedPreferences

    // Method to update translated labels
    fun updateTranslatedLabels(labels: Map<String, String>) {
        translatedLabels = labels
        notifyDataSetChanged()
    }

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
        val item = filteredItems[position]

        // Use translated labels if available, otherwise fall back to English
        val goodNoLabel = translatedLabels["goodNo"] ?: "Good No"
        val senderLabel = translatedLabels["sender"] ?: "Sender"
        val phoneLabel = translatedLabels["phone"] ?: "Phone"
        val dateLabel = translatedLabels["date"] ?: "Date"

        holder.textGoodNo.text = "$goodNoLabel: ${item.goodNo}"
        holder.textSenderName.text = "$senderLabel: ${item.senderName}"
        holder.textPhoneNumber.text = "$phoneLabel: ${item.phoneNumber}"
        holder.textDate.text = "$dateLabel: ${item.date}"

        holder.imageMoreVert.setOnClickListener {
            // Initialize translation on first use
            if (!::translationManager.isInitialized) {
                translationManager = GoogleTranslationManager(holder.itemView.context)
                translationHelper = GoogleTranslationHelper(translationManager)
                sharedPreferences = holder.itemView.context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
            }
            showUpdateDialog(holder.itemView.context, item)
        }
    }

    override fun getItemCount(): Int = filteredItems.size // Count of filtered items

    private fun showUpdateDialog(context: Context, item: LoadingListGoodsItem) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_update_warehouse_items, null)
        val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"

        // Get and translate dialog title and labels
        dialogView.findViewById<TextView>(R.id.tvUpdateDialogTitle)?.let { dialogTitleTv ->
            translationHelper.translateAndSetText(dialogTitleTv, "Update Good Item", currentLanguage)
        }
        dialogView.findViewById<TextView>(R.id.tvGoodsNameLabel)?.let { goodsNameLabelTv ->
            translationHelper.translateAndSetText(goodsNameLabelTv, "Goods Name", currentLanguage)
        }
        dialogView.findViewById<TextView>(R.id.tvPhoneNumberLabel)?.let { phoneNumberLabelTv ->
            translationHelper.translateAndSetText(phoneNumberLabelTv, "Phone Number", currentLanguage)
        }

        val goodNoInputLayout = dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutUpdateGoodNo)
        val senderInputLayout = dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutUpdateSenderName)
        val phoneNumberInputLayout = dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutUpdatePhoneNumber)
        val dateInputLayout = dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutUpdateDate)

        val goodNoField = dialogView.findViewById<TextInputEditText>(R.id.editTextUpdateGoodNo)
        val senderField = dialogView.findViewById<TextInputEditText>(R.id.editTextUpdateSenderName)
        val phoneNumberField = dialogView.findViewById<TextInputEditText>(R.id.editTextUpdatePhoneNumber)
        val dateField = dialogView.findViewById<TextInputEditText>(R.id.editTextUpdateDate)

        val buttonUpdate = dialogView.findViewById<Button>(R.id.buttonUpdate)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancelUpdate)

        // Translate button text
        buttonUpdate?.let { btn ->
            translationHelper.translateAndSetText(btn, "Update", currentLanguage)
        }
        buttonCancel?.let { btn ->
            translationHelper.translateAndSetText(btn, "Cancel", currentLanguage)
        }

        // Translate hints
        goodNoInputLayout?.let { layout ->
            translationHelper.translateText("Good Number (4 characters)", currentLanguage) { translated ->
                layout.hint = translated
            }
        }
        senderInputLayout?.let { layout ->
            translationHelper.translateText("Sender Name", currentLanguage) { translated ->
                layout.hint = translated
            }
        }
        phoneNumberInputLayout?.let { layout ->
            translationHelper.translateText("Enter Phone Number", currentLanguage) { translated ->
                layout.hint = translated
            }
        }
        dateInputLayout?.let { layout ->
            translationHelper.translateText("Date", currentLanguage) { translated ->
                layout.hint = translated
            }
        }

        // Set current values
        goodNoField?.setText(item.goodNo)
        senderField?.setText(item.senderName)
        phoneNumberField?.setText(item.phoneNumber)
        dateField?.setText(item.date)

        dateField?.setOnClickListener {
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

        buttonUpdate?.setOnClickListener {
            val updatedGoodNo = goodNoField?.text.toString().trim()
            val updatedSenderName = senderField?.text.toString().trim()
            val updatedPhoneNumber = phoneNumberField?.text.toString().trim()
            val updatedDate = dateField?.text.toString().trim()

            // Clear any previous error messages
            goodNoInputLayout?.error = null
            senderInputLayout?.error = null
            phoneNumberInputLayout?.error = null
            dateInputLayout?.error = null

            // --- VALIDATION WITH TRANSLATION ---
            if (updatedGoodNo.isEmpty()) {
                var errorMsg = "Goods number cannot be empty."
                translationHelper.translateText(errorMsg, currentLanguage) { translated ->
                    goodNoInputLayout?.error = translated
                }
                return@setOnClickListener
            }
            if (updatedGoodNo.length != 4) {
                var errorMsg = "Goods number must be 4 characters."
                translationHelper.translateText(errorMsg, currentLanguage) { translated ->
                    goodNoInputLayout?.error = translated
                }
                return@setOnClickListener
            }
            if (updatedSenderName.isEmpty()) {
                var errorMsg = "Sender name cannot be empty."
                translationHelper.translateText(errorMsg, currentLanguage) { translated ->
                    senderInputLayout?.error = translated
                }
                return@setOnClickListener
            }
            if (updatedPhoneNumber.isEmpty()) {
                var errorMsg = "Phone number cannot be empty."
                translationHelper.translateText(errorMsg, currentLanguage) { translated ->
                    phoneNumberInputLayout?.error = translated
                }
                return@setOnClickListener
            }
            if (updatedDate.isEmpty()) {
                var errorMsg = "Date cannot be empty."
                translationHelper.translateText(errorMsg, currentLanguage) { translated ->
                    dateInputLayout?.error = translated
                }
                return@setOnClickListener
            }
            // --- END VALIDATION ---

            val updatedItem = item.copy(
                goodNo = updatedGoodNo,
                senderName = updatedSenderName,
                phoneNumber = updatedPhoneNumber,
                date = updatedDate
            )
            onItemUpdated(updatedItem)
            dialog.dismiss()
        }

        buttonCancel?.setOnClickListener {
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
                    val resultList = mutableListOf<LoadingListGoodsItem>()
                    for (row in originalItems) {
                        // Apply lowercase() instead of toLowerCase(Locale.ROOT)
                        if (row.goodNo.lowercase(Locale.ROOT).contains(charSearch.lowercase(Locale.ROOT)) ||
                            row.senderName.lowercase(Locale.ROOT).contains(charSearch.lowercase(
                                Locale.ROOT)) ||
                            row.phoneNumber.lowercase(Locale.ROOT).contains(charSearch.lowercase(
                                Locale.ROOT)) || // Include phone number in filter
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
                filteredItems = results?.values as MutableList<LoadingListGoodsItem>
                notifyDataSetChanged()
            }
        }
    }
}
