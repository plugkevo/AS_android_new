package com.kevann.africanshipping25.shipments

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
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObjects
import com.airbnb.lottie.LottieAnimationView
import com.kevann.africanshipping25.shipments.StoreGoodsAdapter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*
import com.kevann.africanshipping25.R  // Add this import


data class StoreGood(var goodsNumber: Long? = null, var name: String? = null, var storeLocation: String? = null)

class view_store_goods : Fragment() {

    private lateinit var storeInventoryRecyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var storeGoodsAdapter: StoreGoodsAdapter
    private lateinit var db: FirebaseFirestore
    private var currentShipmentId: String? = null
    private lateinit var searchEditText: EditText
    private lateinit var exportButton: Button

    // Declare Lottie animations
    private lateinit var lottieLoadingAnimation: LottieAnimationView
    private lateinit var lottieNoDataAnimation: LottieAnimationView

    private var allStoreGoods: List<StoreGood> = listOf()

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
            currentShipmentId = it.getString("shipmentId")
            Log.d("ViewStoreGoodsFragment", "onCreate: Shipment ID received: $currentShipmentId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_view_store_goods, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        storeInventoryRecyclerView = view.findViewById(R.id.storeInventoryRecyclerView)
        emptyView = view.findViewById(R.id.emptyView)
        searchEditText = view.findViewById(R.id.searchEditText)
        exportButton = view.findViewById(R.id.exportButton)

        // Initialize Lottie animations
        lottieLoadingAnimation = view.findViewById(R.id.lottie_loading_animation)
        lottieNoDataAnimation = view.findViewById(R.id.lottie_no_data_animation)

        storeInventoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        storeInventoryRecyclerView.setHasFixedSize(true)

        storeGoodsAdapter = StoreGoodsAdapter(mutableListOf()) { storeGood ->
            showGoodsDetailsDialog(storeGood)
        }
        storeInventoryRecyclerView.adapter = storeGoodsAdapter

        // Set up export button click listener
        exportButton.setOnClickListener {
            if (allStoreGoods.isEmpty()) {
                Toast.makeText(requireContext(), "No data to export", Toast.LENGTH_SHORT).show()
            } else {
                showExportOptionsDialog()
            }
        }

        loadStoreInventory()
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                filterStoreGoods(s?.toString() ?: "")
            }
        })
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
            val filename = "StoreInventory_${currentShipmentId}_$timestamp.csv"

            val csvContent = buildString {
                // Add header
                appendLine("Goods Number,Goods Name,Store Location,Export Date,Shipment ID")

                // Add data rows
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val currentDate = dateFormat.format(Date())

                allStoreGoods.forEach { storeGood ->
                    val goodsNumber = storeGood.goodsNumber?.toString() ?: ""
                    val goodsName = storeGood.name?.replace(",", ";") ?: ""
                    val storeLocation = storeGood.storeLocation?.replace(",", ";") ?: ""
                    appendLine("$goodsNumber,$goodsName,$storeLocation,$currentDate,${currentShipmentId ?: ""}")
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
            val filename = "StoreInventory_${currentShipmentId}_$timestamp.xls"

            val htmlContent = buildString {
                appendLine("<html><body>")
                appendLine("<table border='1'>")
                appendLine("<tr style='background-color: lightgreen; font-weight: bold;'>")
                appendLine("<td>Goods Number</td><td>Goods Name</td><td>Store Location</td><td>Export Date</td><td>Shipment ID</td>")
                appendLine("</tr>")

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val currentDate = dateFormat.format(Date())

                allStoreGoods.forEach { storeGood ->
                    appendLine("<tr>")
                    appendLine("<td>${storeGood.goodsNumber ?: ""}</td>")
                    appendLine("<td>${storeGood.name ?: ""}</td>")
                    appendLine("<td>${storeGood.storeLocation ?: ""}</td>")
                    appendLine("<td>$currentDate</td>")
                    appendLine("<td>${currentShipmentId ?: ""}</td>")
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

    private fun loadStoreInventory() {
        if (currentShipmentId == null) {
            Log.e("view_store_goods", "Shipment ID is null")
            emptyView.text = "Error: Shipment ID is missing."
            emptyView.visibility = View.VISIBLE

            lottieLoadingAnimation.visibility = View.GONE
            lottieLoadingAnimation.cancelAnimation()
            lottieNoDataAnimation.visibility = View.GONE
            lottieNoDataAnimation.cancelAnimation()
            storeInventoryRecyclerView.visibility = View.GONE

            return
        }

        Log.d("ViewStoreGoodsFragment", "loadStoreInventory: shipmentId = $currentShipmentId")

        lottieLoadingAnimation.visibility = View.VISIBLE
        lottieLoadingAnimation.playAnimation()
        storeInventoryRecyclerView.visibility = View.GONE
        emptyView.visibility = View.GONE
        lottieNoDataAnimation.visibility = View.GONE
        lottieNoDataAnimation.cancelAnimation()

        db.collection("shipments")
            .document(currentShipmentId!!)
            .collection("store_inventory")
            .get()
            .addOnSuccessListener { querySnapshot ->
                lottieLoadingAnimation.cancelAnimation()
                lottieLoadingAnimation.visibility = View.GONE

                handleStoreInventoryData(querySnapshot)
            }
            .addOnFailureListener { e ->
                Log.e("ViewStoreGoodsFragment", "Error getting store inventory: ", e)

                lottieLoadingAnimation.cancelAnimation()
                lottieLoadingAnimation.visibility = View.GONE

                emptyView.text = "Error loading data: ${e.message}"
                emptyView.visibility = View.VISIBLE
                storeInventoryRecyclerView.visibility = View.GONE
                lottieNoDataAnimation.visibility = View.GONE
                lottieNoDataAnimation.cancelAnimation()
            }
    }

    private fun handleStoreInventoryData(querySnapshot: QuerySnapshot) {
        if (querySnapshot.isEmpty) {
            emptyView.text = "No items in store inventory."
            emptyView.visibility = View.VISIBLE
            storeInventoryRecyclerView.visibility = View.GONE
            allStoreGoods = listOf()
            storeGoodsAdapter.updateData(mutableListOf())

            lottieNoDataAnimation.visibility = View.VISIBLE
            lottieNoDataAnimation.playAnimation()
        } else {
            emptyView.visibility = View.GONE
            storeInventoryRecyclerView.visibility = View.VISIBLE
            val storeGoodsList = querySnapshot.toObjects<StoreGood>()
            Log.d("ViewStoreGoodsFragment", "handleStoreInventoryData: storeGoodsList size = ${storeGoodsList.size}")
            allStoreGoods = storeGoodsList
            storeGoodsAdapter.updateData(storeGoodsList)

            lottieNoDataAnimation.visibility = View.GONE
            lottieNoDataAnimation.cancelAnimation()
        }
    }

    private fun filterStoreGoods(query: String) {
        val filteredList = if (query.isBlank()) {
            allStoreGoods
        } else {
            val lowerCaseQuery = query.lowercase(Locale.getDefault())
            allStoreGoods.filter { storeGood ->
                storeGood.name?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true ||
                        storeGood.goodsNumber?.toString()?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true ||
                        storeGood.storeLocation?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true
            }
        }
        storeGoodsAdapter.updateData(filteredList.toMutableList())

        if (filteredList.isEmpty()) {
            emptyView.text = if (query.isBlank()) {
                "No items in store inventory."
            } else {
                "No matching items found."
            }
            emptyView.visibility = View.VISIBLE
            storeInventoryRecyclerView.visibility = View.GONE
            lottieNoDataAnimation.visibility = View.VISIBLE
            lottieNoDataAnimation.playAnimation()
        } else {
            emptyView.visibility = View.GONE
            storeInventoryRecyclerView.visibility = View.VISIBLE
            lottieNoDataAnimation.visibility = View.GONE
            lottieNoDataAnimation.cancelAnimation()
        }
    }

    private fun showGoodsDetailsDialog(storeGood: StoreGood) {
        val dialogBuilder = AlertDialog.Builder(requireContext())
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_store_goods_details, null)
        dialogBuilder.setView(dialogView)
        val dialog = dialogBuilder.create()

        val goodsNameTextView = dialogView.findViewById<TextView>(R.id.detailGoodsNameTextView)
        val goodsNumberEditText = dialogView.findViewById<EditText>(R.id.detailGoodsNumberTextView)
        val storeLocationTextView = dialogView.findViewById<TextView>(R.id.detailStoreLocationTextView)
        val updateButton = dialogView.findViewById<Button>(R.id.detailUpdateButton)
        val closeButton = dialogView.findViewById<Button>(R.id.detailCloseButton)

        val goodsNameOptions = arrayOf("Box","Furniture","Electronics", "Toiletries","Tote/Barrel", "Machinery","Other")
        val locationOptions = arrayOf("Store A","Store B", "Store C")

        var selectedGoodsName = storeGood.name
        var selectedLocation = storeGood.storeLocation

        goodsNumberEditText.setText(storeGood.goodsNumber?.toString())
        goodsNameTextView.text = "Name: ${selectedGoodsName ?: "N/A"}"
        storeLocationTextView.text = "Location: ${selectedLocation ?: "N/A"}"

        goodsNameTextView.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Select Goods Name")
                .setSingleChoiceItems(goodsNameOptions, goodsNameOptions.indexOf(selectedGoodsName)) { _, which ->
                    selectedGoodsName = goodsNameOptions[which]
                }
                .setPositiveButton("OK") { dialogInterface, _ ->
                    goodsNameTextView.text = "Name: $selectedGoodsName"
                    dialogInterface.dismiss()
                }
                .setNegativeButton("Cancel") { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                .show()
        }

        storeLocationTextView.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Select Store Location")
                .setSingleChoiceItems(locationOptions, locationOptions.indexOf(selectedLocation)) { _, which ->
                    selectedLocation = locationOptions[which]
                }
                .setPositiveButton("OK") { dialogInterface, _ ->
                    storeLocationTextView.text = "Location: $selectedLocation"
                    dialogInterface.dismiss()
                }
                .setNegativeButton("Cancel") { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                .show()
        }

        updateButton.setOnClickListener {
            val newNumberString = goodsNumberEditText.text.toString().trim()

            if (newNumberString.isEmpty()) {
                Toast.makeText(requireContext(), "Goods number cannot be empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newNumberString.length != 4) {
                Toast.makeText(requireContext(), "Goods number must be 4 characters.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newNumber = newNumberString.toLongOrNull()
            if (newNumber == null) {
                Toast.makeText(requireContext(), "Invalid goods number (must be numeric).", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d("ViewStoreGoodsFragment", "Update: Name=$selectedGoodsName, Number=$newNumber, Location=$selectedLocation")

            if (currentShipmentId != null) {
                updateStoreGoodInFirestore(
                    currentShipmentId!!,
                    storeGood.goodsNumber,
                    selectedGoodsName,
                    newNumber,
                    selectedLocation
                )
            }
            dialog.dismiss()
        }

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateStoreGoodInFirestore(
        shipmentId: String,
        oldGoodsNumber: Long?,
        newName: String?,
        newNumber: Long,
        newLocation: String?
    ) {
        db.collection("shipments")
            .document(shipmentId)
            .collection("store_inventory")
            .whereEqualTo("goodsNumber", oldGoodsNumber)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Log.d("Firestore", "No matching documents found to update")
                    Toast.makeText(requireContext(), "No matching goods found to update.", Toast.LENGTH_SHORT).show()
                } else {
                    for (document in querySnapshot) {
                        val documentId = document.id
                        db.collection("shipments")
                            .document(shipmentId)
                            .collection("store_inventory")
                            .document(documentId)
                            .update(
                                mapOf(
                                    "name" to newName,
                                    "goodsNumber" to newNumber,
                                    "storeLocation" to newLocation
                                )
                            )
                            .addOnSuccessListener {
                                Log.d("Firestore", "Document updated successfully")
                                Toast.makeText(requireContext(), "Goods updated successfully.", Toast.LENGTH_SHORT).show()
                                loadStoreInventory()
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "Error updating document", e)
                                Toast.makeText(requireContext(), "Error updating goods: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error getting document to update", e)
                Toast.makeText(requireContext(), "Error finding goods to update: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        fun newInstance(shipmentId: String): view_store_goods {
            val fragment = view_store_goods()
            val args = Bundle()
            args.putString("shipmentId", shipmentId)
            fragment.arguments = args
            Log.d("view_store_goods", "newInstance: Shipment ID passed: $shipmentId")
            return fragment
        }
    }
}