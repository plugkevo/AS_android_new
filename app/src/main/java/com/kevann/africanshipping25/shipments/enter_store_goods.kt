package com.kevann.africanshipping25.shipments


import android.content.Context
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
import com.kevann.africanshipping25.R  // Add this import


class enter_store_goods : Fragment() {

    private lateinit var goodsNameSpinner: Spinner
    private lateinit var storeLocationSpinner: Spinner
    private lateinit var goodsNumberEditText: TextInputEditText
    private lateinit var saveButton: Button
    private val firestore = FirebaseFirestore.getInstance()
    private var currentShipmentId: String? = null

    // Key for SharedPreferences
    private val PREFS_NAME = "StoreGoodsPrefs"
    private val LAST_STORE_KEY = "lastSelectedStore"

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
        goodsNumberEditText = view.findViewById(R.id.etgoodsNumber)
        saveButton = view.findViewById(R.id.saveButton)

        // Options for the goods name spinner
        val goodsOptions = arrayOf("Box","Furniture","Electronics", "Toiletries","Tote/Barrel", "Machinery","Other")
        val goodsAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, goodsOptions)
        goodsNameSpinner.adapter = goodsAdapter

        // Options for the store location spinner
        val storeLocations = arrayOf("Store A", "Store B", "Store C")
        val locationAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, storeLocations)
        storeLocationSpinner.adapter = locationAdapter

        // --- NEW: Set last selected store location ---
        val sharedPrefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSelectedStore = sharedPrefs.getString(LAST_STORE_KEY, storeLocations[0]) // Default to first item
        val lastSelectedIndex = storeLocations.indexOf(lastSelectedStore)
        if (lastSelectedIndex != -1) {
            storeLocationSpinner.setSelection(lastSelectedIndex)
        }
        // --- END NEW ---

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
        val goodsnostring = goodsNumberEditText.text.toString().trim()

        if (goodsnostring.isEmpty()) {
            goodsNumberEditText.error = "Please enter the Goods Number"
            return
        }

        val quantity = goodsnostring.toIntOrNull()
        if (quantity == null || quantity <= 0) {
            goodsNumberEditText.error = "Please enter a valid positive quantity"
            return
        }

        if(goodsnostring.length !=4){
            goodsNumberEditText.error = "Goods Number must be 4 characters"
            return
        }

        val itemData = hashMapOf(
            "name" to goodsName,
            "storeLocation" to storeLocation,
            "goodsNumber" to quantity
        )

        firestore.collection("shipments")
            .document(shipmentId)
            .collection("store_inventory")
            .add(itemData)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(
                    requireContext(),
                    "Goods added to store inventory for shipment $shipmentId with ID: ${documentReference.id}",
                    Toast.LENGTH_SHORT
                ).show()

                // --- NEW: Save the currently selected store location ---
                val sharedPrefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                with(sharedPrefs.edit()) {
                    putString(LAST_STORE_KEY, storeLocation)
                    apply()
                }
                // --- END NEW ---

                // Only reset goods name and quantity, keep store location as is
                goodsNumberEditText.text = null
                goodsNameSpinner.setSelection(0)
                // storeLocationSpinner.setSelection(0) // <<< REMOVE THIS LINE to prevent resetting
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