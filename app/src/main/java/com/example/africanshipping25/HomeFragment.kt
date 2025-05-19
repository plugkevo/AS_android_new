package com.example.africanshipping25

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore


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

        // Example: Assuming you have a button in your HomeFragment layout
        // that triggers the display of the create shipment dialog
        val showCreateDialogButton = view.findViewById<CardView>(R.id.card_create_shipment)
        showCreateDialogButton?.setOnClickListener {
            showCreateShipmentDialog()
        }

        val mapsbtn = view.findViewById<CardView>(R.id.card_track_shipment)
        mapsbtn.setOnClickListener {
            val intent= Intent(requireContext(), MapsActivity::class.java)
            startActivity(intent)
        }

        // Initialize other home screen UI elements and listeners here
    }

    private fun showCreateShipmentDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_new_shipment_dialog, null)
        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.show()

        val nameEditText = dialogView.findViewById<EditText>(R.id.et_name)
        val originEditText = dialogView.findViewById<EditText>(R.id.et_origin)
        val destinationEditText = dialogView.findViewById<EditText>(R.id.et_destination)
        val weightEditText = dialogView.findViewById<EditText>(R.id.et_weight)
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

            // Create a map or a data class to hold the shipment data
            val shipment = hashMapOf(
                "name" to name,
                "origin" to origin,
                "destination" to destination,
                "weight" to weight
                // You can add more fields like timestamp, user ID, etc.
            )

            // Add a new document to the "shipments" collection
            firestore.collection("shipments")
                .add(shipment)
                .addOnSuccessListener { documentReference ->
                    Toast.makeText(requireContext(), "Shipment created with ID: ${documentReference.id}", Toast.LENGTH_SHORT).show()
                    dialog.dismiss() // Dismiss the dialog after successful creation
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error creating shipment: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
    }
}