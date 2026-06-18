package com.kevann.africanshipping25.shipments

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.kevann.africanshipping25.R
import com.kevann.africanshipping25.database.OfflineDataStore
import com.kevann.africanshipping25.database.TruckGoodsEntity
import com.kevann.africanshipping25.translation.GoogleTranslationManager
import com.kevann.africanshipping25.translation.GoogleTranslationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

data class TruckGoodInput(
    var name: String? = null,
    var goodsNumber: String? = null,
    var goodsNumber2: String? = null
)

class enter_truck_goods : Fragment() {

    private lateinit var goodsNameSpinner: Spinner
    private lateinit var goodsNumber: TextInputEditText
    private lateinit var goodsNumber2: TextInputEditText
    private lateinit var addButton: Button
    private val firestore = FirebaseFirestore.getInstance()
    private var currentShipmentId: String? = null
    private val goodsOptions = arrayOf("Box","Furniture","Electronics", "Toiletries","Tote/Barrel", "Machinery","Other")
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var translationManager: GoogleTranslationManager
    private lateinit var translationHelper: GoogleTranslationHelper

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

        // Initialize SharedPreferences and translation
        sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        translationManager = GoogleTranslationManager(requireContext())
        translationHelper = GoogleTranslationHelper(translationManager)

        goodsNameSpinner = view.findViewById(R.id.goodsNameSpinner)
        goodsNumber = view.findViewById(R.id.etgoodsNumber)
        goodsNumber2 = view.findViewById(R.id.etgoodsNumber2)
        addButton = view.findViewById(R.id.saveButton)

        addButton.setOnClickListener {
            addGoodsToShipment()
        }

        // Translate UI elements
        val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
        translateUIElements(currentLanguage)
        translateSpinnerItems(currentLanguage)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            networkInfo != null && networkInfo.isConnectedOrConnecting
        }
    }

    private fun addGoodsToShipment() {
        goodsNumber.error = null
        goodsNumber2.error = null

        if (currentShipmentId == null) {
            val errorMsg = "Error: Shipment ID not available."
            showTranslatedToast(errorMsg)
            Log.e("enter_truck_goods", "Shipment ID is null.")
            return
        }

        val goodsName = goodsNameSpinner.selectedItem.toString()
        val goodsNumberString = goodsNumber.text.toString().trim()
        val goodsNumber2String = goodsNumber2.text.toString().trim()

        // Validate first goods number (required)
        if (goodsNumberString.isEmpty()) {
            goodsNumber.error = "Please enter the goods number"
            return
        }

        if (goodsNumberString.length != 4) {
            goodsNumber.error = "Goods number must be 4 characters"
            return
        }

        // Validate second goods number (optional but must be 4 chars if provided)
        if (goodsNumber2String.isNotEmpty() && goodsNumber2String.length != 4) {
            goodsNumber2.error = "Goods number must be 4 characters"
            return
        }

        val newTruckGood = TruckGoodInput(
            name = goodsName, 
            goodsNumber = goodsNumberString,
            goodsNumber2 = if (goodsNumber2String.isEmpty()) null else goodsNumber2String
        )

        // Check if online
        if (isNetworkAvailable()) {
            // Save to Firestore directly
            saveToFirestore(currentShipmentId!!, newTruckGood)
        } else {
            // Save to local database
            saveToLocalDatabase(newTruckGood)
        }
    }

    private fun saveToFirestore(shipmentId: String, good: TruckGoodInput) {
        Log.d("enter_truck_goods", "[v0] Starting saveToFirestore with shipmentId: $shipmentId, goodsName: ${good.name}, goodsNumber: ${good.goodsNumber}")
        
        // First, verify the shipment document exists
        firestore.collection("shipments").document(shipmentId).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    Log.d("enter_truck_goods", "[v0] Shipment document exists, proceeding with save")
                    // Create a map to properly serialize data for Firestore
                    val goodsMap = hashMapOf(
                        "name" to (good.name ?: ""),
                        "goodsNumber" to (good.goodsNumber ?: ""),
                        "goodsNumber2" to (good.goodsNumber2 ?: ""),
                        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                    
                    Log.d("enter_truck_goods", "[v0] Attempting to add to firestore at path: shipments/$shipmentId/offloaded goods")
                    Log.d("enter_truck_goods", "[v0] Data being saved: $goodsMap")
                    
                    firestore.collection("shipments")
                        .document(shipmentId)
                        .collection("offloaded goods")
                        .add(goodsMap)
                        .addOnSuccessListener { documentReference ->
                            Log.d("enter_truck_goods", "[v0] Successfully added document with ID: ${documentReference.id}")
                            val successMsg = "Item added to truck inventory (synced to cloud)"
                            showTranslatedToast(successMsg)
                            clearFields()
                        }
                        .addOnFailureListener { e ->
                            Log.e("enter_truck_goods", "[v0] Error adding item to Firestore", e)
                            Log.e("enter_truck_goods", "[v0] Exception message: ${e.message}")
                            Log.e("enter_truck_goods", "[v0] Full stack trace:", e)
                            val errorMsg = "Error adding item: ${e.message}"
                            showTranslatedToast(errorMsg)
                        }
                } else {
                    Log.e("enter_truck_goods", "[v0] Shipment document does not exist with ID: $shipmentId")
                    val errorMsg = "Error: Shipment not found. Cannot save goods."
                    showTranslatedToast(errorMsg)
                }
            }
            .addOnFailureListener { e ->
                Log.e("enter_truck_goods", "[v0] Error checking if shipment exists", e)
                val errorMsg = "Error verifying shipment: ${e.message}"
                showTranslatedToast(errorMsg)
            }
    }

    private fun saveToLocalDatabase(good: TruckGoodInput) {
        val truckGoodsEntity = TruckGoodsEntity(
            shipmentId = currentShipmentId!!,
            name = good.name ?: "",
            goodsNumber = good.goodsNumber ?: "",
            isSynced = false
        )

        OfflineDataStore.saveTruckGood(truckGoodsEntity, requireContext())
        val localSaveMsg = "Item saved locally (will sync when online)"
        showTranslatedToast(localSaveMsg)
        clearFields()
    }

    private fun clearFields() {
        goodsNumber.text = null
        goodsNumber2.text = null
        goodsNameSpinner.setSelection(0)
    }

    // Translation method
    private fun translateUIElements(targetLanguage: String) {
        view?.let { v ->
            // Translate title
            v.findViewById<TextView>(R.id.titleTextView)?.let { tv ->
                translationHelper.translateAndSetText(tv, "Enter Truck Goods", targetLanguage)
            }

            // Translate labels
            v.findViewById<TextView>(R.id.goodsNameLabel)?.let { tv ->
                translationHelper.translateAndSetText(tv, "Goods Name:", targetLanguage)
            }

            v.findViewById<TextView>(R.id.goodsNumberLabel)?.let { tv ->
                translationHelper.translateAndSetText(tv, "Goods Number:", targetLanguage)
            }

            v.findViewById<TextView>(R.id.goodsNumber2Label)?.let { tv ->
                translationHelper.translateAndSetText(tv, "Goods Number 2 (Optional):", targetLanguage)
            }

            // Translate button
            v.findViewById<Button>(R.id.saveButton)?.let { btn ->
                translationHelper.translateAndSetText(btn, "Save to Truck Inventory", targetLanguage)
            }
        }
    }

    // Translate spinner items
    private fun translateSpinnerItems(targetLanguage: String) {
        val translatedItems = mutableListOf<String>()
        var completedCount = 0
        
        for (item in goodsOptions) {
            translationHelper.translateText(item, targetLanguage) { translatedItem ->
                translatedItems.add(translatedItem)
                completedCount++
                
                // Once all items are translated, update the adapter
                if (completedCount == goodsOptions.size) {
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, translatedItems)
                    goodsNameSpinner.adapter = adapter
                }
            }
        }
    }

    // Helper method to translate toast messages
    private fun showTranslatedToast(message: String) {
        val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
        translationHelper.translateText(message, currentLanguage) { translatedMessage ->
            Toast.makeText(context, translatedMessage, Toast.LENGTH_SHORT).show()
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
