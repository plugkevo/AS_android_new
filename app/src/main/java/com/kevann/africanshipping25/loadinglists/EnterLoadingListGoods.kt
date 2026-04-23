package com.kevann.africanshipping25.loadinglists

import android.app.DatePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.kevann.africanshipping25.R
import com.kevann.africanshipping25.database.OfflineDataStore
import com.kevann.africanshipping25.translation.GoogleTranslationManager
import com.kevann.africanshipping25.translation.GoogleTranslationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

// Define a constant for the argument key
private const val ARG_LOADING_LIST_ID = "loadingListId"
private const val WAREHOUSE_ITEMS_COLLECTION = "warehouseItems" // Subcollection name

class EnterWarehouseGoods : Fragment() {

    private var loadingListId: String? = null
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var translationManager: GoogleTranslationManager
    private lateinit var translationHelper: GoogleTranslationHelper

    // Declare your UI elements
    private lateinit var goodsNumberFieldsContainer: LinearLayout
    private lateinit var buttonAddGoodNo: ImageButton
    private lateinit var editTextSenderName: EditText
    private lateinit var editTextPhoneNumber: EditText
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

        // Initialize translation
        sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        translationManager = GoogleTranslationManager(requireContext())
        translationHelper = GoogleTranslationHelper(translationManager)

        // Initialize UI elements
        goodsNumberFieldsContainer = view.findViewById(R.id.goodsNumberFieldsContainer)
        buttonAddGoodNo = view.findViewById(R.id.buttonAddGoodNo)
        editTextSenderName = view.findViewById(R.id.editTextSenderName)
        editTextPhoneNumber = view.findViewById(R.id.editTextPhoneNumber)
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

        // Translate UI elements
        val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
        translateUIElements(view, currentLanguage)

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

    private fun saveWarehouseItems() {
        val senderName = editTextSenderName.text.toString().trim()
        val phoneNumber = editTextPhoneNumber.text.toString().trim()
        val date = editTextDate.text.toString().trim()

        // Reset errors on all goods number fields
        goodsNumberInputLayouts.forEach { it.error = null }

        // Basic validation for sender name, phone number, and date
        if (senderName.isEmpty() || phoneNumber.isEmpty() || date.isEmpty()) {
            val emptyMsg = "Please fill in Sender Name, Phone Number, and Date"
            showTranslatedToast(emptyMsg)
            return
        }

        if (loadingListId == null) {
            val errorMsg = "Error: Loading List ID is missing. Cannot save item."
            showTranslatedToast(errorMsg)
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
            val errorMsg = "Please correct the errors in the Good Numbers"
            showTranslatedToast(errorMsg)
            return
        }

        if (goodsNumbersToSave.isEmpty()) {
            val emptyMsg = "Please enter at least one valid Good Number"
            showTranslatedToast(emptyMsg)
            return
        }

        // Check if online
        if (isNetworkAvailable()) {
            saveToFirestore(senderName, phoneNumber, date, goodsNumbersToSave)
        } else {
            saveToLocalDatabase(senderName, phoneNumber, date, goodsNumbersToSave)
        }
    }

    private fun saveToFirestore(senderName: String, phoneNumber: String, date: String, goodsNumbersToSave: MutableList<String>) {
        Log.d("EnterWarehouseGoods", "[v0] Starting saveToFirestore with ${goodsNumbersToSave.size} items and loadingListId: $loadingListId")
        
        var successCount = 0
        var failureCount = 0
        val totalItems = goodsNumbersToSave.size

        for (goodNo in goodsNumbersToSave) {
            // Create a map of data to be saved for each good number
            val warehouseItem = hashMapOf(
                "goodNo" to goodNo,
                "senderName" to senderName,
                "phoneNumber" to phoneNumber,
                "date" to date,
                "timestamp" to FieldValue.serverTimestamp()
            )

            Log.d("EnterWarehouseGoods", "[v0] Attempting to save goodNo: $goodNo to collection: loading_lists/$loadingListId/$WAREHOUSE_ITEMS_COLLECTION")

            // Reference to the subcollection
            // loading_lists/{loadingListId}/warehouseItems/{documentId}
            firestore.collection("loading_lists")
                .document(loadingListId!!)
                .collection(WAREHOUSE_ITEMS_COLLECTION)
                .add(warehouseItem)
                .addOnSuccessListener { documentReference ->
                    Log.d("EnterWarehouseGoods", "[v0] DocumentSnapshot added with ID: ${documentReference.id} for Good No: $goodNo")
                    successCount++
                    if (successCount + failureCount == totalItems) {
                        // All items processed
                        if (failureCount == 0) {
                            val successMsg = "All warehouse items synced to cloud!"
                            showTranslatedToast(successMsg)
                        } else {
                            val failMsg = "Synced $successCount items, $failureCount failed."
                            showTranslatedToast(failMsg)
                        }
                        clearInputFields()
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("EnterWarehouseGoods", "[v0] Error adding document for Good No: $goodNo", e)
                    Log.e("EnterWarehouseGoods", "[v0] Exception message: ${e.message}")
                    failureCount++
                    if (successCount + failureCount == totalItems) {
                        // All items processed
                        val failMsg = "Synced $successCount items, $failureCount failed."
                        showTranslatedToast(failMsg)
                        clearInputFields()
                    }
                }
        }
    }

    private fun saveToLocalDatabase(senderName: String, phoneNumber: String, date: String, goodsNumbersToSave: MutableList<String>) {
        val totalItems = goodsNumbersToSave.size

        for (goodNo in goodsNumbersToSave) {
            val warehouseItemEntity = com.kevann.africanshipping25.database.WarehouseGoodsEntity(
                loadingListId = loadingListId!!,
                goodNo = goodNo,
                senderName = senderName,
                phoneNumber = phoneNumber,
                date = date,
                isSynced = false
            )
            OfflineDataStore.saveWarehouseGood(warehouseItemEntity, requireContext())
        }

        val localMsg = "Saved $totalItems warehouse items locally (will sync when online)"
        showTranslatedToast(localMsg)
        clearInputFields()
    }

    private fun clearInputFields() {
        editTextSenderName.text.clear()
        editTextPhoneNumber.text.clear()

        // Clear all dynamically added goods number fields and remove them
        goodsNumberEditTexts.clear()
        goodsNumberInputLayouts.clear()
        goodsNumberFieldsContainer.removeAllViews()

        // Add back a single, fresh goods number input field
        addGoodsNumberInputField()
    }

    private fun translateUIElements(view: View, targetLanguage: String) {
        view.let { v ->
            // Translate Submit button
            v.findViewById<Button>(R.id.buttonSubmit)?.let { btn ->
                translationHelper.translateAndSetText(btn, "Submit", targetLanguage)
            }

            // Translate section titles
            v.findViewById<TextView>(R.id.tvGoodNumbersTitle)?.let { tv ->
                translationHelper.translateAndSetText(tv, "Good Numbers", targetLanguage)
            }

            v.findViewById<TextView>(R.id.tvAddGoodNumberLabel)?.let { tv ->
                translationHelper.translateAndSetText(tv, "Add Good Number Field", targetLanguage)
            }

            v.findViewById<TextView>(R.id.tvSenderNameTitle)?.let { tv ->
                translationHelper.translateAndSetText(tv, "Sender Name", targetLanguage)
            }

            v.findViewById<TextView>(R.id.tvPhoneNumberTitle)?.let { tv ->
                translationHelper.translateAndSetText(tv, "Phone Number", targetLanguage)
            }

            v.findViewById<TextView>(R.id.tvDateTitle)?.let { tv ->
                translationHelper.translateAndSetText(tv, "Date", targetLanguage)
            }

            // Translate TextInputLayout hints (not EditText hints directly)
            v.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilSenderName)?.let { layout ->
                translationHelper.translateText("Sender Name", targetLanguage) { translated ->
                    layout.hint = translated
                }
            }

            v.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilPhoneNumber)?.let { layout ->
                translationHelper.translateText("Phone Number", targetLanguage) { translated ->
                    layout.hint = translated
                }
            }

            v.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilDate)?.let { layout ->
                translationHelper.translateText("Date (dd/MM/yyyy)", targetLanguage) { translated ->
                    layout.hint = translated
                }
            }
        }
    }

    private fun showTranslatedToast(message: String) {
        val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
        translationHelper.translateText(message, currentLanguage) { translatedMessage ->
            Toast.makeText(context, translatedMessage, Toast.LENGTH_SHORT).show()
        }
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
