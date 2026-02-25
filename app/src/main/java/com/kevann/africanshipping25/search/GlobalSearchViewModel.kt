package com.kevann.africanshipping25.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.util.Log

class GlobalSearchViewModel : ViewModel() {
    private val repository = GlobalSearchRepository()
    private val TAG = "GlobalSearchViewModel"

    private val _searchResults = MutableLiveData<List<SearchResultItem>>()
    val searchResults: LiveData<List<SearchResultItem>> = _searchResults

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _selectedShipmentFilter = MutableLiveData<String?>(null)
    val selectedShipmentFilter: LiveData<String?> = _selectedShipmentFilter

    private val _selectedCategoryFilter = MutableLiveData<String?>(null)
    val selectedCategoryFilter: LiveData<String?> = _selectedCategoryFilter

    private val _shipmentsList = MutableLiveData<List<Pair<String, String>>>()
    val shipmentsList: LiveData<List<Pair<String, String>>> = _shipmentsList

    init {
        loadShipments()
    }

    /**
     * Perform search across all shipments
     */
    fun performSearch(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _searchQuery.value = ""
            return
        }

        _searchQuery.value = query
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val results = repository.searchAllGoodsAcrossShipments(query)
                val filteredResults = applyFilters(results)
                _searchResults.value = filteredResults
                Log.d(TAG, "Search completed: found ${filteredResults.size} results")
            } catch (e: Exception) {
                Log.e(TAG, "Search error: ${e.message}")
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear search results
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    /**
     * Filter results by shipment
     */
    fun filterByShipment(shipmentId: String?) {
        _selectedShipmentFilter.value = shipmentId
        val query = _searchQuery.value ?: ""
        if (query.isNotBlank()) {
            performSearch(query)
        }
    }

    /**
     * Filter results by category (store or truck)
     */
    fun filterByCategory(category: String?) {
        _selectedCategoryFilter.value = category
        val query = _searchQuery.value ?: ""
        if (query.isNotBlank()) {
            performSearch(query)
        }
    }

    /**
     * Apply active filters to results
     */
    private fun applyFilters(results: List<SearchResultItem>): List<SearchResultItem> {
        var filtered = results

        // Apply shipment filter
        _selectedShipmentFilter.value?.let { shipmentId ->
            filtered = filtered.filter { it.shipmentId == shipmentId }
        }

        // Apply category filter
        _selectedCategoryFilter.value?.let { category ->
            filtered = filtered.filter { it.category == category }
        }

        return filtered
    }

    /**
     * Load all available shipments for filter dropdown
     */
    private fun loadShipments() {
        viewModelScope.launch {
            try {
                val shipments = repository.getAllShipmentNames()
                _shipmentsList.value = shipments
                Log.d(TAG, "Loaded ${shipments.size} shipments")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading shipments: ${e.message}")
                _shipmentsList.value = emptyList()
            }
        }
    }
}
