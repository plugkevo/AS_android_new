package com.example.africanshipping25

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldValue

class HomeFragment : Fragment(), ShipmentAdapter.OnShipmentItemClickListener {

    private val TAG = "HomeFragment"

    private lateinit var firestore: FirebaseFirestore
    private lateinit var tvActiveCount: TextView
    private lateinit var tvDeliveredCount: TextView
    private lateinit var rvAllShipments: RecyclerView
    private lateinit var homeShipmentAdapter: HomeShipmentAdapter
    private lateinit var sharedPreferences: SharedPreferences

    // Translation components - exactly like ProfileFragment
    private lateinit var translationManager: GoogleTranslationManager
    private lateinit var translationHelper: GoogleTranslationHelper

    // UI elements for translation
    private lateinit var tvWelcome: TextView
    private lateinit var tvOverview: TextView
    private lateinit var tvActiveLabel: TextView
    private lateinit var tvDeliveredLabel: TextView
    private lateinit var tvQuickActions: TextView
    private lateinit var tvRecentShipments: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(TAG, "onViewCreated called")

        firestore = FirebaseFirestore.getInstance()
        sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

        // Initialize translation components - exactly like ProfileFragment
        translationManager = GoogleTranslationManager(requireContext())
        translationHelper = GoogleTranslationHelper(translationManager)

        // Initialize UI elements
        initializeViews(view)

        // Initialize adapter and load data
        homeShipmentAdapter = HomeShipmentAdapter(mutableListOf(), this)
        rvAllShipments.layoutManager = LinearLayoutManager(requireContext())
        rvAllShipments.adapter = homeShipmentAdapter

        loadShipmentCounts()
        loadLatestShipments()

        // Set up click listeners
        setupClickListeners(view)

        // Translate UI elements based on current language - exactly like ProfileFragment
        val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
        translateUIElements(currentLanguage)

        Log.d(TAG, "HomeFragment setup completed")
    }

    private fun initializeViews(view: View) {
        Log.d(TAG, "Initializing views...")

        tvActiveCount = view.findViewById(R.id.tv_active_count)
        tvDeliveredCount = view.findViewById(R.id.tv_delivered_count)
        rvAllShipments = view.findViewById(R.id.rv_all_shipments)

        // Initialize text views for translation
        tvWelcome = view.findViewById(R.id.tv_welcome)
        tvOverview = view.findViewById(R.id.tv_overview)
        tvActiveLabel = view.findViewById(R.id.tv_active_label)
        tvDeliveredLabel = view.findViewById(R.id.tv_delivered_label)
        tvQuickActions = view.findViewById(R.id.tv_quick_actions)
        tvRecentShipments = view.findViewById(R.id.tv_recent_shipments)

        Log.d(TAG, "Views initialized successfully")
    }

    // Translation method - exactly like ProfileFragment
    private fun translateUIElements(targetLanguage: String) {
        view?.let { v ->
            // Translate main UI elements using the same pattern as ProfileFragment
            translationHelper.translateAndSetText(tvWelcome, "Welcome back", targetLanguage)
            translationHelper.translateAndSetText(tvOverview, "Here's your shipping overview", targetLanguage)
            translationHelper.translateAndSetText(tvActiveLabel, "Active Shipments", targetLanguage)
            translationHelper.translateAndSetText(tvDeliveredLabel, "Delivered This Month", targetLanguage)
            translationHelper.translateAndSetText(tvQuickActions, "Quick Actions", targetLanguage)
            translationHelper.translateAndSetText(tvRecentShipments, "Recent Shipments", targetLanguage)

            // Translate card labels using the same pattern as ProfileFragment
            v.findViewById<TextView>(R.id.tv_create_shipment_label)?.let { textView ->
                translationHelper.translateAndSetText(textView, "Create Shipment", targetLanguage)
            }

            v.findViewById<TextView>(R.id.tv_loading_list_label)?.let { textView ->
                translationHelper.translateAndSetText(textView, "Loading List", targetLanguage)
            }

            v.findViewById<TextView>(R.id.tv_track_shipment_label)?.let { textView ->
                translationHelper.translateAndSetText(textView, "Track Shipment", targetLanguage)
            }
        }
    }

    private fun setupClickListeners(view: View) {
        val showCreateShipmentDialogButton = view.findViewById<CardView>(R.id.card_create_shipment)
        showCreateShipmentDialogButton?.setOnClickListener {
            showCreateShipmentDialog()
        }

        val showCreateLoadingListDialogButton = view.findViewById<CardView>(R.id.card_loading)
        showCreateLoadingListDialogButton?.setOnClickListener {
            showCreateLoadingListDialog()
        }

        val mapsbtn = view.findViewById<CardView>(R.id.card_track_shipment)
        mapsbtn.setOnClickListener {
            val intent = Intent(requireContext(), MapsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadShipmentCounts() {
        firestore.collection("shipments")
            .whereIn("status", listOf("Active", "In Transit","Processing"))
            .get()
            .addOnSuccessListener { querySnapshot ->
                val activeCount = querySnapshot.size()
                tvActiveCount.text = activeCount.toString()
                Log.d(TAG, "Active Shipments Count: $activeCount")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting active shipments count: ", e)
                tvActiveCount.text = "Error"
            }

        firestore.collection("shipments")
            .whereEqualTo("status", "Delivered")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val deliveredCount = querySnapshot.size()
                tvDeliveredCount.text = deliveredCount.toString()
                Log.d(TAG, "Delivered Shipments Count: $deliveredCount")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting delivered shipments count: ", e)
                tvDeliveredCount.text = "Error"
            }
    }

    private fun loadLatestShipments() {
        firestore.collection("shipments")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(2)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val latestShipments = mutableListOf<Shipment>()
                for (document in querySnapshot.documents) {
                    val shipment = document.toObject(Shipment::class.java)
                    shipment?.let {
                        it.id = document.id
                        latestShipments.add(it)
                    }
                }
                homeShipmentAdapter.updateShipments(latestShipments)
                Log.d(TAG, "Loaded ${latestShipments.size} latest shipments.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting latest shipments: ", e)
                showTranslatedToast("Error loading recent shipments: ${e.message}")
            }
    }

    private fun showCreateShipmentDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_new_shipment, null)
        builder.setView(dialogView)
        val dialog = builder.create()

        // Translate dialog elements using ProfileFragment pattern
        translateCreateShipmentDialog(dialogView)

        dialog.show()

        val nameEditText = dialogView.findViewById<EditText>(R.id.et_name)
        val originEditText = dialogView.findViewById<EditText>(R.id.et_origin)
        val destinationEditText = dialogView.findViewById<EditText>(R.id.et_destination)
        val weightEditText = dialogView.findViewById<EditText>(R.id.et_details)
        val createButton = dialogView.findViewById<Button>(R.id.btn_create)
        val cancelButton = dialogView.findViewById<Button>(R.id.btn_cancel)

        createButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val origin = originEditText.text.toString().trim()
            val destination = destinationEditText.text.toString().trim()
            val weightStr = weightEditText.text.toString().trim()

            if (name.isEmpty() || origin.isEmpty() || destination.isEmpty() || weightStr.isEmpty()) {
                showTranslatedToast("Please fill in all the details")
                return@setOnClickListener
            }

            val weight = weightStr

            val shipment = hashMapOf(
                "name" to name,
                "origin" to origin,
                "destination" to destination,
                "weight" to weight,
                "status" to "Active",
                "createdAt" to FieldValue.serverTimestamp()
            )

            firestore.collection("shipments")
                .add(shipment)
                .addOnSuccessListener { documentReference ->
                    showTranslatedToast("Shipment created successfully!")
                    dialog.dismiss()
                    loadShipmentCounts()
                    loadLatestShipments()
                }
                .addOnFailureListener { e ->
                    showTranslatedToast("Error creating shipment: ${e.message}")
                }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun translateCreateShipmentDialog(dialogView: View) {
        val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"

        // Use the same translation pattern as ProfileFragment
        dialogView.findViewById<TextView>(R.id.tv_dialog_title)?.let { textView ->
            translationHelper.translateAndSetText(textView, "Create New Shipment", currentLanguage)
        }

        dialogView.findViewById<Button>(R.id.btn_create)?.let { button ->
            translationHelper.translateText("Create", currentLanguage) { translatedText ->
                button.text = translatedText
            }
        }

        dialogView.findViewById<Button>(R.id.btn_cancel)?.let { button ->
            translationHelper.translateText("Cancel", currentLanguage) { translatedText ->
                button.text = translatedText
            }
        }

        // Translate EditText hints
        dialogView.findViewById<EditText>(R.id.et_name)?.let { editText ->
            translationHelper.translateText("Enter shipment name", currentLanguage) { translatedText ->
                editText.hint = translatedText
            }
        }

        dialogView.findViewById<EditText>(R.id.et_origin)?.let { editText ->
            translationHelper.translateText("Enter origin", currentLanguage) { translatedText ->
                editText.hint = translatedText
            }
        }

        dialogView.findViewById<EditText>(R.id.et_destination)?.let { editText ->
            translationHelper.translateText("Enter destination", currentLanguage) { translatedText ->
                editText.hint = translatedText
            }
        }

        dialogView.findViewById<EditText>(R.id.et_details)?.let { editText ->
            translationHelper.translateText("Enter weight/details", currentLanguage) { translatedText ->
                editText.hint = translatedText
            }
        }
    }

    private fun showCreateLoadingListDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_loading, null)
        builder.setView(dialogView)

        val dialog = builder.create()

        // Translate dialog elements using ProfileFragment pattern
        translateCreateLoadingDialog(dialogView)

        dialog.show()

        val nameEditText = dialogView.findViewById<EditText>(R.id.et_name)
        val originEditText = dialogView.findViewById<EditText>(R.id.et_origin)
        val destinationEditText = dialogView.findViewById<EditText>(R.id.et_destination)
        val extraDetailsEditText = dialogView.findViewById<EditText>(R.id.et_details)
        val createButton = dialogView.findViewById<Button>(R.id.btn_create)
        val cancelButton = dialogView.findViewById<Button>(R.id.btn_cancel)

        createButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val origin = originEditText.text.toString().trim()
            val destination = destinationEditText.text.toString().trim()
            val extraDetails = extraDetailsEditText.text.toString().trim()

            if (name.isEmpty() || origin.isEmpty() || destination.isEmpty()) {
                showTranslatedToast("Name, Origin, and Destination are required.")
                return@setOnClickListener
            }

            val loadingList = hashMapOf(
                "name" to name,
                "origin" to origin,
                "destination" to destination,
                "extraDetails" to extraDetails,
                "status" to "New",
                "createdAt" to FieldValue.serverTimestamp()
            )

            firestore.collection("loading_lists")
                .add(loadingList)
                .addOnSuccessListener { documentReference ->
                    showTranslatedToast("Loading List created successfully!")
                    Log.d(TAG, "Loading List Document added with ID: ${documentReference.id}")
                    dialog.dismiss()
                }
                .addOnFailureListener { e ->
                    showTranslatedToast("Error creating loading list: ${e.message}")
                    Log.e(TAG, "Error adding loading list document", e)
                }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
            showTranslatedToast("Loading list creation cancelled.")
        }
    }

    private fun translateCreateLoadingDialog(dialogView: View) {
        val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"

        // Use the same translation pattern as ProfileFragment
        dialogView.findViewById<TextView>(R.id.tv_dialog_title)?.let { textView ->
            translationHelper.translateAndSetText(textView, "Create Loading List", currentLanguage)
        }

        dialogView.findViewById<Button>(R.id.btn_create)?.let { button ->
            translationHelper.translateText("Create", currentLanguage) { translatedText ->
                button.text = translatedText
            }
        }

        dialogView.findViewById<Button>(R.id.btn_cancel)?.let { button ->
            translationHelper.translateText("Cancel", currentLanguage) { translatedText ->
                button.text = translatedText
            }
        }
    }

    // Helper method to translate toast messages - exactly like ProfileFragment
    private fun showTranslatedToast(message: String) {
        val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
        translationHelper.translateText(message, currentLanguage) { translatedMessage ->
            Toast.makeText(context, translatedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    // Public method to refresh translations when language changes - exactly like ProfileFragment
    fun refreshTranslations() {
        Log.d(TAG, "refreshTranslations() called")

        if (!isAdded || view == null) {
            Log.w(TAG, "Fragment not attached or view is null, cannot refresh translations")
            return
        }

        val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"

        // Clear translation cache like ProfileFragment
        translationHelper.clearCache()

        // Retranslate UI elements
        translateUIElements(currentLanguage)

        Log.d(TAG, "Translation refresh completed")
    }

    // Implement the onShipmentItemClick method for HomeFragment
    override fun onShipmentItemClick(shipment: Shipment) {
        val intent = Intent(requireContext(), ViewShipment::class.java)
        intent.putExtra("shipmentId", shipment.id)
        intent.putExtra("shipmentName", shipment.name)
        intent.putExtra("shipmentOrigin", shipment.origin)
        intent.putExtra("shipmentDestination", shipment.destination)
        intent.putExtra("shipmentStatus", shipment.status)
        intent.putExtra("shipmentDate", shipment.date)
        shipment.createdAt?.let {
            intent.putExtra("shipmentCreatedAtMillis", it.time)
        }
        shipment.latitude?.let { intent.putExtra("shipmentLatitude", it) }
        shipment.longitude?.let { intent.putExtra("shipmentLongitude", it) }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::translationManager.isInitialized) {
            translationManager.cleanup()
        }
    }
}