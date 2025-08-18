package com.example.africanshipping25

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
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
    private lateinit var goodsNumberFieldsContainer: LinearLayout
    private lateinit var buttonAddGoodNo: ImageButton
    private lateinit var editTextSenderName: EditText
    private lateinit var editTextPhoneNumber: EditText // Declared the phone number EditText
    private lateinit var editTextDate: EditText
    private lateinit var buttonSubmit: Button

    // List to hold references to all dynamically added goods number input fields and their parent TextInputLayouts
    private val goodsNumberInputLayouts: MutableList<TextInputLayout> = mutableListOf()
    private val goodsNumberEditTexts: MutableList<TextInputEditText> = mutableListOf()

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
        goodsNumberFieldsContainer = view.findViewById(R.id.goodsNumberFieldsContainer)
        buttonAddGoodNo = view.findViewById(R.id.buttonAddGoodNo)
        editTextSenderName = view.findViewById(R.id.editTextSenderName)
        editTextPhoneNumber = view.findViewById(R.id.editTextPhoneNumber) // Initialized the phone number EditText
        editTextDate = view.findViewById(R.id.editTextDate)
        buttonSubmit = view.findViewById(R.id.buttonSubmit)

        // Add the initial goods number input field
        addGoodsNumberInputField()

        // Setup Add Good Number Button Click Listener
        buttonAddGoodNo.setOnClickListener {
            addGoodsNumberInputField()
        }

        // Setup Date Picker
        editTextDate.setOnClickListener {
            showDatePickerDialog()
        }

        // Setup Submit Button Click Listener
        buttonSubmit.setOnClickListener {
            saveWarehouseItems()
        }

        return view
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
                setMargins(0, 0, 0, resources.getDimensionPixelSize(com.example.africanshipping25.R.dimen.margin_small))
            }
            hint = "Enter Good Number (4 characters)" // Update hint for clarity
        }

        val textInputEditText = TextInputEditText(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        textInputLayout.addView(textInputEditText)
        goodsNumberFieldsContainer.addView(textInputLayout)
        goodsNumberInputLayouts.add(textInputLayout) // Add the TextInputLayout to track errors
        goodsNumberEditTexts.add(textInputEditText) // Add to our list for tracking
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

    private fun saveWarehouseItems() {
        val senderName = editTextSenderName.text.toString().trim()
        val phoneNumber = editTextPhoneNumber.text.toString().trim() // Get phone number
        val date = editTextDate.text.toString().trim()

        // Reset errors on all goods number fields
        goodsNumberInputLayouts.forEach { it.error = null }

        // Basic validation for sender name, phone number, and date
        if (senderName.isEmpty() || phoneNumber.isEmpty() || date.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in Sender Name, Phone Number, and Date", Toast.LENGTH_SHORT).show()
            return
        }

        if (loadingListId == null) {
            Toast.makeText(requireContext(), "Error: Loading List ID is missing. Cannot save item.", Toast.LENGTH_LONG).show()
            Log.e("EnterWarehouseGoods", "loadingListId is null when trying to save item!")
            return
        }

        val goodsNumbersToSave = mutableListOf<String>()
        var hasValidationErrors = false

        for (i in goodsNumberEditTexts.indices) {
            val goodNo = goodsNumberEditTexts[i].text.toString().trim()
            if (goodNo.isEmpty()) {
                goodsNumberInputLayouts[i].error = "Good Number cannot be empty"
                hasValidationErrors = true
            } else if (goodNo.length != 4) {
                goodsNumberInputLayouts[i].error = "Good Number must be 4 characters"
                hasValidationErrors = true
            } else {
                goodsNumbersToSave.add(goodNo)
            }
        }

        if (hasValidationErrors) {
            Toast.makeText(requireContext(), "Please correct the errors in the Good Numbers", Toast.LENGTH_LONG).show()
            return
        }

        if (goodsNumbersToSave.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter at least one valid Good Number", Toast.LENGTH_SHORT).show()
            return
        }

        var successCount = 0
        var failureCount = 0
        val totalItems = goodsNumbersToSave.size

        for (goodNo in goodsNumbersToSave) {
            // Create a map of data to be saved for each good number
            val warehouseItem = hashMapOf(
                "goodNo" to goodNo,
                "senderName" to senderName,
                "phoneNumber" to phoneNumber, // Added phone number to the data map
                "date" to date,
                "timestamp" to FieldValue.serverTimestamp() // Adds a server-generated timestamp
            )

            // Reference to the subcollection
            // loading_lists/{loadingListId}/warehouseItems/{documentId}
            firestore.collection("loading_lists")
                .document(loadingListId!!) // Use !! because we've already checked for null
                .collection(WAREHOUSE_ITEMS_COLLECTION)
                .add(warehouseItem) // Add a new document with an auto-generated ID
                .addOnSuccessListener { documentReference ->
                    Log.d("EnterWarehouseGoods", "DocumentSnapshot added with ID: ${documentReference.id} for Good No: $goodNo")
                    successCount++
                    if (successCount + failureCount == totalItems) {
                        // All items processed
                        if (failureCount == 0) {
                            Toast.makeText(requireContext(), "All warehouse items added successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Added $successCount items, $failureCount failed.", Toast.LENGTH_LONG).show()
                        }
                        clearInputFields()
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("EnterWarehouseGoods", "Error adding document for Good No: $goodNo", e)
                    failureCount++
                    if (successCount + failureCount == totalItems) {
                        // All items processed
                        Toast.makeText(requireContext(), "Added $successCount items, $failureCount failed.", Toast.LENGTH_LONG).show()
                        clearInputFields()
                    }
                }
        }
    }

    private fun clearInputFields() {
        editTextSenderName.text.clear()
        editTextPhoneNumber.text.clear() // Clear the phone number field


        // Clear all dynamically added goods number fields and remove them
        goodsNumberEditTexts.clear()
        goodsNumberInputLayouts.clear() // Clear the TextInputLayouts list as well
        goodsNumberFieldsContainer.removeAllViews()

        // Add back a single, fresh goods number input field
        addGoodsNumberInputField()
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