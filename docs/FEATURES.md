# Phone Manager Features Guide

## Overview

Phone Manager provides comprehensive location tracking and automation capabilities for Android devices. This guide covers all implemented features and their usage.

---

## 1. Device Registration

### Description
Register your device with the Phone Manager server and join a device group for location sharing.

### How to Use

1. **Launch the app** - The registration screen appears on first launch
2. **Enter display name** - A friendly name for your device (2-50 characters)
3. **Enter group ID** - The group you want to join (e.g., "family", "work-team")
4. **Tap Register** - Device credentials are securely stored

### Features
- Display name validation (2-50 characters, alphanumeric with spaces)
- Group ID validation
- Secure device ID generation (UUID)
- Encrypted credential storage

### Technical Details
- **Screen**: `RegistrationScreen.kt`
- **ViewModel**: `RegistrationViewModel.kt`
- **API**: `POST /api/devices/register`
- **Storage**: `SecureStorage` (EncryptedSharedPreferences)

---

## 2. Location Tracking

### Description
Continuous GPS location tracking with server synchronization.

### How to Use

1. **Grant permissions** - Location and background location access required
2. **Enable tracking** - Toggle on the Home screen
3. **View status** - Service status card shows tracking state

### Features
- Foreground service with persistent notification
- Configurable tracking intervals
- Battery-optimized using FusedLocationProvider
- Automatic retry on network failures
- Local storage with upload queue

### Permissions Required
- `ACCESS_FINE_LOCATION` - GPS access
- `ACCESS_COARSE_LOCATION` - Network location
- `ACCESS_BACKGROUND_LOCATION` - Background tracking
- `FOREGROUND_SERVICE` - Service notification

### Technical Details
- **Service**: `LocationTrackingService.kt`
- **Manager**: `LocationManager.kt`
- **Storage**: `LocationEntity` (Room)
- **Queue**: `LocationQueueEntity`

---

## 3. Group Members

### Description
View all devices in your group with their last known locations.

### How to Use

1. **Navigate** - Tap "View Group Members" on Home screen
2. **View list** - See all devices with names and last locations
3. **Pull to refresh** - Update member locations

### Features
- Device list with display names
- Last location with timestamp
- Distance from your location
- Pull-to-refresh for updates
- Error handling with retry

### Technical Details
- **Screen**: `GroupMembersScreen.kt`
- **ViewModel**: `GroupMembersViewModel.kt`
- **API**: `GET /api/devices?groupId={id}`
- **Model**: `Device` domain model

---

## 4. Settings

### Description
Configure your device settings and update registration details.

### How to Use

1. **Navigate** - Tap settings icon on Home screen
2. **View device ID** - Read-only unique identifier
3. **Update display name** - Change your device's friendly name
4. **Update group ID** - Switch to a different group (with confirmation)

### Features
- Display name editing with validation
- Group ID editing with confirmation dialog
- Input validation (2-50 characters)
- Real-time error feedback
- Success notifications

### Technical Details
- **Screen**: `SettingsScreen.kt`
- **ViewModel**: `SettingsViewModel.kt`
- **Validation**: Character limits, alphanumeric rules

---

## 5. Real-Time Map

### Description
View your current location on an interactive Google Map.

### How to Use

1. **Navigate** - Tap "View Map" on Home screen
2. **View location** - Blue marker shows your position
3. **Interact** - Pan, zoom, and rotate the map

### Features
- Google Maps integration
- Current location marker (blue dot)
- Standard map interactions
- Loading and error states
- Auto-center on location

### Permissions Required
- Location permission for "my location" feature

### Technical Details
- **Screen**: `MapScreen.kt`
- **ViewModel**: `MapViewModel.kt`
- **Library**: Google Maps Compose

---

## 6. Location History

### Description
View historical location data with date filtering and visualization.

### How to Use

1. **Navigate** - Tap "View Location History" on Home screen
2. **Select date range** - Today, Yesterday, Last 7 Days, or Custom
3. **View path** - Polyline shows movement over time
4. **Interact** - Tap points for details

### Features
- Date/time filtering
- Polyline visualization on map
- Automatic downsampling for performance
- Pull-to-refresh
- Local and server data support

### Date Range Options
- **Today**: Current day's locations
- **Yesterday**: Previous day's locations
- **Last 7 Days**: Week's worth of data
- **Custom**: Select specific date range

### Technical Details
- **Screen**: `HistoryScreen.kt`
- **ViewModel**: `HistoryViewModel.kt`
- **Downsampling**: 200-500 points target
- **Storage**: `LocationEntity` with `isSynced` flag

---

## 7. Proximity Alerts

### Description
Get notified when devices enter or exit a defined proximity range.

### How to Use

1. **Navigate** - Tap "Proximity Alerts" on Home screen
2. **Create alert** - Tap + to add new alert
3. **Configure**:
   - Select target device
   - Set radius (50m - 10km)
   - Choose direction (Enter, Exit, or Both)
4. **Enable/Disable** - Toggle alert active state

### Features
- Device-to-device proximity monitoring
- Configurable radius (50m - 10,000m)
- Direction-based triggers (ENTER, EXIT, BOTH)
- Local notification alerts
- State tracking (INSIDE/OUTSIDE)

### Alert Directions
- **ENTER**: Trigger when target comes within range
- **EXIT**: Trigger when target leaves range
- **BOTH**: Trigger on both enter and exit

### Technical Details
- **Screen**: `AlertsScreen.kt`, `CreateAlertScreen.kt`
- **ViewModel**: `AlertsViewModel.kt`
- **Model**: `ProximityAlert` domain model
- **Calculator**: `ProximityCalculator.kt`
- **Storage**: `ProximityAlertEntity`

---

## 8. Geofences

### Description
Define geographic boundaries that trigger events when crossed.

### How to Use

1. **Navigate** - Tap "Geofences" on Home screen
2. **Create geofence** - Tap + to add new geofence
3. **Configure**:
   - Name your geofence
   - Enter coordinates (latitude/longitude)
   - Set radius (50m - 10km)
   - Select trigger events (Enter, Exit, Dwell)
   - Optionally link a webhook
4. **Manage** - Toggle active state, swipe to delete

### Features
- Named geographic boundaries
- Configurable radius with logarithmic slider
- Multiple transition types
- Webhook integration for automation
- Active/inactive toggle
- Swipe-to-delete

### Transition Types
- **ENTER**: Trigger when entering the geofence
- **EXIT**: Trigger when leaving the geofence
- **DWELL**: Trigger after staying in the area

### Technical Details
- **Screen**: `GeofencesScreen.kt`, `CreateGeofenceScreen.kt`
- **ViewModel**: `GeofencesViewModel.kt`
- **Model**: `Geofence` domain model
- **Android API**: `GeofencingClient`
- **Storage**: `GeofenceEntity`

---

## 9. Webhooks

### Description
Configure webhooks to send geofence events to external services like Home Assistant or n8n.

### How to Use

1. **Navigate** - Tap "Webhooks" on Home screen
2. **Create webhook** - Tap + to add new webhook
3. **Configure**:
   - Name your webhook (e.g., "Home Assistant")
   - Enter target URL (HTTPS required)
   - Copy the auto-generated secret
4. **Manage** - Toggle enabled state, swipe to delete
5. **Link to geofence** - Select webhook when creating geofences

### Features
- HTTPS URL validation
- Auto-generated HMAC secret
- Enable/disable toggle
- Swipe-to-delete
- Link to geofences

### Webhook Payload
When a linked geofence triggers, the webhook receives:
```json
{
  "event": "geofence_trigger",
  "deviceId": "your-device-id",
  "deviceName": "Your Phone",
  "geofenceId": "geo-123",
  "geofenceName": "Home",
  "eventType": "ENTER",
  "timestamp": "2025-01-15T10:30:00Z",
  "location": {
    "latitude": 48.1486,
    "longitude": 17.1077
  }
}
```

### Security
- HMAC-SHA256 signature in `X-Signature` header
- Secret key for payload verification
- HTTPS-only URLs

### Technical Details
- **Screen**: `WebhooksScreen.kt`, `CreateWebhookScreen.kt`
- **ViewModel**: `WebhooksViewModel.kt`, `CreateWebhookViewModel.kt`
- **Model**: `Webhook` domain model
- **Storage**: `WebhookEntity`

---

## 10. Secret Mode

### Description
Discreet operation mode with hidden UI elements and generic notifications.

### How to Activate

**Method 1 - Long Press:**
- Long-press (3 seconds) on "Phone Manager" title

**Method 2 - Tap Gesture:**
- Tap version text (v1.0.0) 5 times quickly

### Features
- No visible mode indicator
- Generic notification: "Service running"
- Suppressed verbose logging
- Low-importance notifications
- No haptic feedback

### Technical Details
- **Storage**: `PreferencesRepository` (DataStore)
- **Service**: Dual notification channels
- **ViewModel**: `HomeViewModel.kt`

---

## Feature Matrix

| Feature | Status | Offline Support | Notifications |
|---------|--------|-----------------|---------------|
| Device Registration | ✅ Complete | No | - |
| Location Tracking | ✅ Complete | Yes | Yes |
| Group Members | ✅ Complete | Cached | - |
| Settings | ✅ Complete | Yes | - |
| Real-Time Map | 🔄 Partial | Cached | - |
| Location History | ✅ Complete | Yes | - |
| Proximity Alerts | ✅ Complete | Yes | Yes |
| Geofences | ✅ Complete | Yes | Yes |
| Webhooks | ✅ Complete | Synced | - |
| Secret Mode | ⏳ Planned | Yes | Modified |

---

## Permissions Summary

| Permission | Required For |
|------------|--------------|
| ACCESS_FINE_LOCATION | GPS tracking, Map |
| ACCESS_COARSE_LOCATION | Network location |
| ACCESS_BACKGROUND_LOCATION | Background tracking |
| FOREGROUND_SERVICE | Service notification |
| POST_NOTIFICATIONS | Alerts, Geofences |
| INTERNET | Server sync |
| RECEIVE_BOOT_COMPLETED | Auto-start |

---

## Troubleshooting

### Location Not Updating
1. Check location permissions are granted
2. Verify GPS is enabled on device
3. Ensure battery optimization is disabled for app
4. Check service is running (notification visible)

### Group Members Not Showing
1. Verify network connection
2. Check API server is reachable
3. Ensure correct group ID

### Geofence Not Triggering
1. Verify background location permission
2. Check geofence is active
3. Ensure radius is appropriate for location
4. Battery optimization may delay triggers

### Webhook Not Delivering
1. Verify URL is HTTPS
2. Check target server is reachable
3. Ensure webhook is enabled
4. Verify HMAC signature validation
