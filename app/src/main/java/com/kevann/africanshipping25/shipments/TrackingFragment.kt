package com.kevann.africanshipping25.shipments

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kevann.africanshipping25.ais.AisHubRepository
import com.kevann.africanshipping25.ais.ShipsRepository
import com.kevann.africanshipping25.R
import com.kevann.africanshipping25.translation.GoogleTranslationManager
import com.kevann.africanshipping25.translation.GoogleTranslationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class Checkpoint(
    val id: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationName: String = "",
    val status: String = "",
    val notes: String = "",
    val timestamp: Date? = null
)

class TrackingFragment : Fragment(), OnMapReadyCallback {

    private lateinit var firestore: FirebaseFirestore
    private var shipmentId: String? = null
    private var googleMap: GoogleMap? = null
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var translationManager: GoogleTranslationManager
    private lateinit var translationHelper: GoogleTranslationHelper

    // Views
    private lateinit var tvShipmentName: TextView
    private lateinit var tvOrigin: TextView
    private lateinit var tvDestination: TextView
    private lateinit var tvCurrentStatus: TextView
    private lateinit var tvLastUpdated: TextView
    private lateinit var tvShipInfo: TextView
    private lateinit var btnAddCheckpoint: Button
    private lateinit var btnRefreshLocation: Button
    private lateinit var rvCheckpoints: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvNoCheckpoints: TextView

    private val checkpointList = mutableListOf<Checkpoint>()
    private lateinit var checkpointAdapter: CheckpointAdapter

    private val aisHubRepository = AisHubRepository()
    private val shipsRepository = ShipsRepository()

    companion object {
        private const val ARG_SHIPMENT_ID = "shipmentId"

        fun newInstance(shipmentId: String): TrackingFragment {
            val fragment = TrackingFragment()
            val args = Bundle()
            args.putString(ARG_SHIPMENT_ID, shipmentId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shipmentId = arguments?.getString(ARG_SHIPMENT_ID)
        firestore = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tracking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize translation components
        sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        translationManager = GoogleTranslationManager(requireContext())
        translationHelper = GoogleTranslationHelper(translationManager)

        initViews(view)
        setupRecyclerView()
        loadShipmentDetails()
        loadCheckpoints()

        btnAddCheckpoint.setOnClickListener {
            showAddCheckpointDialog()
        }

        btnRefreshLocation.setOnClickListener {
            refreshShipLocation()
        }

        // Initialize Google Map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        // Translate UI elements
        val currentLanguage = sharedPreferences.getString("language", "English") ?: "English"
        translateUIElements(currentLanguage)
    }

    private fun initViews(view: View) {
        tvShipmentName = view.findViewById(R.id.tvShipmentName)
        tvOrigin = view.findViewById(R.id.tvOrigin)
        tvDestination = view.findViewById(R.id.tvDestination)
        tvCurrentStatus = view.findViewById(R.id.tvCurrentStatus)
        tvLastUpdated = view.findViewById(R.id.tvLastUpdated)
        tvShipInfo = view.findViewById(R.id.tvShipInfo)
        btnAddCheckpoint = view.findViewById(R.id.btnAddCheckpoint)
        btnRefreshLocation = view.findViewById(R.id.btnRefreshLocation)
        rvCheckpoints = view.findViewById(R.id.rvCheckpoints)
        progressBar = view.findViewById(R.id.progressBar)
        tvNoCheckpoints = view.findViewById(R.id.tvNoCheckpoints)
    }

    private fun setupRecyclerView() {
        checkpointAdapter = CheckpointAdapter(checkpointList)
        rvCheckpoints.layoutManager = LinearLayoutManager(requireContext())
        rvCheckpoints.adapter = checkpointAdapter
    }

    private fun loadShipmentDetails() {
        shipmentId?.let { id ->
            firestore.collection("shipments").document(id)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        tvShipmentName.text = document.getString("name") ?: "Unknown Shipment"
                        tvOrigin.text = "Origin: ${document.getString("origin") ?: "N/A"}"
                        tvDestination.text = "Destination: ${document.getString("destination") ?: "N/A"}"
                        tvCurrentStatus.text = "Status: ${document.getString("status") ?: "Unknown"}"

                        val lastUpdated = document.getDate("lastUpdated")
                        if (lastUpdated != null) {
                            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                            tvLastUpdated.text = "Last Updated: ${dateFormat.format(lastUpdated)}"
                        } else {
                            tvLastUpdated.text = "Last Updated: N/A"
                        }

                        // Check if shipment has assigned ship
                        val assignedShipId = document.getString("assignedShipId")
                        if (!assignedShipId.isNullOrEmpty()) {
                            loadShipDetails(assignedShipId)
                        } else {
                            tvShipInfo.text = "No ship assigned to this shipment"
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("TrackingFragment", "Error loading shipment", e)
                    val errorMsg = "Error loading shipment details"
                    showTranslatedToast(errorMsg)
                }
        }
    }

    private fun loadShipDetails(shipId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val ship = shipsRepository.getShipById(shipId)
                if (ship != null) {
                    tvShipInfo.text = "Ship: ${ship.name} (${ship.number})\n" +
                        "IMO: ${ship.imoNumber}\n" +
                        "Location: ${String.format("%.4f", ship.currentLatitude)}, ${String.format("%.4f", ship.currentLongitude)}\n" +
                        "Speed: ${String.format("%.2f", ship.speed)} knots"

                    // Display ship location on map
                    val shipLocation = LatLng(ship.currentLatitude, ship.currentLongitude)
                    googleMap?.addMarker(
                        MarkerOptions()
                            .position(shipLocation)
                            .title(ship.name)
                            .snippet("Speed: ${ship.speed} knots")
                    )
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(shipLocation, 10f))
                }
            } catch (e: Exception) {
                Log.e("TrackingFragment", "Error loading ship details", e)
            }
        }
    }

    private fun loadCheckpoints() {
        shipmentId?.let { id ->
            progressBar.visibility = View.VISIBLE
            rvCheckpoints.visibility = View.GONE
            tvNoCheckpoints.visibility = View.GONE

            firestore.collection("shipments").document(id)
                .collection("tracking_checkpoints")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    progressBar.visibility = View.GONE
                    checkpointList.clear()

                    for (document in documents) {
                        val checkpoint = Checkpoint(
                            id = document.id,
                            latitude = document.getDouble("latitude") ?: 0.0,
                            longitude = document.getDouble("longitude") ?: 0.0,
                            locationName = document.getString("locationName") ?: "",
                            status = document.getString("status") ?: "",
                            notes = document.getString("notes") ?: "",
                            timestamp = document.getDate("timestamp")
                        )
                        checkpointList.add(checkpoint)
                    }

                    if (checkpointList.isEmpty()) {
                        tvNoCheckpoints.visibility = View.VISIBLE
                        rvCheckpoints.visibility = View.GONE
                    } else {
                        tvNoCheckpoints.visibility = View.GONE
                        rvCheckpoints.visibility = View.VISIBLE
                        checkpointAdapter.notifyDataSetChanged()
                    }
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    Log.e("TrackingFragment", "Error loading checkpoints", e)
                    val errorMsg = "Error loading checkpoints"
                    showTranslatedToast(errorMsg)
                }
        }
    }

    private fun refreshShipLocation() {
        shipmentId?.let { id ->
            firestore.collection("shipments").document(id).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val assignedShipId = document.getString("assignedShipId")
                        if (!assignedShipId.isNullOrEmpty()) {
                            CoroutineScope(Dispatchers.Main).launch {
                                try {
                                    val fetchingMsg = "Fetching live location..."
                                    showTranslatedToast(fetchingMsg)
                                    val ship = shipsRepository.getShipById(assignedShipId)
                                    if (ship != null) {
                                        val vesselLocation = aisHubRepository.getVesselLocationByIMO(ship.imoNumber)
                                        if (vesselLocation != null) {
                                            shipsRepository.updateShipLocation(assignedShipId, vesselLocation)
                                            loadShipDetails(assignedShipId)
                                            val updatedMsg = "Location updated!"
                                            showTranslatedToast(updatedMsg)
                                        } else {
                                            val notFoundMsg = "Vessel not found in AIS database"
                                            showTranslatedToast(notFoundMsg)
                                        }
                                    }
                                } catch (e: Exception) {
                                    val errorMsg = "Error: ${e.message}"
                                    showTranslatedToast(errorMsg)
                                }
                            }
                        } else {
                            val noShipMsg = "No ship assigned to this shipment"
                            showTranslatedToast(noShipMsg)
                        }
                    }
                }
        }
    }

    private fun showAddCheckpointDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_checkpoint, null)

        val etLocationName = dialogView.findViewById<EditText>(R.id.etLocationName)
        val etLatitude = dialogView.findViewById<EditText>(R.id.etLatitude)
        val etLongitude = dialogView.findViewById<EditText>(R.id.etLongitude)
        val spinnerStatus = dialogView.findViewById<Spinner>(R.id.spinnerStatus)
        val etNotes = dialogView.findViewById<EditText>(R.id.etNotes)

        val statusOptions = arrayOf(
            "Departed Origin", "In Transit", "At Checkpoint", "Customs Clearance",
            "At Warehouse", "Out for Delivery", "Delivered", "Delayed", "On Hold"
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statusOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatus.adapter = adapter

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Add Tracking Checkpoint")
            .setView(dialogView)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(R.color.colorDarkBlue, null))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(resources.getColor(R.color.colorDarkBlue, null))

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val locationName = etLocationName.text.toString().trim()
                val latitudeStr = etLatitude.text.toString().trim()
                val longitudeStr = etLongitude.text.toString().trim()
                val status = spinnerStatus.selectedItem.toString()
                val notes = etNotes.text.toString().trim()

                if (locationName.isEmpty()) {
                    val emptyMsg = "Please enter location name"
                    showTranslatedToast(emptyMsg)
                    return@setOnClickListener
                }

                val latitude = latitudeStr.toDoubleOrNull()
                val longitude = longitudeStr.toDoubleOrNull()

                if (latitude == null || longitude == null || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                    val invalidMsg = "Invalid coordinates"
                    showTranslatedToast(invalidMsg)
                    return@setOnClickListener
                }

                shipmentId?.let { id ->
                    val checkpoint = hashMapOf(
                        "locationName" to locationName,
                        "latitude" to latitude,
                        "longitude" to longitude,
                        "status" to status,
                        "notes" to notes,
                        "timestamp" to FieldValue.serverTimestamp()
                    )

                    firestore.collection("shipments").document(id)
                        .collection("tracking_checkpoints")
                        .add(checkpoint)
                        .addOnSuccessListener {
                            val successMsg = "Checkpoint added!"
                            showTranslatedToast(successMsg)
                            dialog.dismiss()
                            loadCheckpoints()
                        }
                        .addOnFailureListener { e ->
                            val errorMsg = "Error: ${e.message}"
                            showTranslatedToast(errorMsg)
                        }
                }
            }
        }

        dialog.show()
    }

    override fun onMapReady(map: GoogleMap) {
        this.googleMap = map
        googleMap?.setMinZoomPreference(5f)
        googleMap?.setMaxZoomPreference(20f)
    }

    // Translation method
    private fun translateUIElements(targetLanguage: String) {
        view?.let { v ->
            // Translate title
            v.findViewById<TextView>(R.id.titleTextView)?.let { tv ->
                translationHelper.translateAndSetText(tv, "Tracking", targetLanguage)
            }

            // Translate labels
            v.findViewById<Button>(R.id.btnAddCheckpoint)?.let { btn ->
                translationHelper.translateAndSetText(btn, "Add Checkpoint", targetLanguage)
            }

            v.findViewById<Button>(R.id.btnRefreshLocation)?.let { btn ->
                translationHelper.translateAndSetText(btn, "Refresh Location", targetLanguage)
            }

            v.findViewById<TextView>(R.id.tvNoCheckpoints)?.let { tv ->
                translationHelper.translateAndSetText(tv, "No checkpoints yet", targetLanguage)
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
}
