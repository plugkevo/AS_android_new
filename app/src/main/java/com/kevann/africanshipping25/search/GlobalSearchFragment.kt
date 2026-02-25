package com.kevann.africanshipping25.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.kevann.africanshipping25.R

class GlobalSearchFragment : Fragment() {

    private lateinit var searchViewModel: GlobalSearchViewModel
    private lateinit var searchEditText: EditText
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var emptyStateTextView: TextView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var shipmentFilterSpinner: Spinner
    private lateinit var categoryFilterSpinner: Spinner
    private lateinit var resultsAdapter: GlobalSearchResultsAdapter
    private lateinit var lottieLoadingAnimation: LottieAnimationView
    private lateinit var lottieNoDataAnimation: LottieAnimationView

    private val TAG = "GlobalSearchFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_global_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        searchViewModel = ViewModelProvider(this).get(GlobalSearchViewModel::class.java)

        // Initialize UI components
        initializeViews(view)

        // Set up RecyclerView
        setupRecyclerView()

        // Set up text search listener
        setupSearchTextListener()

        // Set up filter spinners
        setupFilterSpinners()

        // Observe ViewModel LiveData
        observeViewModelData()
    }

    private fun initializeViews(view: View) {
        searchEditText = view.findViewById(R.id.search_edit_text)
        resultsRecyclerView = view.findViewById(R.id.results_recycler_view)
        emptyStateTextView = view.findViewById(R.id.empty_state_text_view)
        loadingProgressBar = view.findViewById(R.id.loading_progress_bar)
        shipmentFilterSpinner = view.findViewById(R.id.shipment_filter_spinner)
        categoryFilterSpinner = view.findViewById(R.id.category_filter_spinner)
        lottieLoadingAnimation = view.findViewById(R.id.lottie_loading_animation)
        lottieNoDataAnimation = view.findViewById(R.id.lottie_no_data_animation)
    }

    private fun setupRecyclerView() {
        resultsAdapter = GlobalSearchResultsAdapter(mutableListOf()) { selectedItem ->
            showResultDetails(selectedItem)
        }
        resultsRecyclerView.apply {
            adapter = resultsAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(false)
        }
    }

    private fun setupSearchTextListener() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                if (query.length >= 2) {
                    searchViewModel.performSearch(query)
                } else if (query.isEmpty()) {
                    searchViewModel.clearSearch()
                }
            }
        })
    }

    private fun setupFilterSpinners() {
        // Shipment filter spinner
        val shipmentSpinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            mutableListOf("All Shipments")
        )
        shipmentFilterSpinner.adapter = shipmentSpinnerAdapter

        shipmentFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = parent?.getItemAtPosition(position) as? String
                if (selected == "All Shipments") {
                    searchViewModel.filterByShipment(null)
                } else {
                    // Find the shipment ID for the selected name
                    searchViewModel.shipmentsList.value?.find { it.second == selected }?.let {
                        searchViewModel.filterByShipment(it.first)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Category filter spinner
        val categorySpinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("All Categories", "Store", "Truck")
        )
        categoryFilterSpinner.adapter = categorySpinnerAdapter

        categoryFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> searchViewModel.filterByCategory(null)
                    1 -> searchViewModel.filterByCategory("store")
                    2 -> searchViewModel.filterByCategory("truck")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun observeViewModelData() {
        // Observe search results
        searchViewModel.searchResults.observe(viewLifecycleOwner) { results ->
            Log.d(TAG, "Results updated: ${results.size} items")
            updateResultsUI(results)
        }

        // Observe loading state
        searchViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            lottieLoadingAnimation.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe shipments for filter dropdown
        searchViewModel.shipmentsList.observe(viewLifecycleOwner) { shipments ->
            val shipmentNames = mutableListOf("All Shipments")
            shipmentNames.addAll(shipments.map { it.second })
            (shipmentFilterSpinner.adapter as ArrayAdapter<String>).apply {
                clear()
                addAll(shipmentNames)
                notifyDataSetChanged()
            }
        }
    }

    private fun updateResultsUI(results: List<SearchResultItem>) {
        if (results.isEmpty()) {
            if (searchEditText.text.isNotBlank()) {
                emptyStateTextView.visibility = View.VISIBLE
                emptyStateTextView.text = "No goods found matching \"${searchEditText.text}\""
                lottieNoDataAnimation.visibility = View.VISIBLE
            } else {
                emptyStateTextView.visibility = View.GONE
                lottieNoDataAnimation.visibility = View.GONE
            }
            resultsRecyclerView.visibility = View.GONE
        } else {
            emptyStateTextView.visibility = View.GONE
            lottieNoDataAnimation.visibility = View.GONE
            resultsRecyclerView.visibility = View.VISIBLE
            resultsAdapter.updateResults(results)
            Log.d(TAG, "Updated adapter with ${results.size} results")
        }
    }

    private fun showResultDetails(item: SearchResultItem) {
        // You can implement this to show a dialog with full details
        Log.d(TAG, "Selected item: ${item.name} from ${item.shipmentName} (${item.category})")
        // TODO: Implement detail view or navigation
    }

    companion object {
        fun newInstance() = GlobalSearchFragment()
    }
}
