package com.kevann.africanshipping25.ais

import android.util.Log
import com.google.gson.Gson
import com.kevann.africanshipping25.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLEncoder

class AisHubRepository {

    companion object {
        private const val BASE_URL = "http://api.aishub.net/v1/search"
        private val API_KEY = BuildConfig.AIS_HUB_API_KEY
        private const val TAG = "AisHubRepository"
    }

    /**
     * Fetch vessel location from AIS Hub API using IMO number
     * AIS Hub provides near real-time AIS data for vessels worldwide
     */
    suspend fun getVesselLocationByIMO(imoNumber: String): VesselLocation? = withContext(Dispatchers.IO) {
        try {
            // Build query URL for AIS Hub API
            val encodedIMO = URLEncoder.encode(imoNumber, "UTF-8")
            val urlString = "$BASE_URL?imo=$encodedIMO&output=json&key=$API_KEY"
            
            Log.d(TAG, "Fetching vessel data from AIS Hub for IMO: $imoNumber")
            
            val url = URL(urlString)
            val connection = url.openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            Log.d(TAG, "AIS Hub API Response: $response")
            
            // Parse JSON response
            val gson = Gson()
            val aisResponse = gson.fromJson(response, AisHubResponse::class.java)
            
            // Return first result if available
            return@withContext if (!aisResponse.result.isNullOrEmpty()) {
                aisResponse.result[0].also {
                    Log.d(TAG, "Vessel found: ${it.Name} at Lat: ${it.Latitude}, Lng: ${it.Longitude}")
                }
            } else {
                Log.w(TAG, "No vessels found for IMO: $imoNumber")
                null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching vessel location: ${e.message}", e)
            null
        }
    }

    /**
     * Fetch vessel location using MMSI (Maritime Mobile Service Identity)
     * Alternative method if IMO is not available
     */
    suspend fun getVesselLocationByMMSI(mmsi: String): VesselLocation? = withContext(Dispatchers.IO) {
        try {
            val encodedMMSI = URLEncoder.encode(mmsi, "UTF-8")
            val urlString = "$BASE_URL?mmsi=$encodedMMSI&output=json&key=$API_KEY"
            
            Log.d(TAG, "Fetching vessel data from AIS Hub for MMSI: $mmsi")
            
            val url = URL(urlString)
            val connection = url.openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            
            val gson = Gson()
            val aisResponse = gson.fromJson(response, AisHubResponse::class.java)
            
            return@withContext if (!aisResponse.result.isNullOrEmpty()) {
                aisResponse.result[0]
            } else {
                null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching vessel by MMSI: ${e.message}", e)
            null
        }
    }
}
