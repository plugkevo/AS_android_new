package com.example.africanshipping25

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObjects

// Data class to represent a truck good item
data class TruckGood(var goodsNumber: String? = null, var name: String? = null) // Add name

class view_truck_goods : Fragment() {

    private lateinit var truckInventoryRecyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var truckGoodsAdapter: TruckGoodsAdapter
    private lateinit var db: FirebaseFirestore
    private var currentShipmentId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Get the shipmentId from arguments
        arguments?.let {
            currentShipmentId = it.getString("shipmentId")
            Log.d("view_truck_goods", "onCreate: Shipment ID received: $currentShipmentId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_view_truck_goods, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Initialize UI elements
        truckInventoryRecyclerView = view.findViewById(R.id.truckInventoryRecyclerView)
        emptyView = view.findViewById(R.id.emptyView)

        // Set layout manager for the RecyclerView
        truckInventoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        truckInventoryRecyclerView.setHasFixedSize(true) // Improve performance

        // Initialize the adapter (empty list initially)
        truckGoodsAdapter = TruckGoodsAdapter(mutableListOf()) { truckGood -> // Pass click listener
            showGoodsDetailsDialog(truckGood)
        }
        truckInventoryRecyclerView.adapter = truckGoodsAdapter

        // Load data from Firestore
        loadTruckInventory()
    }

    private fun loadTruckInventory() {
        if (currentShipmentId == null) {
            Log.e("ViewTruckGoodsFragment", "Shipment ID is null")
            emptyView.text = "Error: Shipment ID is missing."
            emptyView.visibility = View.VISIBLE
            return
        }

        // Reference the "truck_inventory" subcollection of the specific shipment
        db.collection("shipments")
            .document(currentShipmentId!!)  // Use non-null shipmentId
            .collection("offloaded goods")
            .get()
            .addOnSuccessListener { querySnapshot ->
                handleTruckInventoryData(querySnapshot)
            }
            .addOnFailureListener { e ->
                Log.e("ViewTruckGoodsFragment", "Error getting truck inventory: ", e)
                emptyView.text = "Error loading data: ${e.message}"
                emptyView.visibility = View.VISIBLE
            }
    }

    private fun handleTruckInventoryData(querySnapshot: QuerySnapshot) {
        if (querySnapshot.isEmpty) {
            emptyView.visibility = View.VISIBLE
            truckInventoryRecyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            truckInventoryRecyclerView.visibility = View.VISIBLE
            // Convert the QuerySnapshot to a List of TruckGood objects using toObjects()
            val truckGoodsList = querySnapshot.toObjects<TruckGood>()
            truckGoodsAdapter.updateData(truckGoodsList) // Update the adapter with the new data
        }
    }

    private fun showGoodsDetailsDialog(truckGood: TruckGood) {
        // Create an AlertDialog
        val dialogBuilder = AlertDialog.Builder(requireContext())
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_truck_goods_details, null)
        dialogBuilder.setView(dialogView)
        val dialog = dialogBuilder.create()

        // Initialize views in the dialog
        val goodsNameTextView = dialogView.findViewById<TextView>(R.id.detailGoodsNameTextView) // Changed to TextView
        val goodsNumberEditText = dialogView.findViewById<EditText>(R.id.detailGoodsNumberTextView)
        val updateButton = dialogView.findViewById<Button>(R.id.detailUpdateButton)
        val closeButton = dialogView.findViewById<Button>(R.id.detailCloseButton)

        // Set the data
        goodsNumberEditText.setText(truckGood.goodsNumber)
        val goodsNameOptions = arrayOf("Box","Electronics", "Toiletries", "Books", "Furniture", "Other") // Your options
        var selectedGoodsName = truckGood.name // Track selected

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
        goodsNameTextView.text = "Name: ${truckGood.name}"

        // Set click listeners for the buttons
        updateButton.setOnClickListener {
            // Handle update logic here
            val newNumber = goodsNumberEditText.text.toString()
            Log.d("ViewTruckGoodsFragment", "Update button clicked for Name: $selectedGoodsName, Number: $newNumber")
            if (currentShipmentId != null) {
                updateTruckGoodInFirestore(currentShipmentId!!, truckGood.goodsNumber, selectedGoodsName, newNumber)
            }
            dialog.dismiss() // Dismiss the dialog after handling
        }
        closeButton.setOnClickListener {
            dialog.dismiss() // Dismiss the dialog
        }

        dialog.show() // Show the dialog
    }

    private fun updateTruckGoodInFirestore(shipmentId: String, oldGoodsNumber: String?, newName: String?, newNumber: String) {
        db.collection("shipments")
            .document(shipmentId)
            .collection("offloaded goods")
            .whereEqualTo("goodsNumber", oldGoodsNumber) // Use oldGoodsNumber to find the document
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Log.d("Firestore", "No matching documents found to update")
                    Toast.makeText(requireContext(), "No matching goods found to update.", Toast.LENGTH_SHORT).show()
                } else {
                    // There should be only one matching document
                    for (document in querySnapshot) {
                        val documentId = document.id
                        db.collection("shipments")
                            .document(shipmentId)
                            .collection("offloaded goods")
                            .document(documentId)
                            .update(
                                mapOf(
                                    "name" to newName,
                                    "goodsNumber" to newNumber
                                )
                            )
                            .addOnSuccessListener {
                                Log.d("Firestore", "Document updated successfully")
                                Toast.makeText(requireContext(), "Goods updated successfully.", Toast.LENGTH_SHORT).show()
                                loadTruckInventory() // Refresh the list
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "Error updating document", e)
                                Toast.makeText(
                                    requireContext(),
                                    "Error updating goods: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error getting document to update", e)
                Toast.makeText(
                    requireContext(),
                    "Error finding goods to update: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    companion object {
        // Factory method to create a new instance of the fragment with the shipment ID
        fun newInstance(shipmentId: String): view_truck_goods {
            val fragment = view_truck_goods()
            val args = Bundle()
            args.putString("shipmentId", shipmentId)
            fragment.arguments = args
            Log.d("view_truck_goods", "newInstance: Shipment ID passed: $shipmentId")
            return fragment
        }
    }
}

