package com.example.africanshipping25

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText // Import EditText
import android.widget.TextView // Import TextView for emptyView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

private const val ARG_LOADING_LIST_ID = "loadingListId"
private const val WAREHOUSE_ITEMS_COLLECTION = "warehouseItems"
private const val LOADING_LISTS_COLLECTION = "loading_lists"

class ViewWarehouseGoods : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WarehouseItemAdapter
    // itemList will now hold the original, unfiltered data from Firestore
    private val itemList = mutableListOf<WarehouseItem>()
    private lateinit var firestore: FirebaseFirestore
    private var listenerRegistration: ListenerRegistration? = null
    private var loadingListId: String? = null

    // For the EditText search
    private lateinit var searchEditText: EditText
    private lateinit var emptyView: TextView // Reference to your empty view

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            loadingListId = it.getString(ARG_LOADING_LIST_ID)
        }
        Log.d("ViewWarehouseGoods", "onCreate: Received loadingListId: $loadingListId")

        firestore = FirebaseFirestore.getInstance()

        // No setHasOptionsMenu(true) needed as we're not using a menu for search
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_view_warehouse_goods, container, false) // Use your new layout name

        // Initialize UI elements
        recyclerView = view.findViewById(R.id.storeInventoryRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        searchEditText = view.findViewById(R.id.searchEditText)
        emptyView = view.findViewById(R.id.emptyView)


        // Pass the original itemList to the adapter. The adapter will manage its own filtered list.
        adapter = WarehouseItemAdapter(itemList) { updatedItem ->
            updateItemInFirestore(updatedItem)
        }
        recyclerView.adapter = adapter

        // Set up the TextWatcher for the searchEditText
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not needed for this implementation
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Apply filter as text changes
                adapter.filter.filter(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {
                // Not needed for this implementation
            }
        })

        loadItemsFromFirestore()

        return view
    }

    // No onCreateOptionsMenu or onOptionsItemSelected needed for EditText search

    private fun loadItemsFromFirestore() {
        loadingListId?.let { id ->
            val collectionPath = "$LOADING_LISTS_COLLECTION/$id/$WAREHOUSE_ITEMS_COLLECTION"
            Log.d("ViewWarehouseGoods", "Attempting to load items from Firestore path: $collectionPath")

            listenerRegistration = firestore.collection(LOADING_LISTS_COLLECTION)
                .document(id)
                .collection(WAREHOUSE_ITEMS_COLLECTION)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("ViewWarehouseGoods", "Error fetching documents: ${error.message}", error)
                        Toast.makeText(requireContext(), "Error loading items: ${error.message}", Toast.LENGTH_LONG).show()
                        updateEmptyView(true) // Show empty view on error
                        return@addSnapshotListener
                    }

                    if (snapshot == null) {
                        Log.w("ViewWarehouseGoods", "Snapshot is null, no data received.")
                        updateEmptyView(true) // Show empty view if snapshot is null
                        return@addSnapshotListener
                    }

                    // Temporarily clear the original list, then repopulate
                    itemList.clear()
                    if (snapshot.isEmpty) {
                        Log.d("ViewWarehouseGoods", "No documents found in '$WAREHOUSE_ITEMS_COLLECTION' for loadingListId: $id")
                        updateEmptyView(true) // Show empty view if no data
                    } else {
                        Log.d("ViewWarehouseGoods", "Found ${snapshot.documents.size} documents in '$WAREHOUSE_ITEMS_COLLECTION' for loadingListId: $id")
                        updateEmptyView(false) // Hide empty view if data exists
                    }

                    for (doc in snapshot.documents) {
                        val item = doc.toObject(WarehouseItem::class.java)
                        if (item != null) {
                            itemList.add(item.copy(id = doc.id))
                            Log.d("ViewWarehouseGoods", "Added item: GoodNo=${item.goodNo}, GoodsName=${item.goodsName}, Sender=${item.senderName} (Firestore ID: ${doc.id})")
                        } else {
                            Log.e("ViewWarehouseGoods", "Failed to convert document ${doc.id} to WarehouseItem. Data: ${doc.data}")
                        }
                    }

                    // After loading, update the adapter's internal list (which includes the originalItems)
                    // and apply any current filter
                    adapter.getFilter().filter(searchEditText.text.toString()) // Re-apply current filter if any
                    Log.d("ViewWarehouseGoods", "Adapter notified. Total items in list: ${itemList.size}")
                }
        } ?: run {
            Log.e("ViewWarehouseGoods", "loadingListId is NULL. Cannot load items.")
            Toast.makeText(requireContext(), "Error: Loading list ID is missing. Cannot retrieve items.", Toast.LENGTH_LONG).show()
            updateEmptyView(true) // Show empty view if ID is null
        }
    }

    private fun updateEmptyView(show: Boolean) {
        if (show) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }
    }

    private fun updateItemInFirestore(item: WarehouseItem) {
        loadingListId?.let { id ->
            if (item.id.isEmpty()) {
                Log.e("ViewWarehouseGoods", "Attempted to update item with empty ID: $item")
                Toast.makeText(requireContext(), "Error: Item ID is missing for update.", Toast.LENGTH_SHORT).show()
                return@let
            }
            Log.d("ViewWarehouseGoods", "Updating item with ID: ${item.id} in path: $LOADING_LISTS_COLLECTION/$id/$WAREHOUSE_ITEMS_COLLECTION")

            firestore.collection(LOADING_LISTS_COLLECTION)
                .document(id)
                .collection(WAREHOUSE_ITEMS_COLLECTION)
                .document(item.id)
                .set(item)
                .addOnSuccessListener {
                    Log.d("ViewWarehouseGoods", "Item ${item.id} updated successfully.")
                    Toast.makeText(requireContext(), "Updated successfully", Toast.LENGTH_SHORT).show()
                    // Firestore listener will automatically update the list if the current filter matches
                }
                .addOnFailureListener { e ->
                    Log.e("ViewWarehouseGoods", "Update failed for item ${item.id}: ${e.message}", e)
                    Toast.makeText(requireContext(), "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            Log.e("ViewWarehouseGoods", "loadingListId is NULL. Cannot update item.")
            Toast.makeText(requireContext(), "Error: Loading list ID is missing. Cannot update item.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
        Log.d("ViewWarehouseGoods", "onDestroyView: Firestore listener removed.")
    }

    companion object {
        @JvmStatic
        fun newInstance(loadingListId: String) =
            ViewWarehouseGoods().apply {
                arguments = Bundle().apply {
                    putString(ARG_LOADING_LIST_ID, loadingListId)
                }
            }
    }
}