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

data class StoreGood(var goodsNumber: Long? = null, var name: String? = null, var storeLocation: String? = null)

class view_store_goods : Fragment() {

    private lateinit var storeInventoryRecyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var storeGoodsAdapter: StoreGoodsAdapter
    private lateinit var db: FirebaseFirestore
    private var currentShipmentId: String? = null
    private lateinit var searchEditText: EditText

    // Declare Lottie animations
    private lateinit var lottieLoadingAnimation: LottieAnimationView
    private lateinit var lottieNoDataAnimation: LottieAnimationView

    private var allStoreGoods: List<StoreGood> = listOf()

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

        // Initialize Lottie animations
        lottieLoadingAnimation = view.findViewById(R.id.lottie_loading_animation)
        lottieNoDataAnimation = view.findViewById(R.id.lottie_no_data_animation)

        storeInventoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        storeInventoryRecyclerView.setHasFixedSize(true)

        storeGoodsAdapter = StoreGoodsAdapter(mutableListOf()) { storeGood ->
            showGoodsDetailsDialog(storeGood)
        }
        storeInventoryRecyclerView.adapter = storeGoodsAdapter

        loadStoreInventory()
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // Ensure the string is not null for filtering
                filterStoreGoods(s?.toString() ?: "")
            }
        })
    }

    private fun loadStoreInventory() {
        if (currentShipmentId == null) {
            Log.e("view_store_goods", "Shipment ID is null")
            emptyView.text = "Error: Shipment ID is missing."
            emptyView.visibility = View.VISIBLE

            // Hide all Lotties and RecyclerView
            lottieLoadingAnimation.visibility = View.GONE
            lottieLoadingAnimation.cancelAnimation()
            lottieNoDataAnimation.visibility = View.GONE
            lottieNoDataAnimation.cancelAnimation()
            storeInventoryRecyclerView.visibility = View.GONE

            return
        }

        Log.d("ViewStoreGoodsFragment", "loadStoreInventory: shipmentId = $currentShipmentId")

        // Show loading Lottie, hide everything else
        lottieLoadingAnimation.visibility = View.VISIBLE
        lottieLoadingAnimation.playAnimation()
        storeInventoryRecyclerView.visibility = View.GONE
        emptyView.visibility = View.GONE
        lottieNoDataAnimation.visibility = View.GONE
        lottieNoDataAnimation.cancelAnimation() // Ensure no-data Lottie is stopped

        db.collection("shipments")
            .document(currentShipmentId!!)
            .collection("store_inventory")
            .get()
            .addOnSuccessListener { querySnapshot ->
                lottieLoadingAnimation.cancelAnimation() // Stop loading animation when fetch completes
                lottieLoadingAnimation.visibility = View.GONE // Hide loading Lottie

                handleStoreInventoryData(querySnapshot)
            }
            .addOnFailureListener { e ->
                Log.e("ViewStoreGoodsFragment", "Error getting store inventory: ", e)

                lottieLoadingAnimation.cancelAnimation() // Stop loading animation
                lottieLoadingAnimation.visibility = View.GONE // Hide loading Lottie

                emptyView.text = "Error loading data: ${e.message}"
                emptyView.visibility = View.VISIBLE
                storeInventoryRecyclerView.visibility = View.GONE
                lottieNoDataAnimation.visibility = View.GONE // Ensure no data Lottie is hidden on general error
                lottieNoDataAnimation.cancelAnimation()
            }
    }

    private fun handleStoreInventoryData(querySnapshot: com.google.firebase.firestore.QuerySnapshot) {
        if (querySnapshot.isEmpty) {
            emptyView.text = "No items in store inventory." // Set original no data message
            emptyView.visibility = View.VISIBLE
            storeInventoryRecyclerView.visibility = View.GONE
            allStoreGoods = listOf() // Clear the allStoreGoods list
            storeGoodsAdapter.updateData(mutableListOf()) // Clear adapter as well

            // Show no data Lottie
            lottieNoDataAnimation.visibility = View.VISIBLE
            lottieNoDataAnimation.playAnimation()
        } else {
            emptyView.visibility = View.GONE
            storeInventoryRecyclerView.visibility = View.VISIBLE
            val storeGoodsList = querySnapshot.toObjects<StoreGood>()
            Log.d("ViewStoreGoodsFragment", "handleStoreInventoryData: storeGoodsList size = ${storeGoodsList.size}")
            allStoreGoods = storeGoodsList
            storeGoodsAdapter.updateData(storeGoodsList)

            // Hide no data Lottie if data is present
            lottieNoDataAnimation.visibility = View.GONE
            lottieNoDataAnimation.cancelAnimation()
        }
    }
    private fun filterStoreGoods(query: String) {
        val filteredList = if (query.isBlank()) {
            allStoreGoods // If query is empty, show all items
        } else {
            val lowerCaseQuery = query.lowercase(Locale.getDefault())
            allStoreGoods.filter { storeGood ->
                storeGood.name?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true ||
                        storeGood.goodsNumber?.toString()?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true ||
                        storeGood.storeLocation?.lowercase(Locale.getDefault())?.contains(lowerCaseQuery) == true
            }
        }
        storeGoodsAdapter.updateData(filteredList.toMutableList()) // Update adapter with filtered list

        if (filteredList.isEmpty()) {
            emptyView.text = if (query.isBlank()) {
                "No items in store inventory." // If search is empty and original list is empty
            } else {
                "No matching items found." // If search has query but no matches
            }
            emptyView.visibility = View.VISIBLE
            storeInventoryRecyclerView.visibility = View.GONE
            lottieNoDataAnimation.visibility = View.VISIBLE // Show no data Lottie for empty filtered list
            lottieNoDataAnimation.playAnimation()
        } else {
            emptyView.visibility = View.GONE
            storeInventoryRecyclerView.visibility = View.VISIBLE
            lottieNoDataAnimation.visibility = View.GONE // Hide no data Lottie
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

        // Sample option arrays — you can load these from Firestore if needed
        val goodsNameOptions = arrayOf("Box","Furniture","Electronics", "Toiletries","Tote/Barrel", "Machinery","Other")
        val locationOptions = arrayOf("Store A","Store B", "Store C")

        var selectedGoodsName = storeGood.name
        var selectedLocation = storeGood.storeLocation

        // Set initial data
        goodsNumberEditText.setText(storeGood.goodsNumber?.toString())
        goodsNameTextView.text = "Name: ${selectedGoodsName ?: "N/A"}" // Handle null case
        storeLocationTextView.text = "Location: ${selectedLocation ?: "N/A"}" // Handle null case


        // Goods Name selection dialog
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

        // Store Location selection dialog
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

        // Update button click
        updateButton.setOnClickListener {
            val newNumberString = goodsNumberEditText.text.toString().trim() // Trim whitespace

            // *** ADD VALIDATION HERE ***
            if (newNumberString.isEmpty()) {
                Toast.makeText(requireContext(), "Goods number cannot be empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Stop execution
            }

            if (newNumberString.length != 4) {
                Toast.makeText(requireContext(), "Goods number must be 4 characters.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // Stop execution
            }

            val newNumber = newNumberString.toLongOrNull()
            if (newNumber == null) {
                // This case should ideally be caught by the length check if inputType is number,
                // but keep for robustness against non-numeric input if inputType changes.
                Toast.makeText(requireContext(), "Invalid goods number (must be numeric).", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // *** END VALIDATION ***

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
                                loadStoreInventory() // Reload data to reflect changes
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