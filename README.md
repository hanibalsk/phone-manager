# Phone Manager

A native Android application for real-time location tracking, group device management, geofencing, and webhook automation.

## Overview

Phone Manager is a comprehensive location tracking solution built with modern Android architecture. It enables device registration, group-based location sharing, proximity alerts, geofencing capabilities, and webhook integrations for home automation systems.

### Key Features

- **Location Tracking**: Real-time GPS tracking with configurable intervals
- **Device Groups**: Register devices and view group members' locations
- **Real-Time Map**: Google Maps integration with live location updates and polling
- **Location History**: Historical location data with date filtering
- **Movement Tracking**: Intelligent trip detection with transportation mode recognition
- **Trip History**: View and manage recorded trips with filtering and day grouping
- **Proximity Alerts**: Notifications when devices enter/exit defined ranges
- **Geofencing**: Create geographic boundaries with event triggers
- **Webhook Integration**: Automate actions via Home Assistant or n8n
- **Secret Mode**: Discreet operation with hidden notifications
- **Health Monitoring**: Service watchdog with automatic recovery

## Technology Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin 1.9+ |
| UI Framework | Jetpack Compose + Material Design 3 |
| Architecture | MVVM + Clean Architecture |
| Dependency Injection | Hilt |
| Database | Room (SQLite) |
| HTTP Client | Ktor |
| Background Work | WorkManager |
| Maps | Google Maps Compose |
| Location | Google Play Services Location |
| Security | Android Keystore + EncryptedSharedPreferences |

## Architecture

```
┌─────────────────────────────────────────┐
│           UI Layer (Compose)            │
│  Screens • ViewModels • Navigation      │
├─────────────────────────────────────────┤
│          Domain Layer                   │
│  Models • Repository Interfaces         │
├─────────────────────────────────────────┤
│           Data Layer                    │
│  Room DB • API Services • DataStore     │
└─────────────────────────────────────────┘
```

See [Architecture Documentation](docs/ARCHITECTURE.md) for detailed information.

## Quick Start

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17+
- Android SDK 26+ (Android 8.0)
- Google Maps API key

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/your-org/phone-manager.git
   cd phone-manager
   ```

2. Create `local.properties` with your API keys:
   ```properties
   MAPS_API_KEY=your_google_maps_api_key
   API_BASE_URL=https://your-api-server.com
   API_KEY=your_api_key
   ```

3. Build and run:
   ```bash
   ./gradlew assembleDebug
   ```

See [Developer Guide](docs/DEVELOPER_GUIDE.md) for detailed setup instructions.

## Project Structure

```
app/src/main/java/three/two/bit/phonemanager/
├── data/           # Data layer (Room, repositories)
├── di/             # Hilt dependency injection modules
├── domain/         # Domain models
├── geofence/       # Geofencing implementation
├── location/       # Location capture logic
├── movement/       # Transportation mode detection
├── network/        # API services (Ktor)
├── permission/     # Permission management
├── queue/          # Background work queue
├── security/       # Encrypted storage
├── service/        # Foreground location service
├── trip/           # Trip detection and management
├── ui/             # Compose screens & ViewModels
├── util/           # Utility classes
└── watchdog/       # Service health monitoring
```

## Documentation

| Document | Description |
|----------|-------------|
| [Architecture](docs/ARCHITECTURE.md) | System design and patterns |
| [API Reference](docs/API_REFERENCE.md) | Backend API documentation |
| [Features](docs/FEATURES.md) | Feature descriptions and usage |
| [Data Models](docs/DATA_MODELS.md) | Domain and entity models |
| [Developer Guide](docs/DEVELOPER_GUIDE.md) | Setup and contribution guide |
| [PRD](docs/PRD.md) | Product requirements |
| [Epics](docs/epics.md) | Development roadmap |

## Features Status

| Epic | Feature | Status |
|------|---------|--------|
| 0 | Foundation Infrastructure | ✅ Complete |
| 1 | Device Registration & Groups | ✅ Complete |
| 2 | Secret Mode | ⏳ Planned |
| 3 | Real-Time Map & Group Display | ✅ Complete |
| 4 | Location History | ✅ Complete |
| 5 | Proximity Alerts | ✅ Complete |
| 6 | Geofencing & Webhooks | ✅ Complete |
| 7 | Health Monitoring & Reliability | ✅ Complete |
| 8 | Movement Tracking & Trip Detection | ✅ Complete |

## Database Schema

Current version: **8**

| Table | Purpose |
|-------|---------|
| locations | GPS location records |
| location_queue | Upload queue |
| proximity_alerts | Alert configurations |
| geofences | Geofence definitions |
| geofence_events | Geofence trigger events |
| webhooks | Webhook configurations |
| trips | Trip records with mode and statistics |
| movement_events | Transportation mode change events |

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/devices/register` | POST | Register device |
| `/api/devices` | GET | List group members |
| `/api/v1/locations` | POST | Upload location |
| `/api/v1/locations/batch` | POST | Batch upload |
| `/api/geofences` | CRUD | Geofence management |
| `/api/webhooks` | CRUD | Webhook management |
| `/api/proximity-alerts` | CRUD | Alert management |

See [API Reference](docs/API_REFERENCE.md) for complete documentation.

## Testing

**514 unit tests** covering ViewModels, repositories, and business logic.

Run unit tests:
```bash
./gradlew test
```

Run instrumented tests:
```bash
./gradlew connectedAndroidTest
```

Check code style:
```bash
./gradlew spotlessCheck
```

Format code:
```bash
./gradlew spotlessApply
```

## Security

- Device credentials stored in EncryptedSharedPreferences
- API key secured with Android Keystore
- HTTPS-only communication
- HMAC-SHA256 webhook signatures
- Runtime permission management

## Requirements

- **Minimum SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 36 (Android 15)
- **Permissions Required**:
  - `ACCESS_FINE_LOCATION`
  - `ACCESS_COARSE_LOCATION`
  - `ACCESS_BACKGROUND_LOCATION`
  - `ACTIVITY_RECOGNITION` (for movement tracking)
  - `FOREGROUND_SERVICE`
  - `POST_NOTIFICATIONS`
  - `INTERNET`

## Related Projects

- **Backend Server**: [phone-manager-backend](https://github.com/hanibalsk/phone-manager-backend) - The companion backend server for Phone Manager

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

See [Developer Guide](docs/DEVELOPER_GUIDE.md) for contribution guidelines.
