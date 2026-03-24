package com.kevann.africanshipping25.shipments

import android.content.Context
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
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.kevann.africanshipping25.R
import com.kevann.africanshipping25.database.OfflineDatabase
import com.kevann.africanshipping25.database.StoreGoodsEntity
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

        val goodsOptions = arrayOf("Box","Furniture","Electronics", "Toiletries","Tote/Barrel", "Machinery","Other")
        val goodsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, goodsOptions)
        goodsNameSpinner.adapter = goodsAdapter

        val storeLocations = arrayOf("Store A", "Store B", "Store C")
        val locationAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, storeLocations)
        storeLocationSpinner.adapter = locationAdapter

        val sharedPrefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSelectedStore = sharedPrefs.getString(LAST_STORE_KEY, storeLocations[0])
        val lastSelectedIndex = storeLocations.indexOf(lastSelectedStore)
        if (lastSelectedIndex != -1) {
            storeLocationSpinner.setSelection(lastSelectedIndex)
        }

        saveButton.setOnClickListener {
            currentShipmentId?.let { shipmentId ->
                saveGoodsToStore(shipmentId)
            } ?: run {
                Toast.makeText(requireContext(), "Error: Shipment ID not available.", Toast.LENGTH_SHORT).show()
                Log.e("enter_store_goods", "Shipment ID is null.")
            }
        }
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
                Toast.makeText(
                    requireContext(),
                    "Goods added to store inventory (synced to cloud)",
                    Toast.LENGTH_SHORT
                ).show()

                val sharedPrefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                with(sharedPrefs.edit()) {
                    putString(LAST_STORE_KEY, storeLocation)
                    apply()
                }

                clearFields()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error adding goods to store inventory: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e("FirestoreError", "Error adding store goods", e)
            }
    }

    private fun saveToLocalDatabase(shipmentId: String, goodsName: String, storeLocation: String, goodsNumber: String) {
        val db = OfflineDatabase.getDatabase(requireContext())
        val storeGoodsEntity = StoreGoodsEntity(
            shipmentId = shipmentId,
            name = goodsName,
            storeLocation = storeLocation,
            goodsNumber = goodsNumber,
            isSynced = false
        )

        GlobalScope.launch(Dispatchers.IO) {
            db.offlineDao().insertStoreGoods(storeGoodsEntity)
            GlobalScope.launch(Dispatchers.Main) {
                Toast.makeText(
                    requireContext(),
                    "Goods saved locally (will sync when online)",
                    Toast.LENGTH_SHORT
                ).show()

                val sharedPrefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                with(sharedPrefs.edit()) {
                    putString(LAST_STORE_KEY, storeLocation)
                    apply()
                }

                clearFields()
            }
        }
    }

    private fun clearFields() {
        goodsNumberEditText.text = null
        goodsNameSpinner.setSelection(0)
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
