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

class enter_truck_goods : Fragment() {

    private lateinit var goodsNameSpinner: Spinner
    private lateinit var quantityEditText: TextInputEditText
    private lateinit var addButton: Button // Changed from saveButton to addButton
    private val firestore = FirebaseFirestore.getInstance()
    private var currentShipmentId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retrieve the shipmentId passed as an argument
        arguments?.let {
            currentShipmentId = it.getString("shipmentId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_enter_truck_goods, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        goodsNameSpinner = view.findViewById(R.id.goodsNameSpinner)
        quantityEditText = view.findViewById(R.id.quantityEditText)
        addButton = view.findViewById(R.id.saveButton) // Assuming your button ID is still saveButton, rename if needed

        // Options for the goods name spinner
        val goodsOptions = arrayOf("Electronics", "Clothing", "Food Items", "Books", "Furniture", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, goodsOptions)
        goodsNameSpinner.adapter = adapter

        addButton.setOnClickListener {
            currentShipmentId?.let { shipmentId ->
                saveGoodsToShipment(shipmentId)
            } ?: run {
                Toast.makeText(requireContext(), "Error: Shipment ID not available.", Toast.LENGTH_SHORT).show()
                Log.e("enter_truck_goods", "Shipment ID is null.")
                // Optionally, disable the add button or handle this error differently
            }
        }
    }

    private fun saveGoodsToShipment(shipmentId: String) {
        val goodsName = goodsNameSpinner.selectedItem.toString()
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
            "quantity" to quantity
        )

        firestore.collection("shipments")
            .document(shipmentId)
            .collection("offloaded goods")
            .add(itemData)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(requireContext(), "Item added to shipment $shipmentId with ID: ${documentReference.id}", Toast.LENGTH_SHORT).show()
                // Clear fields
                quantityEditText.text = null
                goodsNameSpinner.setSelection(0)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error adding item to shipment: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("FirestoreError", "Error adding item to shipment $shipmentId", e)
            }
    }

    companion object {
        // Factory method to create a new instance of the fragment with the shipmentId
        fun newInstance(shipmentId: String): enter_truck_goods {
            val fragment = enter_truck_goods()
            val args = Bundle()
            args.putString("shipmentId", shipmentId)
            fragment.arguments = args
            return fragment
        }
    }
}