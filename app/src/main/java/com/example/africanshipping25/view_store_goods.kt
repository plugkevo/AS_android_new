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
import com.google.firebase.firestore.ktx.toObjects

data class StoreGood(var goodsNumber: Long? = null, var name: String? = null, var storeLocation: String? = null)

class view_store_goods : Fragment() {

    private lateinit var storeInventoryRecyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var storeGoodsAdapter: StoreGoodsAdapter
    private lateinit var db: FirebaseFirestore
    private var currentShipmentId: String? = null

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

        storeInventoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        storeInventoryRecyclerView.setHasFixedSize(true)

        storeGoodsAdapter = StoreGoodsAdapter(mutableListOf()) { storeGood ->
            showGoodsDetailsDialog(storeGood)
        }
        storeInventoryRecyclerView.adapter = storeGoodsAdapter

        loadStoreInventory()
    }

    private fun loadStoreInventory() {
        if (currentShipmentId == null) {
            Log.e("view_store_goods", "Shipment ID is null")
            emptyView.text = "Error: Shipment ID is missing."
            emptyView.visibility = View.VISIBLE
            return
        }

        Log.d("ViewStoreGoodsFragment", "loadStoreInventory: shipmentId = $currentShipmentId")

        db.collection("shipments")
            .document(currentShipmentId!!)
            .collection("store_inventory")
            .get()
            .addOnSuccessListener { querySnapshot ->
                Log.d("ViewStoreGoodsFragment", "loadStoreInventory: querySnapshot size = ${querySnapshot.size()}")
                handleStoreInventoryData(querySnapshot)
            }
            .addOnFailureListener { e ->
                Log.e("ViewStoreGoodsFragment", "Error getting store inventory: ", e)
                emptyView.text = "Error loading data: ${e.message}"
                emptyView.visibility = View.VISIBLE
            }
    }

    private fun handleStoreInventoryData(querySnapshot: com.google.firebase.firestore.QuerySnapshot) {
        if (querySnapshot.isEmpty) {
            emptyView.visibility = View.VISIBLE
            storeInventoryRecyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            storeInventoryRecyclerView.visibility = View.VISIBLE
            val storeGoodsList = querySnapshot.toObjects<StoreGood>()
            Log.d("ViewStoreGoodsFragment", "handleStoreInventoryData: storeGoodsList size = ${storeGoodsList.size}")
            storeGoodsAdapter.updateData(storeGoodsList)
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
        val goodsNameOptions = arrayOf("Electronics", "Clothing", "Food Items", "Books", "Furniture", "Other")
        val locationOptions = arrayOf("Nairobi", "Mombasa", "Kisumu", "Eldoret", "Thika", "Other")

        var selectedGoodsName = storeGood.name
        var selectedLocation = storeGood.storeLocation

        // Set initial data
        goodsNumberEditText.setText(storeGood.goodsNumber?.toString())
        goodsNameTextView.text = "Name: ${storeGood.name}"
        storeLocationTextView.text = "Location: ${storeGood.storeLocation}"

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
            val newNumberString = goodsNumberEditText.text.toString()
            val newNumber = newNumberString.toLongOrNull()
            if (newNumber == null) {
                Toast.makeText(requireContext(), "Invalid goods number", Toast.LENGTH_SHORT).show()
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
