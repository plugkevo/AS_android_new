# Navigation Reorganization Summary

## Changes Made

### 1. Quick Actions Update (Home Fragment)
Replaced "Loading List" quick action with "Search All Goods" that navigates to the Global Search Fragment.

**File Changes:**
- `fragment_home.xml`: Replaced card_loading with card_search_goods
- `HomeFragment.kt`: Added GlobalSearchFragment navigation, removed loading list dialog

### 2. Loading Fragment Update  
Added Floating Action Button for creating loading lists instead of action buttons.

**File Changes:**
- `fragment_loading.xml`: Added fab_create_loading_list FloatingActionButton
- `LoadingFragment.kt`: Updated to use FAB instead of buttons, removed search navigation

### 3. Navigation Flow
- Quick Actions (Home): Search Goods now navigates to Global Search
- Loading Lists Fragment: FAB button opens Create Loading List dialog
