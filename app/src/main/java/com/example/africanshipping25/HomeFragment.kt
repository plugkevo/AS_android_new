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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query // Import Query (if needed elsewhere in your fragment)

class HomeFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var tvActiveCount: TextView
    private lateinit var tvDeliveredCount: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()

        tvActiveCount = view.findViewById(R.id.tv_active_count)
        tvDeliveredCount = view.findViewById(R.id.tv_delivered_count)

        loadShipmentCounts()

        // This is your EXISTING dialog trigger (presumably for fragment_new_shipment_dialog)
        val showCreateShipmentDialogButton = view.findViewById<CardView>(R.id.card_create_shipment)
        showCreateShipmentDialogButton?.setOnClickListener {
            // This calls your ORIGINAL dialog function
            showCreateShipmentDialog()
        }

        // --- NEW DIALOG TRIGGER ---
        // Assuming you have another CardView or Button in your fragment_home.xml
        // for triggering the "Create New Loading List" dialog.
        // Replace R.id.your_new_loading_list_trigger_card with the actual ID from your layout
        val showCreateLoadingListDialogButton = view.findViewById<CardView>(R.id.card_loading) // <--- IMPORTANT: Replace with your actual ID
        showCreateLoadingListDialogButton?.setOnClickListener {
            // This calls the NEW dialog function
            showCreateLoadingListDialog()
        }
        // --------------------------


        val mapsbtn = view.findViewById<CardView>(R.id.card_track_shipment)
        mapsbtn.setOnClickListener {
            val intent = Intent(requireContext(), MapsActivity::class.java)
            startActivity(intent)
        }


    }

    private fun loadShipmentCounts() {
        // Query for Active Shipments
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

        // Query for Delivered Shipments
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

    // --- YOUR ORIGINAL DIALOG FUNCTION - UNTOUCHED ---
    private fun showCreateShipmentDialog() {
        val builder = AlertDialog.Builder(requireContext())
        // Assuming this dialog uses fragment_new_shipment_dialog.xml as you mentioned initially
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_new_shipment_dialog, null)
        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.show()

        val nameEditText = dialogView.findViewById<EditText>(R.id.et_name)
        val originEditText = dialogView.findViewById<EditText>(R.id.et_origin)
        val destinationEditText = dialogView.findViewById<EditText>(R.id.et_destination)
        val weightEditText = dialogView.findViewById<EditText>(R.id.et_weight) // This was likely a weight field previously
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

            val weight = weightStr.toDoubleOrNull()
            if (weight == null) {
                Toast.makeText(requireContext(), "Invalid weight", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val shipment = hashMapOf(
                "name" to name,
                "origin" to origin,
                "destination" to destination,
                "weight" to weight,
                "status" to "pending"
            )

            firestore.collection("shipments")
                .add(shipment)
                .addOnSuccessListener { documentReference ->
                    Toast.makeText(requireContext(), "Shipment created with ID: ${documentReference.id}", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadShipmentCounts()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error creating shipment: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
    }
    // --- END OF ORIGINAL DIALOG FUNCTION ---


    // --- START OF NEW DIALOG FUNCTION FOR LOADING LISTS ---
    private fun showCreateLoadingListDialog() {
        val builder = AlertDialog.Builder(requireContext())
        // Inflate the custom layout from YOUR PROVIDED XML (loading_list_dialog_layout.xml)
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_loading, null)
        builder.setView(dialogView)

        val dialog = builder.create()
        dialog.show()

        // Get references to the EditText and Button views from the inflated dialog layout
        val nameEditText = dialogView.findViewById<EditText>(R.id.et_name)
        val originEditText = dialogView.findViewById<EditText>(R.id.et_origin)
        val destinationEditText = dialogView.findViewById<EditText>(R.id.et_destination)
        val extraDetailsEditText = dialogView.findViewById<EditText>(R.id.et_weight) // This is your "Any extra info" field
        val createButton = dialogView.findViewById<Button>(R.id.btn_create)
        val cancelButton = dialogView.findViewById<Button>(R.id.btn_cancel)

        createButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val origin = originEditText.text.toString().trim()
            val destination = destinationEditText.text.toString().trim()
            val extraDetails = extraDetailsEditText.text.toString().trim()

            // Basic validation for mandatory fields
            if (name.isEmpty() || origin.isEmpty() || destination.isEmpty()) {
                Toast.makeText(requireContext(), "Name, Origin, and Destination are required.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create a map to hold the loading list data
            val loadingList = hashMapOf(
                "name" to name,
                "origin" to origin,
                "destination" to destination,
                "extraDetails" to extraDetails, // Storing "Any extra info"
                "status" to "New" // Initial status for a new loading list entry
            )

            // Add a new document to a *different* collection, e.g., "loading_lists"
            // to keep it separate from "shipments", or adjust if "shipments" should also contain this
            firestore.collection("loading_lists") // <--- IMPORTANT: Consider a new collection name if these are distinct from "shipments"
                .add(loadingList)
                .addOnSuccessListener { documentReference ->
                    Toast.makeText(requireContext(), "Loading List created successfully!", Toast.LENGTH_SHORT).show()
                    Log.d("HomeFragment", "Loading List Document added with ID: ${documentReference.id}")
                    dialog.dismiss() // Dismiss the dialog after successful creation
                    // You might want to update a different count or UI element here
                    // if loading lists have their own section/counts.
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
    // --- END OF NEW DIALOG FUNCTION ---
}