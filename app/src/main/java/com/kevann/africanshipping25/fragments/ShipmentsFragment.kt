package com.kevann.africanshipping25.fragments


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
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore
import com.airbnb.lottie.LottieAnimationView
import com.kevann.africanshipping25.shipments.Shipment
import com.kevann.africanshipping25.shipments.ShipmentAdapter
import com.kevann.africanshipping25.shipments.ViewShipment
import com.kevann.africanshipping25.R  // Add this import

interface OnShipmentUpdateListener {
    fun onUpdateShipment(shipment: Shipment)
}

class ShipmentsFragment : Fragment(), OnShipmentUpdateListener, ShipmentAdapter.OnShipmentItemClickListener {

    private lateinit var rvAllShipments: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var shipmentAdapter: ShipmentAdapter
    private lateinit var lottieLoadingAnimation: LottieAnimationView // For loading state
    private lateinit var lottieNoDataAnimation: LottieAnimationView // For no data state
    private lateinit var tvNoDataMessage: TextView
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
        lottieLoadingAnimation = view.findViewById(R.id.lottie_loading_animation)
        lottieNoDataAnimation = view.findViewById(R.id.lottie_no_data_animation) // Initialize no data Lottie
        tvNoDataMessage = view.findViewById(R.id.tv_no_data_message)

        rvAllShipments.layoutManager = LinearLayoutManager(requireContext())
        shipmentAdapter = ShipmentAdapter(filteredShipmentsList, this, this)
        rvAllShipments.adapter = shipmentAdapter

        fetchShipments()
        setupSearchAndFilters()
    }

    private fun fetchShipments() {
        // Show loading animation, hide others
        lottieLoadingAnimation.visibility = View.VISIBLE
        lottieLoadingAnimation.playAnimation()
        lottieNoDataAnimation.visibility = View.GONE
        lottieNoDataAnimation.cancelAnimation()
        tvNoDataMessage.visibility = View.GONE
        rvAllShipments.visibility = View.GONE

        firestore.collection("shipments")
            .get()
            .addOnCompleteListener { task ->
                lottieLoadingAnimation.cancelAnimation() // Stop loading animation when fetch completes

                if (task.isSuccessful) {
                    allShipmentsList.clear()
                    for (document in task.result!!) {
                        val shipment = document.toObject(Shipment::class.java).apply {
                            id = document.id
                        }
                        Log.d("ShipmentStatus", "Status: ${shipment.status}")
                        allShipmentsList.add(shipment)
                    }

                    filteredShipmentsList.clear()
                    filteredShipmentsList.addAll(allShipmentsList)
                    shipmentAdapter.notifyDataSetChanged()

                    if (allShipmentsList.isEmpty()) {
                        // If no data in DB, show no data Lottie and message
                        lottieNoDataAnimation.visibility = View.VISIBLE
                        lottieNoDataAnimation.playAnimation()
                        tvNoDataMessage.visibility = View.VISIBLE
                        tvNoDataMessage.text = "No shipments found in the database." // Initial no data message
                        rvAllShipments.visibility = View.GONE
                    } else {
                        // Data exists, hide Lottie and message, show RecyclerView
                        lottieNoDataAnimation.visibility = View.GONE
                        lottieNoDataAnimation.cancelAnimation()
                        tvNoDataMessage.visibility = View.GONE
                        rvAllShipments.visibility = View.VISIBLE
                    }
                } else {
                    Log.w("AllShipmentsFragment", "Error getting documents.", task.exception)
                    Toast.makeText(requireContext(), "Error loading shipments: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    // On error, show a message indicating failure
                    lottieNoDataAnimation.visibility = View.GONE // No specific error Lottie, so hide it
                    lottieNoDataAnimation.cancelAnimation()
                    tvNoDataMessage.text = "Failed to load shipments. Please try again." // Specific error message
                    tvNoDataMessage.visibility = View.VISIBLE
                    rvAllShipments.visibility = View.GONE
                }
                // Always hide loading animation here, regardless of success/failure
                lottieLoadingAnimation.visibility = View.GONE
            }
    }

    private fun setupSearchAndFilters() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Corrected call: Specify the outer class
                this@ShipmentsFragment.filterShipments(s.toString(), this@ShipmentsFragment.getSelectedFilter())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            // Corrected call: Specify the outer class
            val selectedFilter = this@ShipmentsFragment.getSelectedFilter()
            this@ShipmentsFragment.filterShipments(etSearch.text.toString(), selectedFilter)
        }
    }

    private fun getSelectedFilter(): String {
        return when (chipGroup.checkedChipId) {
            R.id.chip_active -> "Active"
            R.id.chip_in_transit -> "In Transit"
            R.id.chip_delivered -> "Delivered"
            R.id.chip_processing -> "Processing"
            else -> "all"
        }
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
        val latitudeEditText = dialogView.findViewById<EditText>(R.id.et_latitude)
        val longitudeEditText = dialogView.findViewById<EditText>(R.id.et_longitude)
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
        shipment.latitude?.let { latitudeEditText.setText(it.toString()) }
        shipment.longitude?.let { longitudeEditText.setText(it.toString()) }

        updateButton.setOnClickListener {
            val updatedName = nameEditText.text.toString().trim()
            val updatedOrigin = originEditText.text.toString().trim()
            val updatedDestination = destinationEditText.text.toString().trim()
            val updatedDetails = detailsEditText.text.toString().trim()
            val updatedStatus = statusSpinner.selectedItem.toString()
            val updatedLatitude = latitudeEditText.text.toString().toDoubleOrNull()
            val updatedLongitude = longitudeEditText.text.toString().toDoubleOrNull()

            if (updatedName.isEmpty() || updatedOrigin.isEmpty() || updatedDestination.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all the name, origin, and destination", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedShipment = hashMapOf(
                "name" to updatedName,
                "origin" to updatedOrigin,
                "destination" to updatedDestination,
                "details" to updatedDetails,
                "status" to updatedStatus,
                "latitude" to updatedLatitude,
                "longitude" to updatedLongitude
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

        Toast.makeText(requireContext(), "Clicked Shipment : ${shipment.name}", Toast.LENGTH_SHORT).show()

        val intent = Intent(requireContext(), ViewShipment::class.java)
        intent.putExtra("shipmentId", shipment.id)
        intent.putExtra("shipmentName", shipment.name)
        intent.putExtra("shipmentOrigin", shipment.origin)
        intent.putExtra("shipmentDestination", shipment.destination)
        intent.putExtra("shipmentStatus", shipment.status)
        intent.putExtra("shipmentDate", shipment.date)
        shipment.createdAt?.let {
            intent.putExtra("shipmentCreatedAtMillis", it.time)
        }
        shipment.latitude?.let { intent.putExtra("shipmentLatitude", it) }
        shipment.longitude?.let { intent.putExtra("shipmentLongitude", it) }
        startActivity(intent)
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
                shipment.status == filter
            }

            if (matchesSearch && matchesFilter) {
                filteredShipmentsList.add(shipment)
            }
        }
        shipmentAdapter.notifyDataSetChanged()

        // Update visibility based on filtered results
        if (filteredShipmentsList.isEmpty()) {
            lottieNoDataAnimation.visibility = View.VISIBLE
            lottieNoDataAnimation.playAnimation()
            tvNoDataMessage.visibility = View.VISIBLE
            tvNoDataMessage.text = "No matching shipments found." // Message for filtered empty state
            rvAllShipments.visibility = View.GONE
        } else {
            lottieNoDataAnimation.visibility = View.GONE
            lottieNoDataAnimation.cancelAnimation()
            tvNoDataMessage.visibility = View.GONE
            tvNoDataMessage.text = "No shipments found in the database." // Reset to default for next potential no data
            rvAllShipments.visibility = View.VISIBLE
        }
    }
}
