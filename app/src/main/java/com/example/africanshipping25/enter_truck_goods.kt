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
// No need to import TextInputLayout if it's not directly referenced for errors

import com.google.firebase.firestore.FirebaseFirestore

// Data class to represent the data we are saving
data class TruckGoodInput(
    var name: String? = null,
    var goodsNumber: String? = null // Changed to String
)

class enter_truck_goods : Fragment() {

    private lateinit var goodsNameSpinner: Spinner
    private lateinit var goodsNumber: TextInputEditText
    // Removed lateinit var quantityInputLayout: TextInputLayout

    private lateinit var addButton: Button
    private val firestore = FirebaseFirestore.getInstance()
    private var currentShipmentId: String? = null
    private val goodsOptions =arrayOf("Box","Furniture","Electronics", "Toiletries","Tote/Barrel", "Machinery","Other")

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
        return inflater.inflate(R.layout.fragment_enter_truck_goods, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        goodsNameSpinner = view.findViewById(R.id.goodsNameSpinner)
        goodsNumber = view.findViewById(R.id.etgoodsNumber)
        // No longer need to find quantityInputLayout here
        addButton = view.findViewById(R.id.saveButton)

        // Options for the goods name spinner
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, goodsOptions)
        goodsNameSpinner.adapter = adapter

        addButton.setOnClickListener {
            addGoodsToShipment()
        }
    }

    private fun addGoodsToShipment() {
        // Clear any previous errors on the TextInputEditText
        goodsNumber.error = null

        if (currentShipmentId == null) {
            Toast.makeText(requireContext(), "Error: Shipment ID not available.", Toast.LENGTH_SHORT).show()
            Log.e("enter_truck_goods", "Shipment ID is null.")
            return
        }

        val goodsName = goodsNameSpinner.selectedItem.toString()
        val goodsNumberString = goodsNumber.text.toString().trim() // Renamed for clarity

        // --- Validation Logic for Goods Number (4 characters) ---
        if (goodsNumberString.isEmpty()) {
            goodsNumber.error = "Please enter the goods number"
            return // Stop execution if validation fails
        }

        if (goodsNumberString.length != 4) {
            goodsNumber.error = "Goods number must be 4 characters"
            return // Stop execution if validation fails
        }
        // --- End Validation Logic ---

        // If validation passes, proceed to save
        val newTruckGood = TruckGoodInput(name = goodsName, goodsNumber = goodsNumberString)

        firestore.collection("shipments")
            .document(currentShipmentId!!)
            .collection("offloaded goods")
            .add(newTruckGood)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(
                    requireContext(),
                    "Item added to shipment $currentShipmentId with ID: ${documentReference.id}",
                    Toast.LENGTH_SHORT
                ).show()
                // Clear the input field and reset spinner after successful save
                goodsNumber.text = null
                goodsNameSpinner.setSelection(0)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error adding item to shipment: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e("FirestoreError", "Error adding item to shipment $currentShipmentId", e)
            }
    }

    companion object {
        fun newInstance(shipmentId: String): enter_truck_goods {
            val fragment = enter_truck_goods()
            val args = Bundle()
            args.putString("shipmentId", shipmentId)
            fragment.arguments = args
            return fragment
        }
    }
}