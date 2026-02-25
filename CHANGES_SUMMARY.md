## Changes Summary: Moved Search and Create Loading List

### What Was Changed

1. **Removed "Search All Goods" from Toolbar Menu**
   - Removed the menu item from `menu_toolbar_options.xml`
   - Removed the handler from `MainActivity.showMoreOptionsMenu()`
   - Removed the translation logic for the search menu item
   - Removed the `navigateToGlobalSearch()` method from MainActivity
   - Removed the GlobalSearchFragment import from MainActivity

2. **Added to Loading Fragment Layout**
   - Added two buttons at the top of `fragment_loading.xml`:
     - "Create Loading List" button (Primary color)
     - "Search All Goods" button (Secondary color)
   - Buttons are placed in a horizontal LinearLayout with equal weight distribution

3. **Updated LoadingFragment Class**
   - Added import for `GlobalSearchFragment`
   - Added button click listeners in `onViewCreated()`
   - Added `showCreateLoadingListDialog()` method to create new loading lists directly from the fragment
   - Added `navigateToGlobalSearch()` method to navigate to the Global Search Fragment

### File Changes Made

1. `/vercel/share/v0-project/app/src/main/res/menu/menu_toolbar_options.xml`
   - Removed `action_global_search` menu item

2. `/vercel/share/v0-project/app/src/main/java/com/kevann/africanshipping25/MainActivity.kt`
   - Removed search handling from `showMoreOptionsMenu()`
   - Removed search translation from `translatePopupMenu()`
   - Removed `navigateToGlobalSearch()` method
   - Removed GlobalSearchFragment import
   - Removed GlobalSearchFragment case from onBackPressed handler

3. `/vercel/share/v0-project/app/src/main/res/layout/fragment_loading.xml`
   - Added two action buttons above the search CardView

4. `/vercel/share/v0-project/app/src/main/java/com/kevann/africanshipping25/fragments/LoadingFragment.kt`
   - Added GlobalSearchFragment import
   - Added button initialization and click listeners
   - Added `showCreateLoadingListDialog()` for creating loading lists
   - Added `navigateToGlobalSearch()` for searching goods

### User Workflow Now

Users can now access both features from the Loading Fragment:
1. Click "Create Loading List" to create a new loading list directly from the fragment
2. Click "Search All Goods" to search across all shipments' inventories globally
