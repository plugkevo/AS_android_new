package com.example.africanshipping25

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment

class NewShipmentDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_new_shipment_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the dialog UI elements and listeners
        val cancelButton = view.findViewById<Button>(R.id.btn_cancel)
        val createButton = view.findViewById<Button>(R.id.btn_create)

        cancelButton.setOnClickListener {
            dismiss()
        }

        createButton.setOnClickListener {
            // Handle shipment creation logic
            // For now, just dismiss the dialog
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        // Make the dialog wider
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}