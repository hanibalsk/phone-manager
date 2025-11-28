# Story E7.2: Notification Weather Display

**Story ID**: E7.2
**Epic**: 7 - Weather Forecast Integration
**Priority**: Must-Have
**Estimate**: 2 story points (1 day)
**Status**: Draft
**Created**: 2025-11-28
**PRD Reference**: Enhancement (Post-MVP)

---

## Story

As a user,
I want to see current weather in my tracking notification,
so that the notification provides useful information instead of generic text.

## Acceptance Criteria

### AC E7.2.1: Notification Title with Weather
**Given** weather data is available
**When** the notification is displayed
**Then** the title should show: "{icon} {temp}°C"
  - Example: "☀️ 24°C"
  - Temperature rounded to nearest integer
  - Weather icon emoji based on weather code

### AC E7.2.2: Notification Text with Condition
**Given** weather data is available
**When** the notification is displayed
**Then** the text should show weather condition
  - Example: "Partly Cloudy"
  - Use localized string resources

### AC E7.2.3: Low-Importance Channel
**Given** the notification channel is configured
**Then** it should use:
  - IMPORTANCE_MIN (minimal prominence)
  - No sound
  - No vibration
  - No badge
  - lockscreenVisibility = VISIBILITY_SECRET

### AC E7.2.4: Notification Tap Action
**Given** the user taps the notification
**When** the app opens
**Then** it should navigate to WeatherScreen
  - Use PendingIntent with deep link
  - FLAG_IMMUTABLE for security

### AC E7.2.5: Weather Update
**Given** weather cache is refreshed
**When** new data is available
**Then** the notification should update immediately
  - Call notificationManager.notify() with new content
  - No user action required

### AC E7.2.6: Respect Settings Toggle
**Given** "Show weather in notification" is disabled
**When** the notification is displayed
**Then** it should show original text:
  - Title: "Location Tracking Active"
  - Text: "{count} locations • Interval: {n} min"

### AC E7.2.7: Offline/Error Fallback
**Given** weather data is unavailable
**When** the notification is displayed
**Then** it should:
  - Show cached weather with "Updated X ago" if cache exists
  - Fall back to original notification if no cache

## Tasks / Subtasks

- [ ] Task 1: Inject WeatherRepository (AC: E7.2.1, E7.2.2)
  - [ ] Add WeatherRepository to LocationTrackingService
  - [ ] Fetch weather in service scope

- [ ] Task 2: Modify createNotification() (AC: E7.2.1, E7.2.2, E7.2.6)
  - [ ] Check showWeatherInNotification preference
  - [ ] Get cached weather from repository
  - [ ] Build notification title with weather
  - [ ] Build notification text with condition

- [ ] Task 3: Weather Icon Mapping (AC: E7.2.1)
  - [ ] Create weatherCodeToEmoji() function
  - [ ] Map WMO codes to emoji icons
  - [ ] Handle unknown codes gracefully

- [ ] Task 4: Configure Notification Channel (AC: E7.2.3)
  - [ ] Create/update weather notification channel
  - [ ] Set IMPORTANCE_MIN
  - [ ] Disable sound, vibration, badge
  - [ ] Set VISIBILITY_SECRET

- [ ] Task 5: Weather Screen Deep Link (AC: E7.2.4)
  - [ ] Create PendingIntent for WeatherScreen
  - [ ] Update contentIntent in notification
  - [ ] Handle navigation in MainActivity

- [ ] Task 6: Observe Weather Updates (AC: E7.2.5)
  - [ ] Observe weather cache changes
  - [ ] Call updateNotification() on change
  - [ ] Debounce rapid updates

- [ ] Task 7: Fallback Logic (AC: E7.2.7)
  - [ ] Check if weather is stale (>30 min)
  - [ ] Show "Updated X ago" for stale data
  - [ ] Fall back to original notification if no data

- [ ] Task 8: Testing (All ACs)
  - [ ] Manual test notification display
  - [ ] Test notification tap navigation
  - [ ] Test settings toggle behavior

## Dev Notes

### Weather Emoji Mapping

```kotlin
fun weatherCodeToEmoji(code: Int): String = when (code) {
    0 -> "☀️"           // Clear sky
    1, 2, 3 -> "🌤️"    // Partly cloudy
    45, 48 -> "🌫️"     // Fog
    51, 53, 55 -> "🌧️" // Drizzle
    61, 63, 65 -> "🌧️" // Rain
    71, 73, 75, 77 -> "🌨️" // Snow
    95, 96, 99 -> "⛈️" // Thunderstorm
    else -> "🌡️"       // Unknown
}
```

### Notification Builder Modifications

```kotlin
// In LocationTrackingService.createNotification()
val isWeatherEnabled = preferencesRepository.showWeatherInNotification.first()
val weather = weatherRepository.getCachedWeather()

return if (isWeatherEnabled && weather != null) {
    NotificationCompat.Builder(this, CHANNEL_ID_WEATHER)
        .setContentTitle("${weatherCodeToEmoji(weather.weatherCode)} ${weather.temperature}°C")
        .setContentText(weather.condition)
        .setSmallIcon(R.drawable.ic_weather)
        .setOngoing(true)
        .setSilent(true)
        .setPriority(NotificationCompat.PRIORITY_MIN)
        .setContentIntent(weatherScreenPendingIntent)
        .build()
} else {
    // Original notification
    ...
}
```

### Files to Modify

- `service/LocationTrackingService.kt` (MODIFY)
- `di/ServiceModule.kt` (MODIFY - inject WeatherRepository)

### Files to Create

- `util/WeatherUtils.kt` (NEW - emoji mapping, formatting)

### Dependencies

- Story E7.1 (WeatherRepository)
- Story E7.4 (Settings toggle - can be done in parallel)

### References

- [Source: Epic-E7-Weather-Forecast.md - Story E7.2]
- [Source: LocationTrackingService.kt lines 373-429]

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-11-28 | John (PM) | Story created from Epic E7 feature spec |

---

**Last Updated**: 2025-11-28
**Status**: Draft
**Dependencies**: Story E7.1
