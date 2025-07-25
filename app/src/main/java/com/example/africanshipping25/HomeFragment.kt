// com.example.africanshipping25/HomeFragment.kt
package com.example.africanshipping25

import android.app.AlertDialog
import android.content.Intent
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

// Implement the OnShipmentItemClickListener interface
class HomeFragment : Fragment(), ShipmentAdapter.OnShipmentItemClickListener { // <<< Make sure this is here

    private lateinit var firestore: FirebaseFirestore
    private lateinit var tvActiveCount: TextView
    private lateinit var tvDeliveredCount: TextView
    private lateinit var rvAllShipments: RecyclerView
    private lateinit var homeShipmentAdapter: HomeShipmentAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()

        tvActiveCount = view.findViewById(R.id.tv_active_count)
        tvDeliveredCount = view.findViewById(R.id.tv_delivered_count)
        rvAllShipments = view.findViewById(R.id.rv_all_shipments)

        // Initialize HomeShipmentAdapter and pass 'this' as the itemClickListener
        homeShipmentAdapter = HomeShipmentAdapter(mutableListOf(), this) // <<< Pass 'this'
        rvAllShipments.layoutManager = LinearLayoutManager(requireContext())
        rvAllShipments.adapter = homeShipmentAdapter

        loadShipmentCounts()
        loadLatestShipments()

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
                Log.d("HomeFragment", "Active Shipments Count: $activeCount")
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "Error getting active shipments count: ", e)
                tvActiveCount.text = "Error"
            }

        firestore.collection("shipments")
            .whereEqualTo("status", "Delivered")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val deliveredCount = querySnapshot.size()
                tvDeliveredCount.text = deliveredCount.toString()
                Log.d("HomeFragment", "Delivered Shipments Count: $deliveredCount")
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "Error getting delivered shipments count: ", e)
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
                Log.d("HomeFragment", "Loaded ${latestShipments.size} latest shipments.")
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "Error getting latest shipments: ", e)
                Toast.makeText(requireContext(), "Error loading recent shipments: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showCreateShipmentDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_new_shipment, null)
        builder.setView(dialogView)
        val dialog = builder.create()
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
                Toast.makeText(requireContext(), "Please fill in all the details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val weight = weightStr // treat it as a string


            val shipment = hashMapOf(
                "name" to name,
                "origin" to origin,
                "destination" to destination,
                "weight" to weight,  // now it's a string
                "status" to "Active",
                "createdAt" to FieldValue.serverTimestamp()
            )


            firestore.collection("shipments")
                .add(shipment)
                .addOnSuccessListener { documentReference ->
                    Toast.makeText(requireContext(), "Shipment created successfully!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadShipmentCounts()
                    loadLatestShipments()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error creating shipment: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun showCreateLoadingListDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_loading, null)
        builder.setView(dialogView)

        val dialog = builder.create()
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
                Toast.makeText(requireContext(), "Name, Origin, and Destination are required.", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(requireContext(), "Loading List created successfully!", Toast.LENGTH_SHORT).show()
                    Log.d("HomeFragment", "Loading List Document added with ID: ${documentReference.id}")
                    dialog.dismiss()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error creating loading list: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("HomeFragment", "Error adding loading list document", e)
                }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(requireContext(), "Loading list creation cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    // Implement the onShipmentItemClick method for HomeFragment
    override fun onShipmentItemClick(shipment: Shipment) { // <<< This method handles the click
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
}