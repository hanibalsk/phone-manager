# Story E7.1: Weather API Integration & Caching

**Story ID**: E7.1
**Epic**: 7 - Weather Forecast Integration
**Priority**: Must-Have
**Estimate**: 2 story points (1 day)
**Status**: Ready for Review
**Created**: 2025-11-28
**PRD Reference**: Enhancement (Post-MVP)

---

## Story

As a developer,
I want to integrate the Open-Meteo weather API,
so that I can retrieve weather data using the device's location.

## Acceptance Criteria

### AC E7.1.1: Weather API Service
**Given** the network layer is defined
**When** I need to fetch weather data
**Then** WeatherApiService should:
  - Use Ktor HttpClient (reuse existing infrastructure)
  - Call Open-Meteo API: `api.open-meteo.com/v1/forecast`
  - Request parameters: latitude, longitude, current weather, daily forecast
  - No API key required (free tier)

### AC E7.1.2: Weather Domain Model
**Given** the data model is defined
**Then** Weather domain model should include:
  - Current conditions: temperature, feelsLike, humidity, windSpeed, weatherCode
  - Daily forecasts: List of (date, tempMin, tempMax, weatherCode)
  - Metadata: lastUpdated timestamp, locationCoordinates

### AC E7.1.3: Weather Repository
**Given** I need to access weather data
**When** I call WeatherRepository
**Then** it should:
  - Check cache first (DataStore-based)
  - Fetch from API if cache expired (30-minute TTL)
  - Return cached data if offline
  - Not block on failures (graceful degradation)

### AC E7.1.4: Weather Cache
**Given** weather data is fetched
**When** it is stored
**Then** the cache should:
  - Use DataStore for persistence
  - Store serialized Weather object
  - Include timestamp for TTL checking
  - Survive app restarts
  - 30-minute cache TTL

### AC E7.1.5: Hilt DI Integration
**Given** the DI container is configured
**Then** WeatherModule should provide:
  - WeatherApiService (singleton)
  - WeatherRepository (singleton)
  - WeatherCache (singleton)

### AC E7.1.6: Error Handling
**Given** the weather API call fails
**Then** the system should:
  - Return cached data if available
  - Return null/empty if no cache
  - Log error but not crash
  - Not affect location tracking service

## Tasks / Subtasks

- [x] Task 1: Create Weather Network Models (AC: E7.1.1)
  - [x] Create OpenMeteoResponse DTO matching API response
  - [x] Create CurrentWeather DTO
  - [x] Create DailyForecast DTO
  - [x] Add kotlinx.serialization annotations

- [x] Task 2: Create Weather Domain Model (AC: E7.1.2)
  - [x] Create Weather data class
  - [x] Create CurrentConditions data class
  - [x] Create DailyForecast domain model
  - [x] Create WeatherCode enum mapping WMO codes

- [x] Task 3: Create WeatherApiService (AC: E7.1.1)
  - [x] Create WeatherApiService interface
  - [x] Create WeatherApiServiceImpl with Ktor
  - [x] Implement getWeather(lat, lon) method
  - [x] Map API response to domain model

- [x] Task 4: Create WeatherCache (AC: E7.1.4)
  - [x] Create WeatherCache interface
  - [x] Create WeatherCacheImpl using DataStore
  - [x] Implement save/get/clear methods
  - [x] Add TTL checking logic (30 minutes)

- [x] Task 5: Create WeatherRepository (AC: E7.1.3, E7.1.6)
  - [x] Create WeatherRepository interface
  - [x] Create WeatherRepositoryImpl
  - [x] Implement cache-first strategy
  - [x] Add graceful error handling

- [x] Task 6: Create WeatherModule (AC: E7.1.5)
  - [x] Create WeatherModule Hilt module
  - [x] Provide WeatherApiService binding
  - [x] Provide WeatherRepository binding
  - [x] Provide WeatherCache binding

- [x] Task 7: Testing (All ACs)
  - [x] Unit test WeatherRepository caching logic
  - [x] Unit test TTL expiration
  - [x] Unit test error handling

## Dev Notes

### Open-Meteo API

**Endpoint:** `https://api.open-meteo.com/v1/forecast`

**Request Parameters:**
```
?latitude={lat}
&longitude={lon}
&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,apparent_temperature
&daily=temperature_2m_max,temperature_2m_min,weather_code
&timezone=auto
&forecast_days=5
```

**Sample Response:**
```json
{
  "current": {
    "temperature_2m": 24.5,
    "relative_humidity_2m": 45,
    "weather_code": 1,
    "wind_speed_10m": 12.3,
    "apparent_temperature": 26.1
  },
  "daily": {
    "time": ["2025-11-28", "2025-11-29", ...],
    "temperature_2m_max": [26, 24, ...],
    "temperature_2m_min": [18, 17, ...],
    "weather_code": [1, 2, ...]
  }
}
```

### WMO Weather Codes

| Code | Condition |
|------|-----------|
| 0 | Clear sky |
| 1-3 | Partly cloudy |
| 45-48 | Fog |
| 51-55 | Drizzle |
| 61-65 | Rain |
| 71-77 | Snow |
| 95-99 | Thunderstorm |

### Files to Create

- `network/WeatherApiService.kt` (NEW)
- `network/models/WeatherModels.kt` (NEW)
- `domain/model/Weather.kt` (NEW)
- `data/repository/WeatherRepository.kt` (NEW)
- `data/cache/WeatherCache.kt` (NEW)
- `di/WeatherModule.kt` (NEW)

### Dependencies

- Existing Ktor HttpClient
- Existing DataStore infrastructure
- kotlinx.serialization (already included)

### References

- [Source: Epic-E7-Weather-Forecast.md - Story E7.1]
- [Source: Open-Meteo API Documentation](https://open-meteo.com/en/docs)

---

## Dev Agent Record

### Debug Log

**Implementation Complete:**
- Created complete weather API integration layer with Open-Meteo
- Implemented cache-first strategy with 30-minute TTL
- Added comprehensive error handling and graceful degradation
- Created full WMO weather code enum (27 conditions with emojis)
- Followed existing architectural patterns (Ktor, DataStore, Hilt)

**Technical Decisions:**
1. Used kotlinx.serialization for JSON and cache serialization
2. Separate DataStore for weather cache (isolated from preferences)
3. Cache returns expired data if offline (AC E7.1.6)
4. Domain models made Serializable for cache persistence

### Completion Notes

All 7 tasks completed successfully. Foundation infrastructure ready for weather display in notifications (Story E7.2).

**Test Results:**
- Build: ✅ SUCCESS
- Unit Tests: ✅ 6/6 passed (cache, TTL, error handling)
- Code Formatting: ✅ Applied and verified

---

## File List

### Created Files
- `app/src/main/java/three/two/bit/phonemanager/network/WeatherApiService.kt`
- `app/src/main/java/three/two/bit/phonemanager/network/models/WeatherModels.kt`
- `app/src/main/java/three/two/bit/phonemanager/domain/model/Weather.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/repository/WeatherRepository.kt`
- `app/src/main/java/three/two/bit/phonemanager/data/cache/WeatherCache.kt`
- `app/src/main/java/three/two/bit/phonemanager/di/WeatherModule.kt`
- `app/src/test/java/three/two/bit/phonemanager/data/repository/WeatherRepositoryTest.kt`

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-11-28 | John (PM) | Story created from Epic E7 feature spec |
| 2025-11-28 | Dev Agent | Implemented all 7 tasks: API service, domain models, cache, repository, DI, tests |

---

**Last Updated**: 2025-11-28
**Status**: Ready for Review
**Dependencies**: None (foundation story complete)
