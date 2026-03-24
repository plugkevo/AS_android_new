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
import com.kevann.africanshipping25.database.TruckGoodsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

data class TruckGoodInput(
    var name: String? = null,
    var goodsNumber: String? = null
)

class enter_truck_goods : Fragment() {

    private lateinit var goodsNameSpinner: Spinner
    private lateinit var goodsNumber: TextInputEditText
    private lateinit var addButton: Button
    private val firestore = FirebaseFirestore.getInstance()
    private var currentShipmentId: String? = null
    private val goodsOptions = arrayOf("Box","Furniture","Electronics", "Toiletries","Tote/Barrel", "Machinery","Other")

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
        addButton = view.findViewById(R.id.saveButton)

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, goodsOptions)
        goodsNameSpinner.adapter = adapter

        addButton.setOnClickListener {
            addGoodsToShipment()
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

    private fun addGoodsToShipment() {
        goodsNumber.error = null

        if (currentShipmentId == null) {
            Toast.makeText(requireContext(), "Error: Shipment ID not available.", Toast.LENGTH_SHORT).show()
            Log.e("enter_truck_goods", "Shipment ID is null.")
            return
        }

        val goodsName = goodsNameSpinner.selectedItem.toString()
        val goodsNumberString = goodsNumber.text.toString().trim()

        if (goodsNumberString.isEmpty()) {
            goodsNumber.error = "Please enter the goods number"
            return
        }

        if (goodsNumberString.length != 4) {
            goodsNumber.error = "Goods number must be 4 characters"
            return
        }

        val newTruckGood = TruckGoodInput(name = goodsName, goodsNumber = goodsNumberString)

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
        firestore.collection("shipments")
            .document(shipmentId)
            .collection("truck_inventory")
            .add(good)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(
                    requireContext(),
                    "Item added to truck inventory (synced to cloud)",
                    Toast.LENGTH_SHORT
                ).show()
                clearFields()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error adding item: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e("FirestoreError", "Error adding truck goods", e)
            }
    }

    private fun saveToLocalDatabase(good: TruckGoodInput) {
        val db = OfflineDatabase.getDatabase(requireContext())
        val truckGoodsEntity = TruckGoodsEntity(
            shipmentId = currentShipmentId!!,
            name = good.name ?: "",
            goodsNumber = good.goodsNumber ?: "",
            isSynced = false
        )

        GlobalScope.launch(Dispatchers.IO) {
            db.truckGoodsDao().insert(truckGoodsEntity)
            GlobalScope.launch(Dispatchers.Main) {
                Toast.makeText(
                    requireContext(),
                    "Item saved locally (will sync when online)",
                    Toast.LENGTH_SHORT
                ).show()
                clearFields()
            }
        }
    }

    private fun clearFields() {
        goodsNumber.text = null
        goodsNameSpinner.setSelection(0)
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
