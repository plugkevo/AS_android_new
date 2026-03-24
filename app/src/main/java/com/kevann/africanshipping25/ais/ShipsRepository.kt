package com.kevann.africanshipping25.ais

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ShipsRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val shipsCollection = "ships"
    private val locationHistoryCollection = "location_history"
    private const val TAG = "ShipsRepository"

    /**
     * Add a new ship to Firestore
     */
    suspend fun addShip(ship: Ship): String = withContext(Dispatchers.IO) {
        try {
            val shipData = hashMapOf(
                "name" to ship.name,
                "number" to ship.number,
                "imoNumber" to ship.imoNumber,
                "currentLatitude" to ship.currentLatitude,
                "currentLongitude" to ship.currentLongitude,
                "lastLocationUpdate" to FieldValue.serverTimestamp(),
                "speed" to ship.speed,
                "course" to ship.course,
                "status" to ship.status,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val documentRef = firestore.collection(shipsCollection).add(shipData).await()
            Log.d(TAG, "Ship added with ID: ${documentRef.id}")
            documentRef.id
        } catch (e: Exception) {
            Log.e(TAG, "Error adding ship: ${e.message}", e)
            throw e
        }
    }

    /**
     * Get ship by ID
     */
    suspend fun getShipById(shipId: String): Ship? = withContext(Dispatchers.IO) {
        try {
            val document = firestore.collection(shipsCollection).document(shipId).get().await()
            if (document.exists()) {
                Ship(
                    id = document.id,
                    name = document.getString("name") ?: "",
                    number = document.getString("number") ?: "",
                    imoNumber = document.getString("imoNumber") ?: "",
                    currentLatitude = document.getDouble("currentLatitude") ?: 0.0,
                    currentLongitude = document.getDouble("currentLongitude") ?: 0.0,
                    lastLocationUpdate = document.getLong("lastLocationUpdate") ?: 0,
                    speed = document.getDouble("speed") ?: 0.0,
                    course = document.getDouble("course") ?: 0.0,
                    status = document.getString("status") ?: "Active",
                    createdAt = document.getLong("createdAt") ?: 0,
                    updatedAt = document.getLong("updatedAt") ?: 0
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting ship: ${e.message}", e)
            null
        }
    }

    /**
     * Get all ships
     */
    suspend fun getAllShips(): List<Ship> = withContext(Dispatchers.IO) {
        try {
            val documents = firestore.collection(shipsCollection).get().await()
            documents.map { document ->
                Ship(
                    id = document.id,
                    name = document.getString("name") ?: "",
                    number = document.getString("number") ?: "",
                    imoNumber = document.getString("imoNumber") ?: "",
                    currentLatitude = document.getDouble("currentLatitude") ?: 0.0,
                    currentLongitude = document.getDouble("currentLongitude") ?: 0.0,
                    lastLocationUpdate = document.getLong("lastLocationUpdate") ?: 0,
                    speed = document.getDouble("speed") ?: 0.0,
                    course = document.getDouble("course") ?: 0.0,
                    status = document.getString("status") ?: "Active",
                    createdAt = document.getLong("createdAt") ?: 0,
                    updatedAt = document.getLong("updatedAt") ?: 0
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all ships: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Update ship location with AIS Hub data
     */
    suspend fun updateShipLocation(shipId: String, vesselLocation: VesselLocation): Boolean = withContext(Dispatchers.IO) {
        try {
            val updates = hashMapOf<String, Any>(
                "currentLatitude" to (vesselLocation.Latitude ?: 0.0),
                "currentLongitude" to (vesselLocation.Longitude ?: 0.0),
                "speed" to (vesselLocation.Speed ?: 0.0),
                "course" to (vesselLocation.Course ?: 0.0),
                "lastLocationUpdate" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            firestore.collection(shipsCollection).document(shipId).update(updates).await()
            
            // Save location snapshot to history
            saveLocationSnapshot(shipId, vesselLocation)
            
            Log.d(TAG, "Ship location updated for ID: $shipId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating ship location: ${e.message}", e)
            false
        }
    }

    /**
     * Save location snapshot to history
     */
    private suspend fun saveLocationSnapshot(shipId: String, vesselLocation: VesselLocation) {
        try {
            val snapshot = hashMapOf(
                "shipId" to shipId,
                "latitude" to (vesselLocation.Latitude ?: 0.0),
                "longitude" to (vesselLocation.Longitude ?: 0.0),
                "speed" to (vesselLocation.Speed ?: 0.0),
                "course" to (vesselLocation.Course ?: 0.0),
                "timestamp" to FieldValue.serverTimestamp()
            )

            firestore.collection(shipsCollection).document(shipId)
                .collection(locationHistoryCollection).add(snapshot).await()
            
            Log.d(TAG, "Location snapshot saved for ship: $shipId")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving location snapshot: ${e.message}", e)
        }
    }

    /**
     * Delete ship
     */
    suspend fun deleteShip(shipId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection(shipsCollection).document(shipId).delete().await()
            Log.d(TAG, "Ship deleted: $shipId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting ship: ${e.message}", e)
            false
        }
    }

    /**
     * Update ship details
     */
    suspend fun updateShipDetails(shipId: String, name: String, number: String, imoNumber: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val updates = hashMapOf<String, Any>(
                "name" to name,
                "number" to number,
                "imoNumber" to imoNumber,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            firestore.collection(shipsCollection).document(shipId).update(updates).await()
            Log.d(TAG, "Ship details updated for ID: $shipId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating ship details: ${e.message}", e)
            false
        }
    }
}
