#!/bin/bash
# Test data generators for E2E tests
# Provides UUID generation, location paths, and movement event generation

# =============================================================================
# UUID Generation
# =============================================================================

# Generate UUID (cross-platform)
generate_uuid() {
    if command -v uuidgen &>/dev/null; then
        uuidgen | tr '[:upper:]' '[:lower:]'
    elif [[ -f /proc/sys/kernel/random/uuid ]]; then
        cat /proc/sys/kernel/random/uuid
    else
        python3 -c "import uuid; print(uuid.uuid4())"
    fi
}

# Generate short ID for display names
generate_short_id() {
    echo "$(date +%H%M%S)_$$"
}

# =============================================================================
# Random Number Generation
# =============================================================================

# Random float in range (uses awk for portability)
random_float() {
    local min="$1"
    local max="$2"
    local precision="${3:-6}"
    awk -v min="$min" -v max="$max" -v prec="$precision" -v seed="$RANDOM" \
        'BEGIN { srand(seed); printf "%.*f", prec, min + rand() * (max - min) }'
}

# Random integer in range
random_int() {
    local min="$1"
    local max="$2"
    echo $((RANDOM % (max - min + 1) + min))
}

# =============================================================================
# Device Generation
# =============================================================================

# Generate test device info (returns "device_id|display_name")
generate_device() {
    local device_id=$(generate_uuid)
    local display_name="E2E Device $(generate_short_id)"
    echo "$device_id|$display_name"
}

# Generate API key format
generate_api_key() {
    local key=$(openssl rand -hex 32 2>/dev/null || generate_uuid | tr -d '-')
    echo "pm_${key}"
}

# =============================================================================
# Location Generation
# =============================================================================

# Generate single location JSON
generate_location() {
    local latitude="$1"
    local longitude="$2"
    local accuracy="${3:-$(random_float 5 30 1)}"
    local timestamp="${4:-$(date +%s000)}"
    local speed="${5:-0}"
    local bearing="${6:-0}"

    cat <<EOF
{
    "timestamp": $timestamp,
    "latitude": $latitude,
    "longitude": $longitude,
    "accuracy": $accuracy,
    "speed": $speed,
    "bearing": $bearing
}
EOF
}

# Generate random location near San Francisco
generate_sf_location() {
    local lat=$(random_float 37.70 37.82)
    local lon=$(random_float -122.52 -122.35)
    generate_location "$lat" "$lon"
}

# Generate linear path between two points as JSON array
# Timestamps are generated in the PAST to avoid "future timestamp" validation errors
generate_location_path() {
    local start_lat="$1"
    local start_lon="$2"
    local end_lat="$3"
    local end_lon="$4"
    local num_points="${5:-10}"
    local interval_ms="${7:-60000}"  # 1 minute default
    # Calculate start_time so all points end up in the past
    # Default: start from (now - num_points * interval) so last point is ~now
    local default_start=$(($(date +%s) * 1000 - (num_points - 1) * interval_ms))
    local start_time="${6:-$default_start}"

    local locations="["
    for i in $(seq 0 $((num_points - 1))); do
        # Interpolation factor
        local t=$(echo "scale=6; $i / ($num_points - 1)" | bc)
        local lat=$(echo "scale=6; $start_lat + ($end_lat - $start_lat) * $t" | bc)
        local lon=$(echo "scale=6; $start_lon + ($end_lon - $start_lon) * $t" | bc)
        local accuracy=$(random_float 5 25 1)
        local timestamp=$((start_time + i * interval_ms))

        # Calculate bearing (approximate, normalized to 0-360)
        local bearing=0
        if [[ $i -gt 0 ]]; then
            # Simple bearing calculation using atan2 approximation
            local delta_lon=$(echo "scale=6; $end_lon - $start_lon" | bc)
            local delta_lat=$(echo "scale=6; $end_lat - $start_lat" | bc)
            bearing=$(echo "scale=1; a(($delta_lon) / ($delta_lat + 0.0001)) * 57.3" | bc -l 2>/dev/null || echo "0")
            bearing=${bearing:-0}
            # Normalize to 0-360 range
            if [[ $(echo "$bearing < 0" | bc -l) -eq 1 ]]; then
                bearing=$(echo "scale=1; $bearing + 360" | bc)
            fi
        fi

        # Calculate speed (m/s)
        local speed=0
        if [[ $i -gt 0 && "$interval_ms" -gt 0 ]]; then
            # Approximate distance in meters (1 degree ~ 111km)
            local dist=$(echo "scale=2; sqrt(($delta_lat * 111000)^2 + ($delta_lon * 85000)^2) / $num_points" | bc 2>/dev/null || echo "0")
            speed=$(echo "scale=1; $dist / ($interval_ms / 1000)" | bc 2>/dev/null || echo "5")
        fi

        [[ $i -gt 0 ]] && locations+=","
        locations+=$(cat <<EOF

    {
        "timestamp": $timestamp,
        "latitude": $lat,
        "longitude": $lon,
        "accuracy": $accuracy,
        "speed": ${speed:-5},
        "bearing": ${bearing:-0}
    }
EOF
)
    done
    locations+="
]"
    echo "$locations"
}

# Generate walking path (slow speed, frequent updates)
generate_walking_path() {
    local start_lat="$1"
    local start_lon="$2"
    local end_lat="$3"
    local end_lon="$4"
    local duration_minutes="${5:-30}"

    local num_points=$((duration_minutes * 60 / 5))  # Update every 5 seconds
    local interval_ms=5000
    # Let generate_location_path use its default past timestamp calculation
    generate_location_path "$start_lat" "$start_lon" "$end_lat" "$end_lon" "$num_points" "" "$interval_ms"
}

# Generate driving path (faster speed, less frequent updates)
generate_driving_path() {
    local start_lat="$1"
    local start_lon="$2"
    local end_lat="$3"
    local end_lon="$4"
    local duration_minutes="${5:-30}"

    local num_points=$((duration_minutes * 60 / 2))  # Update every 2 seconds
    local interval_ms=2000
    # Let generate_location_path use its default past timestamp calculation
    generate_location_path "$start_lat" "$start_lon" "$end_lat" "$end_lon" "$num_points" "" "$interval_ms"
}

# =============================================================================
# Movement Event Generation
# =============================================================================

# Generate single movement event JSON
generate_movement_event() {
    local trip_id="$1"
    local latitude="$2"
    local longitude="$3"
    local mode="${4:-IN_VEHICLE}"
    local confidence="${5:-0.9}"
    local timestamp="${6:-$(date +%s000)}"

    cat <<EOF
{
    "trip_id": "$trip_id",
    "timestamp": $timestamp,
    "latitude": $latitude,
    "longitude": $longitude,
    "accuracy": $(random_float 5 20 1),
    "speed": $(random_float 5 30 1),
    "transportation_mode": "$mode",
    "confidence": $confidence,
    "detection_source": "ACTIVITY_RECOGNITION"
}
EOF
}

# Generate movement events for a trip path
generate_movement_events_for_path() {
    local trip_id="$1"
    local start_lat="$2"
    local start_lon="$3"
    local end_lat="$4"
    local end_lon="$5"
    local num_events="${6:-10}"
    local mode="${7:-IN_VEHICLE}"
    local interval_ms="${9:-60000}"
    # Calculate start_time so all events end up in the past
    local default_start=$(($(date +%s) * 1000 - (num_events - 1) * interval_ms))
    local start_time="${8:-$default_start}"

    local events="["
    for i in $(seq 0 $((num_events - 1))); do
        local t=$(echo "scale=6; $i / ($num_events - 1)" | bc)
        local lat=$(echo "scale=6; $start_lat + ($end_lat - $start_lat) * $t" | bc)
        local lon=$(echo "scale=6; $start_lon + ($end_lon - $start_lon) * $t" | bc)
        local timestamp=$((start_time + i * interval_ms))
        local confidence=$(random_float 0.85 0.99 2)

        [[ $i -gt 0 ]] && events+=","
        events+=$(cat <<EOF

    {
        "trip_id": "$trip_id",
        "timestamp": $timestamp,
        "latitude": $lat,
        "longitude": $lon,
        "accuracy": $(random_float 5 20 1),
        "speed": $(random_float 10 30 1),
        "transportation_mode": "$mode",
        "confidence": $confidence,
        "detection_source": "ACTIVITY_RECOGNITION"
    }
EOF
)
    done
    events+="
]"
    echo "$events"
}

# Generate multi-modal trip events (walk -> vehicle -> walk)
generate_multimodal_events() {
    local trip_id="$1"
    local start_lat="$2"
    local start_lon="$3"
    local mid1_lat="$4"
    local mid1_lon="$5"
    local mid2_lat="$6"
    local mid2_lon="$7"
    local end_lat="$8"
    local end_lon="$9"

    local events="["
    local interval=30000  # 30 seconds
    # Calculate start time so all 20 events (5+10+5) end up in the past
    local total_events=20
    local time=$(($(date +%s) * 1000 - (total_events - 1) * interval))

    # Walking segment (5 events)
    for i in {0..4}; do
        local t=$(echo "scale=6; $i / 4" | bc)
        local lat=$(echo "scale=6; $start_lat + ($mid1_lat - $start_lat) * $t" | bc)
        local lon=$(echo "scale=6; $start_lon + ($mid1_lon - $start_lon) * $t" | bc)
        [[ $i -gt 0 ]] && events+=","
        events+=$(generate_movement_event "$trip_id" "$lat" "$lon" "WALKING" "0.92" "$time")
        time=$((time + interval))
    done

    # Vehicle segment (10 events)
    for i in {0..9}; do
        local t=$(echo "scale=6; $i / 9" | bc)
        local lat=$(echo "scale=6; $mid1_lat + ($mid2_lat - $mid1_lat) * $t" | bc)
        local lon=$(echo "scale=6; $mid1_lon + ($mid2_lon - $mid1_lon) * $t" | bc)
        events+=","
        events+=$(generate_movement_event "$trip_id" "$lat" "$lon" "IN_VEHICLE" "0.95" "$time")
        time=$((time + interval))
    done

    # Walking segment (5 events)
    for i in {0..4}; do
        local t=$(echo "scale=6; $i / 4" | bc)
        local lat=$(echo "scale=6; $mid2_lat + ($end_lat - $mid2_lat) * $t" | bc)
        local lon=$(echo "scale=6; $mid2_lon + ($end_lon - $mid2_lon) * $t" | bc)
        events+=","
        events+=$(generate_movement_event "$trip_id" "$lat" "$lon" "WALKING" "0.90" "$time")
        time=$((time + interval))
    done

    events+="
]"
    echo "$events"
}

# =============================================================================
# Geofence Generation
# =============================================================================

# Generate geofence at location
generate_geofence_data() {
    local name="$1"
    local latitude="$2"
    local longitude="$3"
    local radius="${4:-100}"
    local events="${5:-[\"enter\", \"exit\"]}"

    cat <<EOF
{
    "name": "$name",
    "latitude": $latitude,
    "longitude": $longitude,
    "radius_meters": $radius,
    "event_types": $events,
    "active": true
}
EOF
}

# =============================================================================
# Test Scenario Coordinates
# =============================================================================

# San Francisco commute route coordinates
get_sf_commute_coordinates() {
    echo "37.7749,-122.4194"   # Start: Downtown SF
    echo "37.7800,-122.4050"   # Highway on-ramp
    echo "37.7900,-122.3900"   # Highway
    echo "37.8000,-122.3800"   # Highway
    echo "37.8100,-122.3700"   # End: Oakland
}

# Home -> School route (shorter)
get_school_route_coordinates() {
    echo "37.7749,-122.4194"   # Home
    echo "37.7780,-122.4150"   # Walking
    echo "37.7800,-122.4100"   # Near school
    echo "37.7820,-122.4080"   # School
}

# =============================================================================
# User Generation
# =============================================================================

# Generate dynamic test user email
generate_test_email() {
    local prefix="${1:-user}"
    echo "${TEST_USER_PREFIX}${prefix}_$(date +%s)@e2e.phonemanager.local"
}

# Generate test user data JSON
generate_user_data() {
    local email="${1:-$(generate_test_email)}"
    local password="${2:-TestPass123!}"
    local display_name="${3:-E2E Test User $(generate_short_id)}"

    cat <<EOF
{
    "email": "$email",
    "password": "$password",
    "display_name": "$display_name"
}
EOF
}

# Generate user with random data
generate_random_user() {
    local prefix="${1:-user}"
    local email=$(generate_test_email "$prefix")
    local password="Test$(random_int 100 999)Pass!"
    local name="E2E $prefix $(generate_short_id)"
    echo "$email|$password|$name"
}

# =============================================================================
# Group Generation
# =============================================================================

# Generate group data JSON
generate_group_data() {
    local name="${1:-E2E Test Group $(generate_short_id)}"
    local description="${2:-Automated test group}"

    cat <<EOF
{
    "name": "$name",
    "description": "$description"
}
EOF
}

# Generate invite code (for validation tests)
generate_fake_invite_code() {
    local chars="ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    local code=""
    for i in {1..8}; do
        local idx=$((RANDOM % ${#chars}))
        code+="${chars:$idx:1}"
    done
    echo "$code"
}

# =============================================================================
# Settings Generation
# =============================================================================

# Generate device settings JSON
generate_device_settings() {
    local tracking_enabled="${1:-true}"
    local update_interval="${2:-60000}"
    local battery_optimization="${3:-false}"
    local weather_enabled="${4:-true}"
    local secret_mode="${5:-false}"

    cat <<EOF
{
    "tracking_enabled": $tracking_enabled,
    "location_update_interval_ms": $update_interval,
    "battery_optimization_enabled": $battery_optimization,
    "weather_notifications_enabled": $weather_enabled,
    "secret_mode_enabled": $secret_mode
}
EOF
}

# Generate settings lock data
generate_settings_lock() {
    local setting_key="$1"
    local reason="${2:-Locked by admin for testing}"

    cat <<EOF
{
    "setting_key": "$setting_key",
    "reason": "$reason"
}
EOF
}

# Generate unlock request data
generate_unlock_request() {
    local setting_key="$1"
    local reason="${2:-Need to adjust this setting for personal use}"

    cat <<EOF
{
    "setting_key": "$setting_key",
    "reason": "$reason"
}
EOF
}

# =============================================================================
# Trip Generation
# =============================================================================

# Generate trip data JSON for API upload
generate_trip_data() {
    local device_id="$1"
    local local_trip_id="${2:-$(generate_uuid)}"
    local start_lat="${3:-$SF_DOWNTOWN_LAT}"
    local start_lon="${4:-$SF_DOWNTOWN_LON}"
    local transport_mode="${5:-IN_VEHICLE}"
    local timestamp="${6:-$(date +%s000)}"

    cat <<EOF
{
    "device_id": "$device_id",
    "local_trip_id": "$local_trip_id",
    "start_timestamp": $timestamp,
    "start_latitude": $start_lat,
    "start_longitude": $start_lon,
    "transportation_mode": "$transport_mode",
    "detection_source": "ACTIVITY_RECOGNITION"
}
EOF
}

# Generate trip completion data
generate_trip_completion() {
    local end_lat="${1:-$OAKLAND_LAT}"
    local end_lon="${2:-$OAKLAND_LON}"
    local timestamp="${3:-$(date +%s000)}"

    cat <<EOF
{
    "state": "COMPLETED",
    "end_timestamp": $timestamp,
    "end_latitude": $end_lat,
    "end_longitude": $end_lon
}
EOF
}

# Generate a complete simulated trip with locations and events
generate_simulated_trip() {
    local device_id="$1"
    local start_lat="${2:-$SF_DOWNTOWN_LAT}"
    local start_lon="${3:-$SF_DOWNTOWN_LON}"
    local end_lat="${4:-$OAKLAND_LAT}"
    local end_lon="${5:-$OAKLAND_LON}"
    local duration_minutes="${6:-30}"
    local mode="${7:-IN_VEHICLE}"

    local trip_id=$(generate_uuid)
    local num_points=$((duration_minutes * 2))  # 2 points per minute
    local interval_ms=30000  # 30 seconds

    echo "{"
    echo "  \"trip_id\": \"$trip_id\","
    echo "  \"device_id\": \"$device_id\","
    echo "  \"transportation_mode\": \"$mode\","
    echo "  \"locations\": $(generate_location_path "$start_lat" "$start_lon" "$end_lat" "$end_lon" "$num_points" "" "$interval_ms"),"
    echo "  \"events\": $(generate_movement_events_for_path "$trip_id" "$start_lat" "$start_lon" "$end_lat" "$end_lon" "$num_points" "$mode" "" "$interval_ms")"
    echo "}"
}

# =============================================================================
# Weather Generation (for mocking)
# =============================================================================

# Generate weather data JSON (for mock responses)
generate_weather_data() {
    local temperature="${1:-18.5}"
    local condition="${2:-PARTLY_CLOUDY}"
    local humidity="${3:-65}"
    local wind_speed="${4:-12.5}"

    cat <<EOF
{
    "temperature": $temperature,
    "condition": "$condition",
    "humidity": $humidity,
    "wind_speed": $wind_speed,
    "feels_like": $(echo "scale=1; $temperature - 2" | bc),
    "timestamp": $(date +%s000)
}
EOF
}

# Generate 5-day forecast
generate_weather_forecast() {
    local base_temp="${1:-18}"

    echo "["
    for i in {0..4}; do
        local temp=$(echo "scale=1; $base_temp + $(random_float -5 5 1)" | bc)
        local conditions=("SUNNY" "PARTLY_CLOUDY" "CLOUDY" "RAINY" "CLEAR")
        local condition="${conditions[$((RANDOM % ${#conditions[@]}))]}"

        [[ $i -gt 0 ]] && echo ","
        cat <<EOF
    {
        "date": "$(date -v+${i}d +%Y-%m-%d 2>/dev/null || date -d "+${i} days" +%Y-%m-%d)",
        "high_temperature": $(echo "scale=1; $temp + 5" | bc),
        "low_temperature": $(echo "scale=1; $temp - 5" | bc),
        "condition": "$condition"
    }
EOF
    done
    echo "]"
}

# =============================================================================
# Geofence Generation (Extended)
# =============================================================================

# Generate geofence at predefined location
generate_home_geofence() {
    local device_id="$1"
    generate_geofence_data "Home" "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON" "$TEST_GEOFENCE_HOME_RADIUS"
}

generate_work_geofence() {
    local device_id="$1"
    generate_geofence_data "Work" "$TEST_LOC_WORK_LAT" "$TEST_LOC_WORK_LON" "$TEST_GEOFENCE_WORK_RADIUS"
}

generate_school_geofence() {
    local device_id="$1"
    generate_geofence_data "School" "$TEST_LOC_SCHOOL_LAT" "$TEST_LOC_SCHOOL_LON" "$TEST_GEOFENCE_SCHOOL_RADIUS"
}

# =============================================================================
# Proximity Alert Generation
# =============================================================================

# Generate proximity alert data
generate_proximity_alert_data() {
    local source_device_id="$1"
    local target_device_id="$2"
    local name="${3:-Proximity Alert $(generate_short_id)}"
    local radius="${4:-5000}"

    cat <<EOF
{
    "source_device_id": "$source_device_id",
    "target_device_id": "$target_device_id",
    "name": "$name",
    "radius_meters": $radius,
    "is_active": true
}
EOF
}

# =============================================================================
# Webhook Generation
# =============================================================================

# Generate webhook data
generate_webhook_data() {
    local device_id="$1"
    local url="${2:-https://webhook.test/e2e-$(generate_short_id)}"
    local events="${3:-[\"geofence_enter\", \"geofence_exit\"]}"
    local secret="${4:-test_secret_$(generate_short_id)}"

    cat <<EOF
{
    "device_id": "$device_id",
    "url": "$url",
    "events": $events,
    "secret": "$secret",
    "active": true
}
EOF
}

# =============================================================================
# Enrollment Generation
# =============================================================================

# Generate enrollment token (fake, for testing)
generate_fake_enrollment_token() {
    openssl rand -hex 16 2>/dev/null || generate_uuid | tr -d '-'
}

# Generate device info for enrollment
generate_enrollment_device_info() {
    local device_name="${1:-E2E Test Device}"
    local manufacturer="${2:-Google}"
    local model="${3:-Pixel 8a}"
    local os_version="${4:-14}"

    cat <<EOF
{
    "device_name": "$device_name",
    "manufacturer": "$manufacturer",
    "model": "$model",
    "os_version": "$os_version",
    "platform": "android"
}
EOF
}

# =============================================================================
# Test Scenario Helpers
# =============================================================================

# Generate family scenario data (3 users, 1 group)
generate_family_scenario() {
    local session_id="${1:-$(generate_short_id)}"

    cat <<EOF
{
    "session_id": "$session_id",
    "parent": {
        "email": "parent_${session_id}@e2e.phonemanager.local",
        "password": "Parent123!",
        "name": "Parent User"
    },
    "child1": {
        "email": "child1_${session_id}@e2e.phonemanager.local",
        "password": "Child123!",
        "name": "Child One"
    },
    "child2": {
        "email": "child2_${session_id}@e2e.phonemanager.local",
        "password": "Child123!",
        "name": "Child Two"
    },
    "group_name": "Family ${session_id}",
    "geofences": {
        "home": {
            "lat": $TEST_LOC_HOME_LAT,
            "lon": $TEST_LOC_HOME_LON,
            "radius": $TEST_GEOFENCE_HOME_RADIUS
        },
        "school": {
            "lat": $TEST_LOC_SCHOOL_LAT,
            "lon": $TEST_LOC_SCHOOL_LON,
            "radius": $TEST_GEOFENCE_SCHOOL_RADIUS
        }
    }
}
EOF
}

# Generate commute scenario data
generate_commute_scenario() {
    local device_id="$1"
    local session_id="${2:-$(generate_short_id)}"

    cat <<EOF
{
    "session_id": "$session_id",
    "device_id": "$device_id",
    "segments": [
        {
            "name": "walk_to_car",
            "start_lat": $TEST_LOC_HOME_LAT,
            "start_lon": $TEST_LOC_HOME_LON,
            "end_lat": $(echo "scale=6; $TEST_LOC_HOME_LAT + 0.002" | bc),
            "end_lon": $(echo "scale=6; $TEST_LOC_HOME_LON + 0.002" | bc),
            "mode": "WALKING",
            "duration_min": 5
        },
        {
            "name": "drive_to_work",
            "start_lat": $(echo "scale=6; $TEST_LOC_HOME_LAT + 0.002" | bc),
            "start_lon": $(echo "scale=6; $TEST_LOC_HOME_LON + 0.002" | bc),
            "end_lat": $TEST_LOC_WORK_LAT,
            "end_lon": $TEST_LOC_WORK_LON,
            "mode": "IN_VEHICLE",
            "duration_min": 25
        },
        {
            "name": "walk_to_office",
            "start_lat": $TEST_LOC_WORK_LAT,
            "start_lon": $TEST_LOC_WORK_LON,
            "end_lat": $(echo "scale=6; $TEST_LOC_WORK_LAT + 0.001" | bc),
            "end_lon": $(echo "scale=6; $TEST_LOC_WORK_LON + 0.001" | bc),
            "mode": "WALKING",
            "duration_min": 3
        }
    ]
}
EOF
}

echo "Generators loaded"

# =============================================================================
# Aliases for backward compatibility
# =============================================================================
generate_device_id() {
    generate_uuid
}

