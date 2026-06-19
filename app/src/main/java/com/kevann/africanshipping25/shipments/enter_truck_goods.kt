package com.kevann.africanshipping25.shipments

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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
    var goodsNumbers: List<String> = emptyList()
)

class enter_truck_goods : Fragment() {

    private lateinit var goodsNameSpinner: Spinner
    private lateinit var goodsNumberFieldsContainer: LinearLayout
    private lateinit var buttonAddGoodNo: ImageButton
    private lateinit var addButton: Button
    private val firestore = FirebaseFirestore.getInstance()
    private var currentShipmentId: String? = null
    private val goodsOptions = arrayOf("Box","Furniture","Electronics", "Toiletries","Tote/Barrel", "Machinery","Other")
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var translationManager: GoogleTranslationManager
    private lateinit var translationHelper: GoogleTranslationHelper

    // List to hold references to all dynamically added goods number input fields and their parent TextInputLayouts
    private val goodsNumberInputLayouts: MutableList<TextInputLayout> = mutableListOf()
    private val goodsNumberEditTexts: MutableList<TextInputEditText> = mutableListOf()

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
        goodsNumberFieldsContainer = view.findViewById(R.id.goodsNumberFieldsContainer)
        buttonAddGoodNo = view.findViewById(R.id.buttonAddGoodNo)
        addButton = view.findViewById(R.id.saveButton)

        // Add the initial goods number input field
        addGoodsNumberInputField()

        // Setup Add Good Number Button Click Listener
        buttonAddGoodNo.setOnClickListener {
            addGoodsNumberInputField()
        }

        addButton.setOnClickListener {
            addGoodsToShipment()
        }

        // Translate UI elements
        val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
        translateUIElements(currentLanguage)
        translateSpinnerItems(currentLanguage)
    }

    /**
     * Dynamically adds a new TextInputEditText for a goods number.
     */
    private fun addGoodsNumberInputField() {
        // Create TextInputLayout with the OutlinedBox style
        val textInputLayout = TextInputLayout(
            requireContext(),
            null,
            com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox
        ).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, resources.getDimensionPixelSize(com.kevann.africanshipping25.R.dimen.margin_small))
            }
        }

        val textInputEditText = TextInputEditText(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        textInputLayout.addView(textInputEditText)
        goodsNumberFieldsContainer.addView(textInputLayout)
        goodsNumberInputLayouts.add(textInputLayout)
        goodsNumberEditTexts.add(textInputEditText)
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
        if (currentShipmentId == null) {
            val errorMsg = "Error: Shipment ID not available."
            showTranslatedToast(errorMsg)
            Log.e("enter_truck_goods", "Shipment ID is null.")
            return
        }

        val goodsName = goodsNameSpinner.selectedItem.toString()
        val goodsNumbersList = mutableListOf<String>()
        var hasValidationErrors = false

        // Collect and validate all goods numbers
        for (i in goodsNumberEditTexts.indices) {
            val goodNo = goodsNumberEditTexts[i].text.toString().trim()
            
            if (goodNo.isEmpty()) {
                goodsNumberInputLayouts[i].error = "Good Number cannot be empty"
                hasValidationErrors = true
            } else if (goodNo.length != 4) {
                goodsNumberInputLayouts[i].error = "Good Number must be 4 characters"
                hasValidationErrors = true
            } else {
                goodsNumbersList.add(goodNo)
            }
        }

        if (hasValidationErrors) {
            val errorMsg = "Please correct the errors in the Good Numbers"
            showTranslatedToast(errorMsg)
            return
        }

        if (goodsNumbersList.isEmpty()) {
            val errorMsg = "Please enter at least one goods number"
            showTranslatedToast(errorMsg)
            return
        }

        val newTruckGood = TruckGoodInput(
            name = goodsName, 
            goodsNumbers = goodsNumbersList
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
        Log.d("enter_truck_goods", "[v0] Starting saveToFirestore with shipmentId: $shipmentId, goodsName: ${good.name}, goodsNumbers: ${good.goodsNumbers}")
        
        // First, verify the shipment document exists
        firestore.collection("shipments").document(shipmentId).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    Log.d("enter_truck_goods", "[v0] Shipment document exists, proceeding with save")
                    // Create a map to properly serialize data for Firestore
                    val goodsMap = hashMapOf(
                        "name" to (good.name ?: ""),
                        "goodsNumbers" to good.goodsNumbers,
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
        // Save each good number as a separate entity or combined
        val goodsNumbersCombined = good.goodsNumbers.joinToString(",")
        
        val truckGoodsEntity = TruckGoodsEntity(
            shipmentId = currentShipmentId!!,
            name = good.name ?: "",
            goodsNumber = goodsNumbersCombined,
            isSynced = false
        )

        OfflineDataStore.saveTruckGood(truckGoodsEntity, requireContext())
        val localSaveMsg = "Item saved locally (will sync when online)"
        showTranslatedToast(localSaveMsg)
        clearFields()
    }

    private fun clearFields() {
        // Clear all goods number fields
        goodsNumberFieldsContainer.removeAllViews()
        goodsNumberInputLayouts.clear()
        goodsNumberEditTexts.clear()
        
        // Add initial empty field
        addGoodsNumberInputField()
        
        // Reset spinner
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
