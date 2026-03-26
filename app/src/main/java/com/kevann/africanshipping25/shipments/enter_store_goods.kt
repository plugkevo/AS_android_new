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
import com.kevann.africanshipping25.database.StoreGoodsEntity
import com.kevann.africanshipping25.translation.GoogleTranslationManager
import com.kevann.africanshipping25.translation.GoogleTranslationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class enter_store_goods : Fragment() {

    private lateinit var goodsNameSpinner: Spinner
    private lateinit var storeLocationSpinner: Spinner
    private lateinit var goodsNumberEditText: TextInputEditText
    private lateinit var saveButton: Button
    private val firestore = FirebaseFirestore.getInstance()
    private var currentShipmentId: String? = null
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var translationManager: GoogleTranslationManager
    private lateinit var translationHelper: GoogleTranslationHelper

    private val PREFS_NAME = "StoreGoodsPrefs"
    private val LAST_STORE_KEY = "lastSelectedStore"

    private val goodsOptions = arrayOf("Box","Furniture","Electronics", "Toiletries","Tote/Barrel", "Machinery","Other")
    private val storeLocations = arrayOf("Store A", "Store B", "Store C")

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

        // Initialize SharedPreferences and translation
        sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        translationManager = GoogleTranslationManager(requireContext())
        translationHelper = GoogleTranslationHelper(translationManager)

        goodsNameSpinner = view.findViewById(R.id.goodsNameSpinner)
        storeLocationSpinner = view.findViewById(R.id.storeLocationSpinner)
        goodsNumberEditText = view.findViewById(R.id.etgoodsNumber)
        saveButton = view.findViewById(R.id.saveButton)

        saveButton.setOnClickListener {
            currentShipmentId?.let { shipmentId ->
                saveGoodsToStore(shipmentId)
            } ?: run {
                val errorMsg = "Error: Shipment ID not available."
                showTranslatedToast(errorMsg)
                Log.e("enter_store_goods", "Shipment ID is null.")
            }
        }

        val sharedPrefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSelectedStore = sharedPrefs.getString(LAST_STORE_KEY, storeLocations[0])
        val lastSelectedIndex = storeLocations.indexOf(lastSelectedStore)
        if (lastSelectedIndex != -1) {
            storeLocationSpinner.setSelection(lastSelectedIndex)
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

    private fun saveGoodsToStore(shipmentId: String) {
        val goodsName = goodsNameSpinner.selectedItem.toString()
        val storeLocation = storeLocationSpinner.selectedItem.toString()
        val goodsnostring = goodsNumberEditText.text.toString().trim()

        if (goodsnostring.isEmpty()) {
            goodsNumberEditText.error = "Please enter the Goods Number"
            return
        }

        if (goodsnostring.length != 4) {
            goodsNumberEditText.error = "Goods Number must be 4 characters"
            return
        }

        val itemData = hashMapOf(
            "name" to goodsName,
            "storeLocation" to storeLocation,
            "goodsNumber" to goodsnostring
        )

        if (isNetworkAvailable()) {
            saveToFirestore(shipmentId, itemData, storeLocation)
        } else {
            saveToLocalDatabase(shipmentId, goodsName, storeLocation, goodsnostring)
        }
    }

    private fun saveToFirestore(shipmentId: String, itemData: HashMap<String, String>, storeLocation: String) {
        firestore.collection("shipments")
            .document(shipmentId)
            .collection("store_inventory")
            .add(itemData)
            .addOnSuccessListener { documentReference ->
                val successMsg = "Goods added to store inventory (synced to cloud)"
                showTranslatedToast(successMsg)

                val sharedPrefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                with(sharedPrefs.edit()) {
                    putString(LAST_STORE_KEY, storeLocation)
                    apply()
                }

                clearFields()
            }
            .addOnFailureListener { e ->
                val errorMsg = "Error adding goods: ${e.message}"
                showTranslatedToast(errorMsg)
                Log.e("FirestoreError", "Error adding store goods", e)
            }
    }

    private fun saveToLocalDatabase(shipmentId: String, goodsName: String, storeLocation: String, goodsNumber: String) {
        val storeGoodsEntity = StoreGoodsEntity(
            shipmentId = shipmentId,
            name = goodsName,
            storeLocation = storeLocation,
            goodsNumber = goodsNumber,
            isSynced = false
        )

        OfflineDataStore.saveStoreGood(storeGoodsEntity, requireContext())
        val localSaveMsg = "Goods saved locally (will sync when online)"
        showTranslatedToast(localSaveMsg)

        val sharedPrefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putString(LAST_STORE_KEY, storeLocation)
            apply()
        }

        clearFields()
    }

    private fun clearFields() {
        goodsNumberEditText.text = null
        goodsNameSpinner.setSelection(0)
    }

    // Translation method
    private fun translateUIElements(targetLanguage: String) {
        view?.let { v ->
            // Translate title
            v.findViewById<TextView>(R.id.titleTextView)?.let { tv ->
                translationHelper.translateAndSetText(tv, "Enter Store Goods", targetLanguage)
            }

            // Translate labels
            v.findViewById<TextView>(R.id.goodsNameLabel)?.let { tv ->
                translationHelper.translateAndSetText(tv, "Goods Name:", targetLanguage)
            }

            v.findViewById<TextView>(R.id.storeLocationLabel)?.let { tv ->
                translationHelper.translateAndSetText(tv, "Store Location:", targetLanguage)
            }

            v.findViewById<TextView>(R.id.goodsNumberLabel)?.let { tv ->
                translationHelper.translateAndSetText(tv, "Goods Number:", targetLanguage)
            }

            // Translate button
            v.findViewById<Button>(R.id.saveButton)?.let { btn ->
                translationHelper.translateAndSetText(btn, "Save to Store Inventory", targetLanguage)
            }
        }
    }

    // Translate spinner items
    private fun translateSpinnerItems(targetLanguage: String) {
        val translatedGoodsItems = mutableListOf<String>()
        var goodsCount = 0

        // Translate goods items
        for (item in goodsOptions) {
            translationHelper.translateText(item, targetLanguage) { translatedItem ->
                translatedGoodsItems.add(translatedItem)
                goodsCount++

                // Once all goods items are translated, update the adapter
                if (goodsCount == goodsOptions.size) {
                    val goodsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, translatedGoodsItems)
                    goodsNameSpinner.adapter = goodsAdapter
                }
            }
        }

        // Translate store location items
        val translatedLocationItems = mutableListOf<String>()
        var locationCount = 0

        for (item in storeLocations) {
            translationHelper.translateText(item, targetLanguage) { translatedItem ->
                translatedLocationItems.add(translatedItem)
                locationCount++

                // Once all location items are translated, update the adapter
                if (locationCount == storeLocations.size) {
                    val locationAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, translatedLocationItems)
                    storeLocationSpinner.adapter = locationAdapter
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
        fun newInstance(shipmentId: String): enter_store_goods {
            val fragment = enter_store_goods()
            val args = Bundle()
            args.putString("shipmentId", shipmentId)
            fragment.arguments = args
            return fragment
        }
    }
}
