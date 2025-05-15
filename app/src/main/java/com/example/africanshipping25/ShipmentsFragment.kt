package com.example.africanshipping25

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore

class ShipmentsFragment : Fragment(), OnShipmentUpdateListener, ShipmentAdapter.OnShipmentItemClickListener {

    private lateinit var rvAllShipments: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var shipmentAdapter: ShipmentAdapter
    private val allShipmentsList = mutableListOf<Shipment>()
    private val filteredShipmentsList = mutableListOf<Shipment>()
    private val firestore = FirebaseFirestore.getInstance()

    private val statusOptions = arrayOf("Active", "In Transit", "Delivered", "Processing")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_shipments, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvAllShipments = view.findViewById(R.id.rv_all_shipments)
        etSearch = view.findViewById(R.id.et_search)
        chipGroup = view.findViewById(R.id.chip_group)

        rvAllShipments.layoutManager = LinearLayoutManager(requireContext())
        shipmentAdapter = ShipmentAdapter(filteredShipmentsList, this, this) // Pass 'this' as the itemClickListener
        rvAllShipments.adapter = shipmentAdapter

        fetchShipments()
        setupSearchAndFilters()
    }

    private fun fetchShipments() {
        firestore.collection("shipments")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    allShipmentsList.clear()
                    for (document in task.result!!) {
                        val shipment = document.toObject(Shipment::class.java).apply {
                            id = document.id
                        }
                        Log.d("ShipmentStatus", "Status: ${shipment.status}") // Add this line
                        allShipmentsList.add(shipment)
                    }
                    filteredShipmentsList.addAll(allShipmentsList)
                    shipmentAdapter.notifyDataSetChanged()
                } else {
                    Log.w("AllShipmentsFragment", "Error getting documents.", task.exception)
                    // Handle the error appropriately, e.g., display a toast
                }
            }
    }

    private fun setupSearchAndFilters() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterShipments(s.toString(), getSelectedFilter())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val selectedFilter = getSelectedFilter()
            filterShipments(etSearch.text.toString(), selectedFilter)
        }
    }

    private fun getSelectedFilter(): String {
        return when (chipGroup.checkedChipId) {
            R.id.chip_active -> "Active" // Match the status options
            R.id.chip_in_transit -> "In Transit"
            R.id.chip_delivered -> "Delivered"
            R.id.chip_processing -> "Processing"
            else -> "all"
        }
    }

    private fun filterShipments(searchText: String, filter: String) {
        filteredShipmentsList.clear()
        val lowerCaseSearchText = searchText.lowercase()

        for (shipment in allShipmentsList) {
            val matchesSearch =
                shipment.name.lowercase().contains(lowerCaseSearchText) ||
                        shipment.origin.lowercase().contains(lowerCaseSearchText) ||
                        shipment.destination.lowercase().contains(lowerCaseSearchText)

            val matchesFilter = if (filter == "all") {
                true
            } else {
                shipment.status == filter // Direct case-sensitive comparison
            }

            if (matchesSearch && matchesFilter) {
                filteredShipmentsList.add(shipment)
            }
        }
        shipmentAdapter.notifyDataSetChanged()
    }

    override fun onUpdateShipment(shipment: Shipment) {
        showUpdateShipmentDialog(shipment)
    }

    private fun showUpdateShipmentDialog(shipment: Shipment) {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_update_shipment, null)
        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.show()

        val nameEditText = dialogView.findViewById<EditText>(R.id.et_name)
        val originEditText = dialogView.findViewById<EditText>(R.id.et_origin)
        val destinationEditText = dialogView.findViewById<EditText>(R.id.et_destination)
        val detailsEditText = dialogView.findViewById<EditText>(R.id.et_details)
        val statusSpinner = dialogView.findViewById<Spinner>(R.id.spinner_status)
        val updateButton = dialogView.findViewById<Button>(R.id.btn_update)
        val cancelButton = dialogView.findViewById<Button>(R.id.btn_cancel)


        // Populate the spinner
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, statusOptions)
        statusSpinner.adapter = adapter

        // Set the initial selection of the spinner
        shipment.status?.let { status ->
            val index = statusOptions.indexOf(status)
            if (index != -1) {
                statusSpinner.setSelection(index)
            }
        }

        // Populate other fields
        nameEditText.setText(shipment.name)
        originEditText.setText(shipment.origin)
        destinationEditText.setText(shipment.destination)
        detailsEditText.setText(shipment.details)

        updateButton.setOnClickListener {
            val updatedName = nameEditText.text.toString().trim()
            val updatedOrigin = originEditText.text.toString().trim()
            val updatedDestination = destinationEditText.text.toString().trim()
            val updatedDetails = detailsEditText.text.toString().trim()
            val updatedStatus = statusSpinner.selectedItem.toString() // Get selected status

            if (updatedName.isEmpty() || updatedOrigin.isEmpty() || updatedDestination.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all the name, origin, and destination", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedShipment = hashMapOf(
                "name" to updatedName,
                "origin" to updatedOrigin,
                "destination" to updatedDestination,
                "details" to updatedDetails,
                "status" to updatedStatus
                // Add other fields you want to update
            )

            firestore.collection("shipments").document(shipment.id)
                .update(updatedShipment as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Shipment updated successfully", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    fetchShipments() // Refresh the list
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error updating shipment: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
    }

    override fun onShipmentItemClick(shipment: Shipment) {
        // Handle item click to navigate to another activity
        val intent = Intent(requireContext(), ViewShipment::class.java)
        intent.putExtra("shipmentId", shipment.id) // It's a good practice to pass the ID
        intent.putExtra("shipmentName", shipment.name)
        intent.putExtra("shipmentOrigin", shipment.origin)
        intent.putExtra("shipmentDestination", shipment.destination)
        intent.putExtra("shipmentStatus", shipment.status)
        intent.putExtra("shipmentDate", shipment.date)
        startActivity(intent)
    }
}