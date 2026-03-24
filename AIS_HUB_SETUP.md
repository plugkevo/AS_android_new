# AIS Hub Live Ship Tracking Setup Guide

## Overview
This implementation adds real-time ship tracking to African Shipping using **AIS Hub API** - a free/limited-free service providing global vessel tracking data via satellite AIS.

## What Was Built

### 1. **Ships Management System**
- Admin interface to add/edit/delete ships
- Ships identified by IMO (International Maritime Organization) number
- Store ship details: name, registration number, IMO
- View current ship location and speed

### 2. **Live Location Tracking**
- Ships repository for Firestore operations
- AIS Hub API integration for fetching real-time vessel positions
- Location updates stored in Firestore
- Location history tracked for each ship

### 3. **Enhanced Tracking Fragment**
- Search shipments by name
- View assigned ship details
- Display live ship location on Google Maps
- "Refresh Live Location" button to fetch latest AIS data
- Integration with existing checkpoints system

## Setup Instructions

### Step 1: Get AIS Hub API Key

1. Visit: https://www.aishub.net/
2. Sign up for a free account
3. Get your API key from the dashboard
4. Note: Free tier has generous rate limits (suitable for 24-hour updates)

### Step 2: Add API Key to Project

#### Option A: Using local.properties (Development)
```properties
AIS_HUB_API_KEY=your_api_key_here
```

#### Option B: Using Build Config (Production)
The API key is already configured in `build.gradle.kts`:
```kotlin
buildConfigField("String", "AIS_HUB_API_KEY", "\"${project.findProperty("AIS_HUB_API_KEY") ?: \"\"}\"")
```

### Step 3: Update AndroidManifest.xml
Add permissions if not already present:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

### Step 4: Ensure Google Play Services is Configured
- Google Maps API key should be in `AndroidManifest.xml`
- Maps dependency already in `build.gradle.kts`

## Database Schema

### Ships Collection
```
ships/
├── {shipId}
│   ├── name: String
│   ├── number: String (registration/call sign)
│   ├── imoNumber: String (for AIS Hub lookup)
│   ├── currentLatitude: Double
│   ├── currentLongitude: Double
│   ├── speed: Double (in knots)
│   ├── course: Double (heading in degrees)
│   ├── status: String (Active, Inactive)
│   ├── lastLocationUpdate: Timestamp
│   ├── createdAt: Timestamp
│   ├── updatedAt: Timestamp
│   └── location_history/ (subcollection)
│       └── {snapshotId}
│           ├── latitude: Double
│           ├── longitude: Double
│           ├── speed: Double
│           ├── course: Double
│           └── timestamp: Timestamp
```

### Updated Shipments Collection
```
shipments/
├── {shipmentId}
│   ├── name: String
│   ├── origin: String
│   ├── destination: String
│   ├── status: String
│   ├── assignedShipId: String (reference to ships collection)
│   └── ... (other existing fields)
```

## How It Works

### 1. Admin Workflow
- Navigate to Ships Management (new feature)
- Click FAB to add new ship
- Enter: Ship Name, Ship Number, IMO Number
- Click "Refresh" to fetch live AIS data
- AIS Hub API called → Location stored in Firestore

### 2. User Workflow (Tracking Shipments)
1. Search/select shipment by name
2. Tracking Fragment shows shipment details
3. If ship is assigned, shows:
   - Ship name and details
   - Current live location on Google Maps
   - Speed and course
   - Last update timestamp
4. Click "Refresh Live Location" to fetch latest data
5. Location updates automatically on map

### 3. Update Frequency
- Default: Every 24 hours via manual refresh
- Can be automated with Firebase Cloud Functions
- AIS data updated globally by ship transmitters every few minutes
- Free tier: Sufficient for typical shipping operations

## Files Created/Modified

### New Files
```
ais/
├── AisModels.kt (data classes for API responses)
├── AisHubRepository.kt (API integration)
├── ShipsRepository.kt (Firestore operations)
├── ShipsManagementFragment.kt (UI for management)
├── ShipsAdapter.kt (RecyclerView adapter)

res/layout/
├── fragment_ships_management.xml
├── item_ship.xml
├── dialog_add_ship.xml
```

### Modified Files
```
shipments/
├── TrackingFragment.kt (enhanced with live tracking + maps)

res/layout/
├── fragment_tracking.xml (added maps + refresh button)

build.gradle.kts (added API key, lifecycle dependencies)
```

## API Response Example (AIS Hub)

```json
{
  "result": [
    {
      "MMSI": "311002000",
      "IMO": "9632678",
      "Name": "PACIFIC NAVIGATOR",
      "Callsign": "SCOW",
      "Latitude": 25.2658,
      "Longitude": 55.2708,
      "Speed": 12.5,
      "Course": 180.0,
      "ShipType": "Container Ship",
      "LastUpdate": "2024-03-24T10:30:00Z"
    }
  ]
}
```

## Important Notes

### Limitations
- Free AIS Hub: API may be HTTP (not HTTPS)
- AIS data only available for commercial vessels
- Some vessels operate without AIS (military, small boats)
- Coverage better in international waters vs coastal
- Rate limited on free tier (sufficient for 24-hour updates)

### Best Practices
1. Always validate IMO numbers before adding
2. Handle API failures gracefully (vessel offline/not found)
3. Cache locations in Firestore to reduce API calls
4. Only call AIS Hub API every 24 hours per requirement
5. Show user-friendly messages when vessels unavailable

### Security
- API key stored in BuildConfig (not in code)
- Use HTTPS endpoint if available
- Never expose API key in client code
- Consider backend proxy for production

## Testing

### Test a Ship
1. Find real vessel IMO: https://www.imoschecker.com/
2. Add ship with that IMO number
3. Click "Refresh" button
4. Should see live location appear on map

### Example IMO Numbers
- 9632678 - PACIFIC NAVIGATOR
- 9400278 - MAERSK SEATRADE
- 9506867 - EVER GIVEN

## Future Enhancements

1. **Automated Updates**: Cloud Functions to update locations every 24 hours
2. **Predictive Routes**: Use vessel speed/course to predict arrival
3. **Geofencing**: Alerts when ship approaches waypoints
4. **Historical Tracking**: Show path taken over time
5. **Multiple Shipments**: Show all shipments on single ship
6. **Performance Metrics**: Calculate speed, ETA, fuel efficiency
7. **Integration with other APIs**: Wind, weather, ports data

## Troubleshooting

### "Vessel not found" error
- Check IMO number is correct
- Vessel may be offline or in territorial waters
- Try again later when vessel has AIS signal

### "API connection failed"
- Check internet connectivity
- Verify AIS Hub is not down
- Check API key is valid
- May be rate-limited (wait a few minutes)

### Location not updating
- Verify ship has been assigned to shipment
- Check Firestore has write permissions
- Verify Google Maps API key is configured
- Check logcat for detailed error messages

## Support & Documentation

- AIS Hub: https://www.aishub.net/
- IMO Checker: https://www.imoschecker.com/
- Google Maps Docs: https://developers.google.com/maps
- Firebase Firestore: https://firebase.google.com/docs/firestore
