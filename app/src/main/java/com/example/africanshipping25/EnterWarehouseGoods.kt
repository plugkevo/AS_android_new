package com.example.africanshipping25

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Define a constant for the argument key
private const val ARG_LOADING_LIST_ID = "loadingListId"
private const val WAREHOUSE_ITEMS_COLLECTION = "warehouseItems" // Subcollection name

class EnterWarehouseGoods : Fragment() {

    private var loadingListId: String? = null
    private lateinit var firestore: FirebaseFirestore

    // Declare your UI elements
    private lateinit var editTextGoodNo: EditText
    private lateinit var spinnerGoodsName: Spinner
    private lateinit var editTextSenderName: EditText
    private lateinit var editTextDate: EditText
    private lateinit var buttonSubmit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            loadingListId = it.getString(ARG_LOADING_LIST_ID)
        }
        Log.d("EnterWarehouseGoods", "Fragment received Loading List ID: $loadingListId")

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_enter_warehouse_goods, container, false)

        // Initialize UI elements
        editTextGoodNo = view.findViewById(R.id.editTextGoodNo)
        spinnerGoodsName = view.findViewById(R.id.spinnerGoodsName)
        editTextSenderName = view.findViewById(R.id.editTextSenderName)
        editTextDate = view.findViewById(R.id.editTextDate)
        buttonSubmit = view.findViewById(R.id.buttonSubmit)

        // Setup Spinner with options from strings.xml
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.goods_name_options, // Make sure this array name matches your strings.xml
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerGoodsName.adapter = adapter
        }

        // Setup Date Picker
        editTextDate.setOnClickListener {
            showDatePickerDialog()
        }

        // Setup Submit Button Click Listener
        buttonSubmit.setOnClickListener {
            saveWarehouseItem()
        }

        return view
    }

    private fun showDatePickerDialog() {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        val dpd = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            // Display selected date in EditText
            val selectedDate = Calendar.getInstance()
            selectedDate.set(selectedYear, selectedMonth, selectedDay)
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            editTextDate.setText(sdf.format(selectedDate.time))
        }, year, month, day)
        dpd.show()
    }

    private fun saveWarehouseItem() {
        val goodNo = editTextGoodNo.text.toString().trim()
        val goodsName = spinnerGoodsName.selectedItem.toString()
        val senderName = editTextSenderName.text.toString().trim()
        val date = editTextDate.text.toString().trim()

        // Basic validation
        if (goodNo.isEmpty() || senderName.isEmpty() || date.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (loadingListId == null) {
            Toast.makeText(requireContext(), "Error: Loading List ID is missing. Cannot save item.", Toast.LENGTH_LONG).show()
            Log.e("EnterWarehouseGoods", "loadingListId is null when trying to save item!")
            return
        }

        // Create a map of data to be saved
        val warehouseItem = hashMapOf(
            "goodNo" to goodNo,
            "goodsName" to goodsName,
            "senderName" to senderName,
            "date" to date,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp() // Adds a server-generated timestamp
        )

        // Reference to the subcollection
        // loading_lists/{loadingListId}/warehouseItems/{documentId}
        firestore.collection("loading_lists")
            .document(loadingListId!!) // Use !! because we've already checked for null
            .collection(WAREHOUSE_ITEMS_COLLECTION)
            .add(warehouseItem) // Add a new document with an auto-generated ID
            .addOnSuccessListener { documentReference ->
                Log.d("EnterWarehouseGoods", "DocumentSnapshot added with ID: ${documentReference.id}")
                Toast.makeText(requireContext(), "Warehouse item added successfully!", Toast.LENGTH_SHORT).show()
                // Optionally clear fields after successful submission
                clearInputFields()
            }
            .addOnFailureListener { e ->
                Log.w("EnterWarehouseGoods", "Error adding document", e)
                Toast.makeText(requireContext(), "Error adding warehouse item: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun clearInputFields() {
        editTextGoodNo.text.clear()
        editTextSenderName.text.clear()
        editTextDate.text.clear()
        spinnerGoodsName.setSelection(0) // Reset spinner to first item
    }

    companion object {
        @JvmStatic
        fun newInstance(loadingListId: String) =
            EnterWarehouseGoods().apply {
                arguments = Bundle().apply {
                    putString(ARG_LOADING_LIST_ID, loadingListId)
                }
            }
    }
}