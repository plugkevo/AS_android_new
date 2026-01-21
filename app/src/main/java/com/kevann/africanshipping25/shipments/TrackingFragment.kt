package com.kevann.africanshipping25.shipments


import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kevann.africanshipping25.R
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

class TrackingFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private var shipmentId: String? = null

    // Views
    private lateinit var tvShipmentName: TextView
    private lateinit var tvOrigin: TextView
    private lateinit var tvDestination: TextView
    private lateinit var tvCurrentStatus: TextView
    private lateinit var tvLastUpdated: TextView
    private lateinit var btnAddCheckpoint: Button
    private lateinit var rvCheckpoints: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvNoCheckpoints: TextView

    private val checkpointList = mutableListOf<Checkpoint>()
    private lateinit var checkpointAdapter: CheckpointAdapter

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

        initViews(view)
        setupRecyclerView()
        loadShipmentDetails()
        loadCheckpoints()

        btnAddCheckpoint.setOnClickListener {
            showAddCheckpointDialog()
        }
    }

    private fun initViews(view: View) {
        tvShipmentName = view.findViewById(R.id.tvShipmentName)
        tvOrigin = view.findViewById(R.id.tvOrigin)
        tvDestination = view.findViewById(R.id.tvDestination)
        tvCurrentStatus = view.findViewById(R.id.tvCurrentStatus)
        tvLastUpdated = view.findViewById(R.id.tvLastUpdated)
        btnAddCheckpoint = view.findViewById(R.id.btnAddCheckpoint)
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
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("TrackingFragment", "Error loading shipment", e)
                    Toast.makeText(requireContext(), "Error loading shipment details", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(requireContext(), "Error loading checkpoints", Toast.LENGTH_SHORT).show()
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

        // Setup status spinner
        val statusOptions = arrayOf(
            "Departed Origin",
            "In Transit",
            "At Checkpoint",
            "Customs Clearance",
            "At Warehouse",
            "Out for Delivery",
            "Delivered",
            "Delayed",
            "On Hold"
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statusOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatus.adapter = adapter

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Add Tracking Checkpoint")
            .setView(dialogView)
            .setPositiveButton("Add", null) // Set to null initially
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            // Change Add button color
            val addButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            addButton.setTextColor(resources.getColor(R.color.colorDarkBlue, null))

            // Change Cancel button color
            val cancelButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            cancelButton.setTextColor(resources.getColor(R.color.colorDarkBlue, null)) // Or any color you want

            // Set click listener for Add button
            addButton.setOnClickListener {
                val locationName = etLocationName.text.toString().trim()
                val latitudeStr = etLatitude.text.toString().trim()
                val longitudeStr = etLongitude.text.toString().trim()
                val status = spinnerStatus.selectedItem.toString()
                val notes = etNotes.text.toString().trim()

                // Validate inputs
                if (locationName.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter location name", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val latitude = latitudeStr.toDoubleOrNull()
                val longitude = longitudeStr.toDoubleOrNull()

                if (latitude == null || longitude == null) {
                    Toast.makeText(requireContext(), "Please enter valid coordinates", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (latitude < -90 || latitude > 90) {
                    Toast.makeText(requireContext(), "Latitude must be between -90 and 90", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (longitude < -180 || longitude > 180) {
                    Toast.makeText(requireContext(), "Longitude must be between -180 and 180", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                addCheckpoint(locationName, latitude, longitude, status, notes)
                dialog.dismiss()
            }

            // Set click listener for Cancel button
            cancelButton.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }
    private fun addCheckpoint(
        locationName: String,
        latitude: Double,
        longitude: Double,
        status: String,
        notes: String
    ) {
        shipmentId?.let { id ->
            val checkpointData = hashMapOf(
                "locationName" to locationName,
                "latitude" to latitude,
                "longitude" to longitude,
                "status" to status,
                "notes" to notes,
                "timestamp" to FieldValue.serverTimestamp()
            )

            firestore.collection("shipments").document(id)
                .collection("tracking_checkpoints")
                .add(checkpointData)
                .addOnSuccessListener {
                    // Update shipment status and last updated
                    firestore.collection("shipments").document(id)
                        .update(
                            mapOf(
                                "status" to status,
                                "currentLocationName" to locationName,
                                "lastUpdated" to FieldValue.serverTimestamp()
                            )
                        )
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Checkpoint added successfully", Toast.LENGTH_SHORT).show()
                            loadShipmentDetails()
                            loadCheckpoints()
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("TrackingFragment", "Error adding checkpoint", e)
                    Toast.makeText(requireContext(), "Error adding checkpoint: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Inner adapter class for checkpoints
    inner class CheckpointAdapter(private val checkpoints: List<Checkpoint>) :
        RecyclerView.Adapter<CheckpointAdapter.CheckpointViewHolder>() {

        inner class CheckpointViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvLocationName: TextView = itemView.findViewById(R.id.tvCheckpointLocation)
            val tvStatus: TextView = itemView.findViewById(R.id.tvCheckpointStatus)
            val tvCoordinates: TextView = itemView.findViewById(R.id.tvCheckpointCoordinates)
            val tvTimestamp: TextView = itemView.findViewById(R.id.tvCheckpointTimestamp)
            val tvNotes: TextView = itemView.findViewById(R.id.tvCheckpointNotes)
            val viewTimeline: View = itemView.findViewById(R.id.viewTimeline)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckpointViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_checkpoint, parent, false)
            return CheckpointViewHolder(view)
        }

        override fun onBindViewHolder(holder: CheckpointViewHolder, position: Int) {
            val checkpoint = checkpoints[position]

            holder.tvLocationName.text = checkpoint.locationName
            holder.tvStatus.text = checkpoint.status
            holder.tvCoordinates.text = "Lat: ${checkpoint.latitude}, Lng: ${checkpoint.longitude}"

            if (checkpoint.timestamp != null) {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                holder.tvTimestamp.text = dateFormat.format(checkpoint.timestamp)
            } else {
                holder.tvTimestamp.text = "N/A"
            }

            if (checkpoint.notes.isNotEmpty()) {
                holder.tvNotes.visibility = View.VISIBLE
                holder.tvNotes.text = checkpoint.notes
            } else {
                holder.tvNotes.visibility = View.GONE
            }

            // Show timeline connector except for last item
            holder.viewTimeline.visibility = if (position == checkpoints.size - 1) View.INVISIBLE else View.VISIBLE
        }

        override fun getItemCount(): Int = checkpoints.size
    }
}