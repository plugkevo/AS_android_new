package com.example.africanshipping25

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class enter_store_goods : Fragment() {

    private lateinit var goodsNameSpinner: Spinner
    private lateinit var storeLocationSpinner: Spinner
    private lateinit var quantityEditText: TextInputEditText
    private lateinit var saveButton: Button
    private val firestore = FirebaseFirestore.getInstance()
    private var currentShipmentId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            currentShipmentId = it.getString("shipmentId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_enter_store_goods, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        goodsNameSpinner = view.findViewById(R.id.goodsNameSpinner)
        storeLocationSpinner = view.findViewById(R.id.storeLocationSpinner)
        quantityEditText = view.findViewById(R.id.quantityEditText)
        saveButton = view.findViewById(R.id.saveButton)

        // Options for the goods name spinner
        val goodsOptions = arrayOf("Box","Electronics", "Toiletries", "Books", "Furniture", "Other")
        val goodsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, goodsOptions)
        goodsNameSpinner.adapter = goodsAdapter

        // Options for the store location spinner
        val storeLocations = arrayOf("Store A", "Store B", "Store C")
        val locationAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, storeLocations)
        storeLocationSpinner.adapter = locationAdapter

        saveButton.setOnClickListener {
            currentShipmentId?.let { shipmentId ->
                saveGoodsToStore(shipmentId)
            } ?: run {
                Toast.makeText(requireContext(), "Error: Shipment ID not available.", Toast.LENGTH_SHORT).show()
                Log.e("enter_store_goods", "Shipment ID is null.")
            }
        }
    }

    private fun saveGoodsToStore(shipmentId: String) {
        val goodsName = goodsNameSpinner.selectedItem.toString()
        val storeLocation = storeLocationSpinner.selectedItem.toString()
        val quantityString = quantityEditText.text.toString().trim()

        if (quantityString.isEmpty()) {
            quantityEditText.error = "Please enter the quantity"
            return
        }

        val quantity = quantityString.toIntOrNull()
        if (quantity == null || quantity <= 0) {
            quantityEditText.error = "Please enter a valid positive quantity"
            return
        }

        val itemData = hashMapOf(
            "name" to goodsName,
            "storeLocation" to storeLocation, // Changed "location" to "storeLocation"
            "goodsNumber" to quantity
        )

        firestore.collection("shipments")
            .document(shipmentId)
            .collection("store_inventory") // Using "store_inventory" as the subcollection name
            .add(itemData)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(
                    requireContext(),
                    "Goods added to store inventory for shipment $shipmentId with ID: ${documentReference.id}",
                    Toast.LENGTH_SHORT
                ).show()
                quantityEditText.text = null
                goodsNameSpinner.setSelection(0)
                storeLocationSpinner.setSelection(0)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error adding goods to store inventory: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e("FirestoreError", "Error adding store goods for shipment $shipmentId", e)
            }
    }


    companion object {
        fun newInstance(shipmentId: String): enter_store_goods {
            val fragment = enter_store_goods()
            val args = Bundle()
            args.putString("shipmentId", shipmentId)
            fragment.arguments = args
            return fragment
        }
    }
}