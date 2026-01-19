package com.kevann.africanshipping25.loadinglists

import android.Manifest
import android.content.ContentValues
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

    // Permission launcher for storage access
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            exportToCSV()
        } else {
            Toast.makeText(requireContext(), "Storage permission is required to export files", Toast.LENGTH_LONG).show()
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
                Toast.makeText(requireContext(), "No data to export", Toast.LENGTH_SHORT).show()
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

        return view
    }

    private fun showExportOptionsDialog() {
        val options = arrayOf("Export as CSV", "Export as Excel")
        AlertDialog.Builder(requireContext())
            .setTitle("Choose Export Format")
            .setItems(options) { _, which ->
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
            exportButton.setText("Exporting...")

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
            Toast.makeText(requireContext(), "Error exporting file: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            exportButton.isEnabled = true
            exportButton.setText("Export")
        }
    }

    private fun exportToExcel() {
        try {
            exportButton.isEnabled = false
            exportButton.setText("Exporting...")

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "WarehouseInventory_${loadingListId}_$timestamp.xls"

            val htmlContent = buildString {
                appendLine("<html><body>")
                appendLine("<table border='1'>")
                appendLine("<tr style='background-color: lightcoral; font-weight: bold;'>")
                appendLine("<td>Goods Number</td><td>Sender Name</td><td>Phone Number</td><td>Date</td><td>Export Date</td><td>Loading List ID</td>")
                appendLine("</tr>")

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val currentDate = dateFormat.format(Date())

                itemList.forEach { warehouseItem ->
                    appendLine("<tr>")
                    appendLine("<td>${warehouseItem.goodNo}</td>")
                    appendLine("<td>${warehouseItem.senderName}</td>")
                    appendLine("<td>${warehouseItem.phoneNumber}</td>")
                    appendLine("<td>${warehouseItem.date}</td>")
                    appendLine("<td>$currentDate</td>")
                    appendLine("<td>${loadingListId ?: ""}</td>")
                    appendLine("</tr>")
                }

                appendLine("</table>")
                appendLine("</body></html>")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveHTMLToDownloads(htmlContent, filename)
            } else {
                saveHTMLToExternalStorage(htmlContent, filename)
            }

        } catch (e: Exception) {
            Log.e("ExportExcel", "Error exporting to Excel", e)
            Toast.makeText(requireContext(), "Error exporting file: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            exportButton.isEnabled = true
            exportButton.setText("Export")
        }
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
                        Toast.makeText(requireContext(), "CSV file exported to Downloads: $filename", Toast.LENGTH_LONG).show()
                    }
                }
            } ?: run {
                Toast.makeText(requireContext(), "Failed to create file", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(requireContext(), "CSV file exported to Downloads: $filename", Toast.LENGTH_LONG).show()
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
                        Toast.makeText(requireContext(), "Excel file exported to Downloads: $filename", Toast.LENGTH_LONG).show()
                    }
                }
            } ?: run {
                Toast.makeText(requireContext(), "Failed to create file", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(requireContext(), "Excel file exported to Downloads: $filename", Toast.LENGTH_LONG).show()
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
                        Toast.makeText(requireContext(), "Error loading items: ${error.message}", Toast.LENGTH_LONG).show()
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
            Toast.makeText(requireContext(), "Error: Loading list ID is missing. Cannot retrieve items.", Toast.LENGTH_LONG).show()
            updateEmptyView(true)
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
                    Toast.makeText(requireContext(), "Updated successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("ViewWarehouseGoods", "Update failed for item ${item.id}: ${e.message}", e)
                    Toast.makeText(requireContext(), "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            Log.e("ViewWarehouseGoods", "loadingListId is NULL. Cannot update item.")
            Toast.makeText(requireContext(), "Error: Loading list ID is missing. Cannot update item.", Toast.LENGTH_LONG).show()
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