package com.example.africanshipping25

import android.os.Bundle
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
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObjects
import com.airbnb.lottie.LottieAnimationView // Import LottieAnimationView
import java.util.Locale

// Data class to represent a truck good item
data class TruckGood(var goodsNumber: String? = null, var name: String? = null)

class view_truck_goods : Fragment() {

    private lateinit var truckInventoryRecyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var truckGoodsAdapter: TruckGoodsAdapter
    private lateinit var db: FirebaseFirestore
    private var currentShipmentId: String? = null
    private lateinit var searchEditText: EditText

    // Declare Lottie animations
    private lateinit var lottieLoadingAnimation: LottieAnimationView
    private lateinit var lottieNoDataAnimation: LottieAnimationView

    private var allTruckGoods: List<TruckGood> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            currentShipmentId = it.getString("shipmentId")
            Log.d("view_truck_goods", "onCreate: Shipment ID received: $currentShipmentId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_view_truck_goods, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()

        truckInventoryRecyclerView = view.findViewById(R.id.truckInventoryRecyclerView)
        emptyView = view.findViewById(R.id.emptyView)
        searchEditText = view.findViewById(R.id.searchEditText)

        // Initialize Lottie animations
        lottieLoadingAnimation = view.findViewById(R.id.lottie_loading_animation)
        lottieNoDataAnimation = view.findViewById(R.id.lottie_no_data_animation)

        truckInventoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        truckInventoryRecyclerView.setHasFixedSize(true)

        truckGoodsAdapter = TruckGoodsAdapter(mutableListOf()) { truckGood ->
            showGoodsDetailsDialog(truckGood)
        }
        truckInventoryRecyclerView.adapter = truckGoodsAdapter

        loadTruckInventory()

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                filterTruckGoods(s.toString())
            }
        })
    }

    private fun loadTruckInventory() {
        if (currentShipmentId == null) {
            Log.e("ViewTruckGoodsFragment", "Shipment ID is null")
            emptyView.text = "Error: Shipment ID is missing."
            emptyView.visibility = View.VISIBLE

            // Hide Lotties on this specific error
            lottieLoadingAnimation.visibility = View.GONE
            lottieLoadingAnimation.cancelAnimation()
            lottieNoDataAnimation.visibility = View.GONE
            lottieNoDataAnimation.cancelAnimation()
            truckInventoryRecyclerView.visibility = View.GONE // Ensure RV is hidden

            return
        }

        // Show loading Lottie, hide everything else
        lottieLoadingAnimation.visibility = View.VISIBLE
        lottieLoadingAnimation.playAnimation()
        truckInventoryRecyclerView.visibility = View.GONE
        emptyView.visibility = View.GONE
        lottieNoDataAnimation.visibility = View.GONE
        lottieNoDataAnimation.cancelAnimation()

        db.collection("shipments")
            .document(currentShipmentId!!)
            .collection("offloaded goods")
            .get()
            .addOnSuccessListener { querySnapshot ->
                lottieLoadingAnimation.cancelAnimation() // Stop loading animation when fetch completes
                lottieLoadingAnimation.visibility = View.GONE // Hide loading Lottie

                handleTruckInventoryData(querySnapshot)
            }
            .addOnFailureListener { e ->
                Log.e("ViewTruckGoodsFragment", "Error getting truck inventory: ", e)

                lottieLoadingAnimation.cancelAnimation() // Stop loading animation
                lottieLoadingAnimation.visibility = View.GONE // Hide loading Lottie

                emptyView.text = "Error loading data: ${e.message}"
                emptyView.visibility = View.VISIBLE
                truckInventoryRecyclerView.visibility = View.GONE
                lottieNoDataAnimation.visibility = View.GONE // Ensure no data Lottie is hidden on general error
                lottieNoDataAnimation.cancelAnimation()
            }
    }

    private fun handleTruckInventoryData(querySnapshot: QuerySnapshot) {
        if (querySnapshot.isEmpty) {
            emptyView.text = "No items in truck inventory." // Set original no data message
            emptyView.visibility = View.VISIBLE
            truckInventoryRecyclerView.visibility = View.GONE
            allTruckGoods = listOf() // Clear the allTruckGoods list
            truckGoodsAdapter.updateData(mutableListOf()) // Clear adapter as well

            // Show no data Lottie
            lottieNoDataAnimation.visibility = View.VISIBLE
            lottieNoDataAnimation.playAnimation()
        } else {
            emptyView.visibility = View.GONE
            truckInventoryRecyclerView.visibility = View.VISIBLE
            val truckGoodsList = querySnapshot.toObjects<TruckGood>()
            allTruckGoods = truckGoodsList // Store the full list
            truckGoodsAdapter.updateData(truckGoodsList.toMutableList()) // Update the adapter with the new data

            // Hide no data Lottie if data is present
            lottieNoDataAnimation.visibility = View.GONE
            lottieNoDataAnimation.cancelAnimation()
        }
    }

    private fun filterTruckGoods(query: String) {
        val filteredList = if (query.isBlank()) {
            allTruckGoods // If query is empty, show all items
        } else {
            val lowerCaseQuery = query.lowercase(Locale.getDefault())
            allTruckGoods.filter { truckGood ->
                truckGood.name?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true ||
                        truckGood.goodsNumber?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true
            }
        }
        truckGoodsAdapter.updateData(filteredList.toMutableList()) // Update adapter with filtered list

        if (filteredList.isEmpty()) {
            emptyView.text = if (query.isBlank()) {
                "No items in truck inventory." // If search is empty and original list is empty
            } else {
                "No matching items found." // If search has query but no matches
            }
            emptyView.visibility = View.VISIBLE
            truckInventoryRecyclerView.visibility = View.GONE
            lottieNoDataAnimation.visibility = View.VISIBLE // Show no data Lottie for empty filtered list
            lottieNoDataAnimation.playAnimation()
        } else {
            emptyView.visibility = View.GONE
            truckInventoryRecyclerView.visibility = View.VISIBLE
            lottieNoDataAnimation.visibility = View.GONE // Hide no data Lottie
            lottieNoDataAnimation.cancelAnimation()
        }
    }

    private fun showGoodsDetailsDialog(truckGood: TruckGood) {
        // Create an AlertDialog
        val dialogBuilder = AlertDialog.Builder(requireContext())
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_truck_goods_details, null)
        dialogBuilder.setView(dialogView)
        val dialog = dialogBuilder.create()

        // Initialize views in the dialog
        val goodsNameTextView = dialogView.findViewById<TextView>(R.id.detailGoodsNameTextView)
        val goodsNumberEditText = dialogView.findViewById<EditText>(R.id.detailGoodsNumberTextView)
        val updateButton = dialogView.findViewById<Button>(R.id.detailUpdateButton)
        val closeButton = dialogView.findViewById<Button>(R.id.detailCloseButton)

        // Set the data
        goodsNumberEditText.setText(truckGood.goodsNumber)
        val goodsNameOptions = arrayOf("Box","Furniture","Electronics", "Toiletries","Tote/Barrel", "Machinery","Other")// Your options
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
            val newNumber = goodsNumberEditText.text.toString().trim() // Trim whitespace

            // --- ADD VALIDATION HERE ---
            if (newNumber.isEmpty()) {
                Toast.makeText(requireContext(), "Goods number cannot be empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Stop execution of the click listener
            }

            if (newNumber.length != 4) {
                Toast.makeText(requireContext(), "Goods number must be 4 characters.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Stop execution of the click listener
            }
            // --- END VALIDATION ---

            Log.d("ViewTruckGoodsFragment", "Update button clicked for Name: $selectedGoodsName, Number: $newNumber")
            if (currentShipmentId != null) {
                updateTruckGoodInFirestore(currentShipmentId!!, truckGood.goodsNumber, selectedGoodsName, newNumber)
            }
            dialog.dismiss()
        }
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateTruckGoodInFirestore(shipmentId: String, oldGoodsNumber: String?, newName: String?, newNumber: String) {
        db.collection("shipments")
            .document(shipmentId)
            .collection("offloaded goods")
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