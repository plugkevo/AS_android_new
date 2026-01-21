package com.kevann.africanshipping25.loadinglists

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.firestore.FirebaseFirestore
import com.kevann.africanshipping25.R

// Ensure LoadingListItem data class is defined in LoadingListItem.kt
// Ensure OnLoadingListItemClickListener interface is defined in LoadingListAdapter.kt

class LoadingFragment : Fragment(), OnLoadingListItemClickListener {

    private lateinit var rvAllLoadingLists: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var loadingListAdapter: LoadingListAdapter
    private val allLoadingLists = mutableListOf<LoadingListItem>()
    private val filteredLoadingLists = mutableListOf<LoadingListItem>()
    private val firestore = FirebaseFirestore.getInstance()

    private lateinit var loadingAnimationView: LottieAnimationView
    private lateinit var contentLayout: LinearLayout // Declare contentLayout

    // Define status options for the Spinner in the update dialog
    private val loadingListStatusOptions = arrayOf("New", "Open", "Closed")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_loading, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views FIRST
        rvAllLoadingLists = view.findViewById(R.id.rv_all_loading_lists)
        etSearch = view.findViewById(R.id.et_search)
        loadingAnimationView = view.findViewById(R.id.loading_animation_view) // Initialize LottieAnimationView
        contentLayout = view.findViewById(R.id.content_layout) // Initialize contentLayout

        // Now that all views are initialized, proceed with other setup
        rvAllLoadingLists.layoutManager = LinearLayoutManager(requireContext())
        loadingListAdapter = LoadingListAdapter(filteredLoadingLists, this)
        rvAllLoadingLists.adapter = loadingListAdapter

        fetchLoadingLists() // This is now safe to call as loadingAnimationView is initialized
        setupSearch()
    }

    private fun showLoading() {
        loadingAnimationView.visibility = View.VISIBLE
        loadingAnimationView.playAnimation()
        rvAllLoadingLists.visibility = View.GONE // Hide content while loading
        etSearch.visibility = View.GONE // Hide search while loading
    }

    private fun hideLoading() {
        loadingAnimationView.visibility = View.GONE
        loadingAnimationView.pauseAnimation()
        rvAllLoadingLists.visibility = View.VISIBLE // Show content after loading
        etSearch.visibility = View.VISIBLE // Show search after loading
    }

    private fun fetchLoadingLists() {
        showLoading() // Show loading animation before fetching data
        firestore.collection("loading_lists")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)  // Order by timestamp, newest first
            .get()
            .addOnSuccessListener { querySnapshot ->
                allLoadingLists.clear()
                for (document in querySnapshot.documents) {
                    val loadingList = document.toObject(LoadingListItem::class.java)?.apply {
                        id = document.id
                    }
                    if (loadingList != null) {
                        allLoadingLists.add(loadingList)
                    }
                }
                filterLoadingLists(etSearch.text.toString())
                hideLoading() // Hide loading animation on success
            }
            .addOnFailureListener { e ->
                Log.e("LoadingFragment", "Error fetching loading lists: ", e)
                Toast.makeText(requireContext(), "Error loading lists: ${e.message}", Toast.LENGTH_SHORT).show()
                hideLoading() // Hide loading animation on failure
            }
    }
    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterLoadingLists(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterLoadingLists(searchText: String) {
        filteredLoadingLists.clear()
        val lowerCaseSearchText = searchText.lowercase()

        if (lowerCaseSearchText.isEmpty()) {
            filteredLoadingLists.addAll(allLoadingLists)
        } else {
            for (item in allLoadingLists) {
                if (item.name.lowercase().contains(lowerCaseSearchText) ||
                    item.origin.lowercase().contains(lowerCaseSearchText) ||
                    item.destination.lowercase().contains(lowerCaseSearchText) ||
                    item.extraDetails.lowercase().contains(lowerCaseSearchText)) {
                    filteredLoadingLists.add(item)
                }
            }
        }
        loadingListAdapter.notifyDataSetChanged()
    }

    // --- Implementation of OnLoadingListItemClickListener methods ---
    override fun onLoadingListItemClick(loadingList: LoadingListItem) {
        // Handle full item click, e.g., open a detailed view
        Toast.makeText(requireContext(), "Clicked Loading List: ${loadingList.name}", Toast.LENGTH_SHORT).show()
        // Example: Intent to a detail activity
        val intent = Intent(requireContext(), LoadingListDetailsActivity::class.java)
        intent.putExtra("loadingListId", loadingList.id)
        startActivity(intent)
    }

    // THIS IS THE SIMPLIFIED onMoreOptionsClick function
    override fun onMoreOptionsClick(loadingList: LoadingListItem, anchorView: View) {
        // Directly show the update dialog when the "More Options" button is clicked
        showUpdateLoadingListDialog(loadingList)
    }
    // --- END OF THE SIMPLIFIED onMoreOptionsClick function ---


    // --- showUpdateLoadingListDialog FUNCTION (remains the same) ---
    private fun showUpdateLoadingListDialog(loadingList: LoadingListItem) {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_update_loading_list, null) // Inflate your new dialog XML
        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.show()

        // Get references to views in the dialog
        val nameEditText = dialogView.findViewById<EditText>(R.id.et_name)
        val originEditText = dialogView.findViewById<EditText>(R.id.et_origin)
        val destinationEditText = dialogView.findViewById<EditText>(R.id.et_destination)
        val detailsEditText = dialogView.findViewById<EditText>(R.id.et_details)
        val statusSpinner = dialogView.findViewById<Spinner>(R.id.spinner_status)
        val updateButton = dialogView.findViewById<Button>(R.id.btn_update)
        val cancelButton = dialogView.findViewById<Button>(R.id.btn_cancel)

        // Populate the Spinner with status options
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            loadingListStatusOptions
        )
        statusSpinner.adapter = adapter

        // Set initial values from the passed loadingList object
        nameEditText.setText(loadingList.name)
        originEditText.setText(loadingList.origin)
        destinationEditText.setText(loadingList.destination)
        detailsEditText.setText(loadingList.extraDetails)

        // Set the initial selection of the status spinner
        loadingList.status.let { status ->
            val index = loadingListStatusOptions.indexOf(status)
            if (index != -1) {
                statusSpinner.setSelection(index)
            }
        }

        // Set click listener for the Update button
        updateButton.setOnClickListener {
            val updatedName = nameEditText.text.toString().trim()
            val updatedOrigin = originEditText.text.toString().trim()
            val updatedDestination = destinationEditText.text.toString().trim()
            val updatedDetails = detailsEditText.text.toString().trim()
            val updatedStatus = statusSpinner.selectedItem.toString()

            // Basic validation
            if (updatedName.isEmpty() || updatedOrigin.isEmpty() || updatedDestination.isEmpty()) {
                Toast.makeText(requireContext(), "Name, Origin, and Destination are required.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create a map of updated data
            val updatedLoadingListData = hashMapOf(
                "name" to updatedName,
                "origin" to updatedOrigin,
                "destination" to updatedDestination,
                "extraDetails" to updatedDetails,
                "status" to updatedStatus
            )

            // Update the document in Firestore
            firestore.collection("loading_lists").document(loadingList.id)
                .update(updatedLoadingListData as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Loading List updated successfully!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    fetchLoadingLists() // Refresh the list to show updated data
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error updating loading list: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("LoadingFragment", "Error updating loading list document", e)
                }
        }

        // Set click listener for the Cancel button
        cancelButton.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(requireContext(), "Update cancelled.", Toast.LENGTH_SHORT).show()
        }
    }
}