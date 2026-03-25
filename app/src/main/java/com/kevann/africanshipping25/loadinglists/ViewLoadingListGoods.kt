package com.kevann.africanshipping25.loadinglists

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.kevann.africanshipping25.loadinglists.LoadingListGoodsItem
import com.kevann.africanshipping25.translation.GoogleTranslationManager
import com.kevann.africanshipping25.translation.GoogleTranslationHelper
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*
import com.kevann.africanshipping25.R  // Add this import


private const val ARG_LOADING_LIST_ID = "loadingListId"
private const val WAREHOUSE_ITEMS_COLLECTION = "warehouseItems"
private const val LOADING_LISTS_COLLECTION = "loading_lists"

class ViewWarehouseGoods : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LoadingListItemAdapter
    private val itemList = mutableListOf<LoadingListGoodsItem>()
    private lateinit var firestore: FirebaseFirestore
    private var listenerRegistration: ListenerRegistration? = null
    private var loadingListId: String? = null

    private lateinit var searchEditText: EditText
    private lateinit var emptyView: TextView
    private lateinit var exportButton: Button
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var translationManager: GoogleTranslationManager
    private lateinit var translationHelper: GoogleTranslationHelper

    // Permission launcher for storage access
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            exportToCSV()
        } else {
            val permMsg = "Storage permission is required to export files"
            if (::sharedPreferences.isInitialized) {
                val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
                translationHelper.translateText(permMsg, currentLanguage) { translatedMessage ->
                    Toast.makeText(context, translatedMessage, Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(requireContext(), permMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            loadingListId = it.getString(ARG_LOADING_LIST_ID)
        }
        Log.d("ViewWarehouseGoods", "onCreate: Received loadingListId: $loadingListId")

        firestore = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_view_warehouse_goods, container, false)

        // Initialize translation
        sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        translationManager = GoogleTranslationManager(requireContext())
        translationHelper = GoogleTranslationHelper(translationManager)

        recyclerView = view.findViewById(R.id.storeInventoryRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        searchEditText = view.findViewById(R.id.searchEditText)
        emptyView = view.findViewById(R.id.emptyView)
        exportButton = view.findViewById(R.id.exportButton)

        adapter = LoadingListItemAdapter(itemList) { updatedItem ->
            updateItemInFirestore(updatedItem)
        }
        recyclerView.adapter = adapter

        // Set up export button click listener
        exportButton.setOnClickListener {
            if (itemList.isEmpty()) {
                val noDataMsg = "No data to export"
                showTranslatedToast(noDataMsg)
            } else {
                showExportOptionsDialog()
            }
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter.filter(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        loadItemsFromFirestore()

        // Translate UI elements
        val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
        translateUIElements(view, currentLanguage)

        return view
    }

    private fun showExportOptionsDialog() {
        val options = arrayOf("Export as CSV", "Export as Excel")
        val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
        var dialogTitle = "Choose Export Format"
        var csvOption = "Export as CSV"
        var excelOption = "Export as Excel"

        translationHelper.translateText("Choose Export Format", currentLanguage) { translated ->
            dialogTitle = translated
        }
        translationHelper.translateText("Export as CSV", currentLanguage) { translated ->
            csvOption = translated
        }
        translationHelper.translateText("Export as Excel", currentLanguage) { translated ->
            excelOption = translated
        }

        AlertDialog.Builder(requireContext())
            .setTitle(dialogTitle)
            .setItems(arrayOf(csvOption, excelOption)) { _, which ->
                when (which) {
                    0 -> checkPermissionAndExportCSV()
                    1 -> checkPermissionAndExportExcel()
                }
            }
            .show()
    }

    private fun checkPermissionAndExportCSV() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                exportToCSV()
            }
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                exportToCSV()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun checkPermissionAndExportExcel() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                exportToExcel()
            }
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                exportToExcel()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun exportToCSV() {
        try {
            exportButton.isEnabled = false
            val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
            translationHelper.translateText("Exporting...", currentLanguage) { translated ->
                exportButton.text = translated
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "WarehouseInventory_${loadingListId}_$timestamp.csv"

            val csvContent = buildString {
                // Add header
                appendLine("Goods Number,Sender Name,Phone Number,Date,Export Date,Loading List ID")

                // Add data rows
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val currentDate = dateFormat.format(Date())

                itemList.forEach { warehouseItem ->
                    val goodsNumber = warehouseItem.goodNo.replace(",", ";")
                    val senderName = warehouseItem.senderName.replace(",", ";")
                    val phoneNumber = warehouseItem.phoneNumber.replace(",", ";")
                    val itemDate = warehouseItem.date.replace(",", ";")
                    appendLine("$goodsNumber,$senderName,$phoneNumber,$itemDate,$currentDate,${loadingListId ?: ""}")
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveCSVToDownloads(csvContent, filename)
            } else {
                saveCSVToExternalStorage(csvContent, filename)
            }

        } catch (e: Exception) {
            Log.e("ExportCSV", "Error exporting to CSV", e)
            val errorMsg = "Error exporting file: ${e.message}"
            showTranslatedToast(errorMsg)
        } finally {
            exportButton.isEnabled = true
            val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
            translationHelper.translateText("Export", currentLanguage) { translated ->
                exportButton.text = translated
            }
        }
    }

    private fun exportToExcel() {
        try {
            exportButton.isEnabled = false
            val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
            translationHelper.translateText("Exporting...", currentLanguage) { translated ->
                exportButton.text = translated
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "WarehouseInventory_${loadingListId}_$timestamp.csv"

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val currentDate = dateFormat.format(Date())

            val csvContent = buildString {
                // Add BOM for Excel to recognize UTF-8
                append("\uFEFF")
                // Header row
                append("Goods Number,Sender Name,Phone Number,Date,Export Date,Loading List ID\n")

                // Data rows
                itemList.forEach { warehouseItem ->
                    append("\"${escapeCsvField(warehouseItem.goodNo)}\",")
                    append("\"${escapeCsvField(warehouseItem.senderName)}\",")
                    append("\"${escapeCsvField(warehouseItem.phoneNumber)}\",")
                    append("\"${escapeCsvField(warehouseItem.date)}\",")
                    append("\"$currentDate\",")
                    append("\"${escapeCsvField(loadingListId ?: "")}\"\n")
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = requireContext().contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                        val successMsg = "File exported to Downloads: $filename\nOpen with Excel or Google Sheets"
                        showTranslatedToast(successMsg)
                    }
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()

                File(downloadsDir, filename).writeText(csvContent, Charsets.UTF_8)
                val successMsg = "File exported to Downloads: $filename\nOpen with Excel or Google Sheets"
                showTranslatedToast(successMsg)
            }

        } catch (e: Exception) {
            Log.e("ExportExcel", "Error exporting", e)
            val errorMsg = "Error exporting: ${e.message}"
            showTranslatedToast(errorMsg)
        } finally {
            exportButton.isEnabled = true
            val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
            translationHelper.translateText("Export", currentLanguage) { translated ->
                exportButton.text = translated
            }
        }
    }

    private fun escapeCsvField(field: String): String {
        return field.replace("\"", "\"\"")
    }
    private fun saveCSVToDownloads(content: String, filename: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = requireContext().contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(content)
                        val successMsg = "CSV file exported to Downloads: $filename"
                        showTranslatedToast(successMsg)
                    }
                }
            } ?: run {
                val failMsg = "Failed to create file"
                showTranslatedToast(failMsg)
            }
        }
    }

    private fun saveCSVToExternalStorage(content: String, filename: String) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val file = File(downloadsDir, filename)
        FileOutputStream(file).use { outputStream ->
            OutputStreamWriter(outputStream).use { writer ->
                writer.write(content)
                val successMsg = "CSV file exported to Downloads: $filename"
                showTranslatedToast(successMsg)
            }
        }
    }

    private fun saveHTMLToDownloads(content: String, filename: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = requireContext().contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.ms-excel")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(content)
                        val successMsg = "Excel file exported to Downloads: $filename"
                        showTranslatedToast(successMsg)
                    }
                }
            } ?: run {
                val failMsg = "Failed to create file"
                showTranslatedToast(failMsg)
            }
        }
    }

    private fun saveHTMLToExternalStorage(content: String, filename: String) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val file = File(downloadsDir, filename)
        FileOutputStream(file).use { outputStream ->
            OutputStreamWriter(outputStream).use { writer ->
                writer.write(content)
                val successMsg = "Excel file exported to Downloads: $filename"
                showTranslatedToast(successMsg)
            }
        }
    }

    private fun loadItemsFromFirestore() {
        loadingListId?.let { id ->
            val collectionPath = "$LOADING_LISTS_COLLECTION/$id/$WAREHOUSE_ITEMS_COLLECTION"
            Log.d("ViewWarehouseGoods", "Attempting to load items from Firestore path: $collectionPath")

            listenerRegistration = firestore.collection(LOADING_LISTS_COLLECTION)
                .document(id)
                .collection(WAREHOUSE_ITEMS_COLLECTION)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("ViewWarehouseGoods", "Error fetching documents: ${error.message}", error)
                        val errorMsg = "Error loading items: ${error.message}"
                        showTranslatedToast(errorMsg)
                        updateEmptyView(true)
                        return@addSnapshotListener
                    }

                    if (snapshot == null) {
                        Log.w("ViewWarehouseGoods", "Snapshot is null, no data received.")
                        updateEmptyView(true)
                        return@addSnapshotListener
                    }

                    itemList.clear()
                    if (snapshot.isEmpty) {
                        Log.d("ViewWarehouseGoods", "No documents found in '$WAREHOUSE_ITEMS_COLLECTION' for loadingListId: $id")
                        updateEmptyView(true)
                    } else {
                        Log.d("ViewWarehouseGoods", "Found ${snapshot.documents.size} documents in '$WAREHOUSE_ITEMS_COLLECTION' for loadingListId: $id")
                        updateEmptyView(false)
                    }

                    for (doc in snapshot.documents) {
                        val item = doc.toObject(LoadingListGoodsItem::class.java)
                        if (item != null) {
                            itemList.add(item.copy(id = doc.id))
                            Log.d("ViewWarehouseGoods", "Added item: GoodNo=${item.goodNo}, Sender=${item.senderName}, Phone=${item.phoneNumber} (Firestore ID: ${doc.id})")
                        } else {
                            Log.e("ViewWarehouseGoods", "Failed to convert document ${doc.id} to WarehouseItem. Data: ${doc.data}")
                        }
                    }

                    adapter.getFilter().filter(searchEditText.text.toString())
                    Log.d("ViewWarehouseGoods", "Adapter notified. Total items in list: ${itemList.size}")
                }
        } ?: run {
            Log.e("ViewWarehouseGoods", "loadingListId is NULL. Cannot load items.")
            val errorMsg = "Error: Loading list ID is missing. Cannot retrieve items."
            showTranslatedToast(errorMsg)
            updateEmptyView(true)
        }
    }

    // Helper method to translate toast messages
    private fun showTranslatedToast(message: String) {
        val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
        translationHelper.translateText(message, currentLanguage) { translatedMessage ->
            Toast.makeText(context, translatedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    // Translation method for UI elements
    private fun translateUIElements(view: View, targetLanguage: String) {
        view.let { v ->
            // Translate Export button
            v.findViewById<Button>(R.id.exportButton)?.let { btn ->
                translationHelper.translateAndSetText(btn, "Export", targetLanguage)
            }
            
            // Translate Search EditText hint
            v.findViewById<EditText>(R.id.searchEditText)?.let { et ->
                translationHelper.translateAndSetText(et, "Search warehouse items...", targetLanguage)
            }
            
            // Translate empty state message
            v.findViewById<TextView>(R.id.emptyView)?.let { tv ->
                translationHelper.translateAndSetText(tv, "No warehouse items", targetLanguage)
            }
        }
    }

    private fun updateEmptyView(show: Boolean) {
        if (show) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }
    }

    private fun updateItemInFirestore(item: LoadingListGoodsItem) {
        loadingListId?.let { id ->
            if (item.id.isEmpty()) {
                Log.e("ViewWarehouseGoods", "Attempted to update item with empty ID: $item")
                Toast.makeText(requireContext(), "Error: Item ID is missing for update.", Toast.LENGTH_SHORT).show()
                return@let
            }
            Log.d("ViewWarehouseGoods", "Updating item with ID: ${item.id} in path: $LOADING_LISTS_COLLECTION/$id/$WAREHOUSE_ITEMS_COLLECTION")
            firestore.collection(LOADING_LISTS_COLLECTION)
                .document(id)
                .collection(WAREHOUSE_ITEMS_COLLECTION)
                .document(item.id)
                .set(item)
                .addOnSuccessListener {
                    Log.d("ViewWarehouseGoods", "Item ${item.id} updated successfully.")
                    val successMsg = "Updated successfully"
                    showTranslatedToast(successMsg)
                }
                .addOnFailureListener { e ->
                    Log.e("ViewWarehouseGoods", "Update failed for item ${item.id}: ${e.message}", e)
                    val errorMsg = "Update failed: ${e.message}"
                    showTranslatedToast(errorMsg)
                }
        } ?: run {
            Log.e("ViewWarehouseGoods", "loadingListId is NULL. Cannot update item.")
            val errorMsg = "Error: Loading list ID is missing. Cannot update item."
            showTranslatedToast(errorMsg)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
        Log.d("ViewWarehouseGoods", "onDestroyView: Firestore listener removed.")
    }

    companion object {
        @JvmStatic
        fun newInstance(loadingListId: String) =
            ViewWarehouseGoods().apply {
                arguments = Bundle().apply {
                    putString(ARG_LOADING_LIST_ID, loadingListId)
                }
            }
    }
}
