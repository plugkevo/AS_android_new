# Offline-First Architecture Implementation

This document explains the offline-first functionality added to the African Shipping application.

## Overview

The application now supports offline data entry for three key features:
1. Truck Goods Entry
2. Store Goods Entry
3. Loading List Creation

Data is stored locally in Room database and automatically synced to Firestore when the device comes online.

## Architecture Components

### 1. Room Database Layer (`database/`)

**OfflineEntities.kt**
- `TruckGoodsEntity` - Local storage for truck goods with shipmentId, name, goodsNumber, sync status
- `StoreGoodsEntity` - Local storage for store goods with shipmentId, name, location, quantity, sync status
- `LoadingListEntity` - Local storage for loading lists with name, origin, destination, sync status

**OfflineDao.kt**
- `TruckGoodsDao` - CRUD operations and unsynced data queries for truck goods
- `StoreGoodsDao` - CRUD operations and unsynced data queries for store goods
- `LoadingListDao` - CRUD operations, search, and unsynced data queries for loading lists

**OfflineDatabase.kt**
- Singleton Room database instance
- Manages database creation and access
- Provides DAO instances

### 2. Sync Layer (`sync/`)

**SyncManager.kt**
- Monitors network connectivity
- Syncs unsynced data to Firestore when online
- Handles three types of data: truck goods, store goods, loading lists
- Marks data as synced after successful Firebase upload
- Includes error handling and logging

**ConnectivityReceiver.kt**
- BroadcastReceiver that listens for network connectivity changes
- Automatically triggers sync when device comes online
- Can be registered/unregistered in activities/fragments

## How to Use

### 1. In Activities/Fragments

**Initialize sync receiver** (in onCreate or onCreateView):
```kotlin
private val connectivityReceiver = ConnectivityReceiver { isConnected ->
    if (isConnected) {
        Toast.makeText(context, "Device online - syncing data", Toast.LENGTH_SHORT).show()
    }
}

override fun onStart() {
    super.onStart()
    connectivityReceiver.register(requireContext())
}

override fun onStop() {
    super.onStop()
    connectivityReceiver.unregister(requireContext())
}
```

### 2. Saving Data Locally

**For Truck Goods:**
```kotlin
private suspend fun saveOffline() {
    val db = OfflineDatabase.getInstance(requireContext())
    val truckGood = TruckGoodsEntity(
        shipmentId = currentShipmentId,
        name = goodsName,
        goodsNumber = goodsNumberString
    )
    db.truckGoodsDao().insert(truckGood)
    Toast.makeText(context, "Saved offline - will sync when online", Toast.LENGTH_SHORT).show()
}
```

**For Store Goods:**
```kotlin
private suspend fun saveOffline() {
    val db = OfflineDatabase.getInstance(requireContext())
    val storeGood = StoreGoodsEntity(
        shipmentId = shipmentId,
        name = goodsName,
        storeLocation = storeLocation,
        quantity = quantity
    )
    db.storeGoodsDao().insert(storeGood)
    Toast.makeText(context, "Saved offline - will sync when online", Toast.LENGTH_SHORT).show()
}
```

**For Loading Lists:**
```kotlin
private suspend fun saveOffline() {
    val db = OfflineDatabase.getInstance(requireContext())
    val loadingList = LoadingListEntity(
        name = name,
        origin = origin,
        destination = destination,
        extraDetails = extraDetails,
        status = "New"
    )
    db.loadingListDao().insert(loadingList)
    Toast.makeText(context, "Saved offline - will sync when online", Toast.LENGTH_SHORT).show()
}
```

## Data Flow

1. **User enters data offline**
   - App saves to local Room database
   - `isSynced` flag set to false
   - User sees "offline" notification

2. **Device comes online**
   - ConnectivityReceiver detects network
   - Triggers SyncManager.syncAllData()
   - SyncManager queries unsynced data from Room

3. **Syncing to Firestore**
   - SyncManager uploads each unsynced record
   - Updates Firestore collections:
     - `shipments/{shipmentId}/truck_inventory`
     - `shipments/{shipmentId}/store_inventory`
     - `loading_lists`
   - Marks records as synced in local database

4. **Sync complete**
   - User can see confirmation in logs
   - Data persists in both local and remote databases

## Integration Steps

1. **Add permissions to AndroidManifest.xml:**
```xml
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

2. **Update your entry activities** (enter_truck_goods, enter_store_goods, LoadingFragment):
   - Add network availability check
   - If offline, call saveOffline() instead of Firestore
   - If online, save to Firestore (optionally also local for backup)

3. **Register ConnectivityReceiver** in activities/fragments that perform offline data entry

4. **Test offline mode:**
   - Disable airplane mode or disconnect network
   - Enter data
   - Reconnect to network
   - Verify data syncs to Firestore

## Database Schema

### truck_goods_offline
- id (INT, Primary Key, Auto-increment)
- shipmentId (TEXT)
- name (TEXT)
- goodsNumber (TEXT)
- isSynced (BOOLEAN, default: false)
- createdAt (LONG, default: System.currentTimeMillis())

### store_goods_offline
- id (INT, Primary Key, Auto-increment)
- shipmentId (TEXT)
- name (TEXT)
- storeLocation (TEXT)
- quantity (INT)
- isSynced (BOOLEAN, default: false)
- createdAt (LONG, default: System.currentTimeMillis())

### loading_lists_offline
- id (INT, Primary Key, Auto-increment)
- name (TEXT)
- origin (TEXT)
- destination (TEXT)
- extraDetails (TEXT)
- status (TEXT, default: "New")
- isSynced (BOOLEAN, default: false)
- createdAt (LONG, default: System.currentTimeMillis())

## Error Handling

- Network errors are logged and don't crash the app
- Failed syncs are retried automatically when device comes online again
- User is notified of sync status via Toast messages
- Local data is preserved until successfully synced

## Future Enhancements

- Add retry logic with exponential backoff for failed syncs
- Implement conflict resolution if data changes remotely
- Add sync progress notifications
- Implement selective sync (e.g., sync only specific shipment)
- Add background sync service
