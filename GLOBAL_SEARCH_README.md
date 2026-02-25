# Global Search Feature - Implementation Guide

## Overview
The Global Search feature allows users to search for goods across **all shipments** and both **store** and **truck** inventory in a single search operation, eliminating the need to search within individual shipments separately.

## Architecture

### Components

#### 1. **GlobalSearchRepository** (`GlobalSearchRepository.kt`)
- Handles all Firestore database queries
- Searches across all shipments' subcollections simultaneously
- Returns combined results with shipment metadata
- Key Methods:
  - `searchAllGoodsAcrossShipments(searchQuery)` - Main search function
  - `searchInStoreInventory()` - Searches store_inventory subcollection
  - `searchInTruckInventory()` - Searches truck_inventory subcollection
  - `getAllShipmentNames()` - Loads available shipments for filters

#### 2. **GlobalSearchViewModel** (`GlobalSearchViewModel.kt`)
- Manages search state and lifecycle
- Applies filters (by shipment, by category)
- LiveData observables for UI updates
- Key Methods:
  - `performSearch(query)` - Initiates search
  - `filterByShipment(shipmentId)` - Filter results by shipment
  - `filterByCategory(category)` - Filter by store/truck
  - `clearSearch()` - Reset search

#### 3. **GlobalSearchFragment** (`GlobalSearchFragment.kt`)
- UI Fragment for search interface
- Handles user input and filter selection
- Observes ViewModel for live updates
- Features:
  - Real-time search (2+ characters)
  - Shipment dropdown filter
  - Category filter (Store/Truck)
  - Loading states with Lottie animations
  - Empty states display

#### 4. **GlobalSearchResultsAdapter** (`GlobalSearchResultsAdapter.kt`)
- RecyclerView adapter for displaying search results
- Shows:
  - Goods name
  - Goods number (#ID)
  - Location (store or truck)
  - Shipment name
  - Category badge (color-coded)

### Data Model

#### SearchResultItem
```kotlin
data class SearchResultItem(
    val goodsNumber: Long? = null,
    val name: String? = null,
    val storeLocation: String? = null,  // For store goods
    val shipmentId: String = "",
    val shipmentName: String = "",
    val category: String = "",  // "store" or "truck"
    val truckLocation: String? = null  // For truck goods
)
```

## How It Works

### Search Flow
1. User enters search query (minimum 2 characters)
2. ViewModel calls `performSearch(query)`
3. Repository fetches all shipments from Firestore
4. For each shipment:
   - Query `store_inventory` subcollection
   - Query `truck_inventory` subcollection
5. Results are combined with shipment metadata
6. Filters are applied (if any)
7. Results are displayed in RecyclerView

### Search Algorithm
- Case-insensitive matching
- Searches across:
  - Goods name
  - Goods number
  - Location (store or truck)
- Results sorted by shipment name

## UI Components

### Layouts
- **fragment_global_search.xml** - Main search interface
  - Search EditText
  - Shipment filter Spinner
  - Category filter Spinner
  - Loading animation
  - Results RecyclerView
  - Empty state message

- **item_global_search_result.xml** - Individual result card
  - Goods name and number
  - Category badge (Store/Truck)
  - Location
  - Shipment name
  - Click handler

### Colors
- `category_store` - Green (#4CAF50) for store goods
- `category_truck` - Orange (#FF9800) for truck goods

## Navigation Integration

### Access Points
1. **Toolbar Menu** - "Search All Goods" option
   - Added to `menu_toolbar_options.xml`
   - Translatable menu item

2. **MainActivity**
   - `navigateToGlobalSearch()` - Navigate to search fragment
   - Added to back stack
   - Toolbar title translation support

## Filter Features

### Shipment Filter
- Dropdown with all available shipments
- "All Shipments" option to clear filter
- Defaults to searching all shipments

### Category Filter
- Options: All Categories, Store, Truck
- Filters results to specific inventory type

## Performance Considerations

### Optimization
- Parallel queries for store and truck inventory
- Results returned as soon as all queries complete
- LiveData ensures UI only updates on main thread
- Loading state prevents multiple simultaneous searches

### Scalability
- Works efficiently with multiple shipments
- Backend-based search (Firestore handles indexing)
- No data loaded into memory until needed

## Usage Example

### Starting a Search
```kotlin
// User types in search field
override fun afterTextChanged(s: Editable?) {
    val query = s?.toString()?.trim() ?: ""
    if (query.length >= 2) {
        searchViewModel.performSearch(query)
    }
}
```

### Observing Results
```kotlin
searchViewModel.searchResults.observe(viewLifecycleOwner) { results ->
    resultsAdapter.updateResults(results)
}
```

### Filtering
```kotlin
// Filter by specific shipment
searchViewModel.filterByShipment(shipmentId)

// Filter by category
searchViewModel.filterByCategory("store")
```

## Future Enhancements

### Possible Improvements
1. Search history/recent searches
2. Advanced filters (date range, location range)
3. Bulk actions on search results
4. Export search results
5. Search suggestions/autocomplete
6. Full-text search indexing for better performance
7. Offline search caching
8. Search analytics

## Testing Considerations

### Test Scenarios
1. Search with empty query - Should show empty state
2. Search with 1 character - Should not trigger search
3. Search with special characters - Should handle gracefully
4. Search across multiple shipments - Should combine results
5. Filter by shipment - Should show only selected shipment results
6. Filter by category - Should show only store or truck goods
7. Combined filters - Should apply both filters correctly
8. No results found - Should show empty state message
9. Loading state - Should show animation
10. Network error - Should show error handling

## File Locations

```
app/src/main/java/com/kevann/africanshipping25/search/
├── GlobalSearchRepository.kt
├── GlobalSearchViewModel.kt
├── GlobalSearchFragment.kt
└── GlobalSearchResultsAdapter.kt

app/src/main/res/layout/
├── fragment_global_search.xml
└── item_global_search_result.xml

app/src/main/res/drawable/
├── rounded_edittext_background.xml
├── spinner_background.xml (existing)
├── item_background.xml
└── badge_background.xml

app/src/main/res/values/
└── colors.xml (updated with category colors)

app/src/main/res/menu/
└── menu_toolbar_options.xml (updated)
```

## Dependencies
- AndroidX (Fragment, ViewModel, LiveData, RecyclerView)
- Firebase Firestore
- Kotlin Coroutines
- Lottie Animations (for loading/empty states)
