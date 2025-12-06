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

echo "Generators loaded"
