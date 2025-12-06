#!/bin/bash
# Scenario: Daily Commute
# Simulates a complete home-to-work commute with:
# - Geofence exit from home
# - Trip with IN_VEHICLE mode
# - Movement events along route
# - Geofence enter at work
# - Trip completion

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$SCRIPT_DIR/config.sh"
source "$SCRIPT_DIR/lib/common.sh"
source "$SCRIPT_DIR/lib/api.sh"
source "$SCRIPT_DIR/lib/adb.sh"
source "$SCRIPT_DIR/lib/generators.sh"

test_start "Daily Commute Scenario"

# =============================================================================
# Scenario Configuration
# =============================================================================

# Home location (Downtown SF)
HOME_LAT="37.7749"
HOME_LON="-122.4194"
HOME_NAME="Home"
HOME_RADIUS="100"

# Work location (Oakland)
WORK_LAT="37.8044"
WORK_LON="-122.2712"
WORK_NAME="Work"
WORK_RADIUS="200"

# Commute duration simulation
NUM_LOCATIONS="20"
COMMUTE_DURATION_MS=$((30 * 60 * 1000))  # 30 minutes

log_info "=== Commute Scenario ==="
log_info "From: $HOME_NAME ($HOME_LAT, $HOME_LON)"
log_info "To:   $WORK_NAME ($WORK_LAT, $WORK_LON)"

# =============================================================================
# Setup: Register device and create geofences
# =============================================================================
log_info "Setting up device and geofences..."

device_info=$(generate_device)
DEVICE_ID=$(echo "$device_info" | cut -d'|' -f1)
DEVICE_NAME="Commuter $(generate_short_id)"
GROUP_ID="commute-scenario-$(generate_short_id)"

# Register device
response=$(api_register_device "$DEVICE_ID" "$DEVICE_NAME" "$GROUP_ID")
export API_KEY=$(json_get "$(get_response_body "$response")" ".api_key")
log_success "Device registered: $DEVICE_NAME"

# Create Home geofence (exit trigger)
log_step "Creating Home geofence..."
response=$(api_create_geofence "$DEVICE_ID" "$HOME_NAME" "$HOME_LAT" "$HOME_LON" "$HOME_RADIUS" '["exit"]')
code=$(get_http_code "$response")
HOME_GEOFENCE_ID=$(json_get "$(get_response_body "$response")" ".id")

if [[ "$code" == "201" ]]; then
    log_success "Home geofence created: $HOME_GEOFENCE_ID"
    ((TESTS_PASSED++))
else
    log_error "Failed to create Home geofence"
    ((TESTS_FAILED++))
fi

# Create Work geofence (enter trigger)
log_step "Creating Work geofence..."
response=$(api_create_geofence "$DEVICE_ID" "$WORK_NAME" "$WORK_LAT" "$WORK_LON" "$WORK_RADIUS" '["enter"]')
code=$(get_http_code "$response")
WORK_GEOFENCE_ID=$(json_get "$(get_response_body "$response")" ".id")

if [[ "$code" == "201" ]]; then
    log_success "Work geofence created: $WORK_GEOFENCE_ID"
    ((TESTS_PASSED++))
else
    log_error "Failed to create Work geofence"
    ((TESTS_FAILED++))
fi

# =============================================================================
# Phase 1: Start at home (inside geofence)
# =============================================================================
log_info "=== Phase 1: At Home ==="

# Upload location at home
log_step "Recording home location..."
response=$(api_upload_location "$DEVICE_ID" "$HOME_LAT" "$HOME_LON" "10.0")
assert_http_success "$(get_http_code "$response")" "Upload home location"

# Simulate GPS on emulator if connected
if adb_check_connection && adb_is_emulator; then
    log_step "Setting emulator GPS to home..."
    adb_set_location "$HOME_LAT" "$HOME_LON"
    sleep 2
    adb_screenshot "commute_01_at_home.png"
fi

# =============================================================================
# Phase 2: Leave home (geofence exit, trip start)
# =============================================================================
log_info "=== Phase 2: Leaving Home ==="

# Create trip (triggered by geofence exit)
LOCAL_TRIP_ID="commute-$(date +%s)"
TRIP_START_TIME=$(date +%s000)

log_step "Starting trip..."
response=$(api_create_trip "$DEVICE_ID" "$LOCAL_TRIP_ID" "$HOME_LAT" "$HOME_LON" "IN_VEHICLE" "GEOFENCE_EXIT" "$TRIP_START_TIME")
code=$(get_http_code "$response")
body=$(get_response_body "$response")

TRIP_ID=$(json_get "$body" ".id")
assert_http_status "$code" "201" "Create commute trip"
log_success "Trip started: $TRIP_ID"

# =============================================================================
# Phase 3: Driving to work (location updates + movement events)
# =============================================================================
log_info "=== Phase 3: Driving ==="

# Generate route locations
log_step "Generating commute route ($NUM_LOCATIONS points)..."
INTERVAL_MS=$((COMMUTE_DURATION_MS / NUM_LOCATIONS))

locations=$(generate_location_path \
    "$HOME_LAT" "$HOME_LON" \
    "$WORK_LAT" "$WORK_LON" \
    "$NUM_LOCATIONS" \
    "$TRIP_START_TIME" \
    "$INTERVAL_MS")

# Upload locations
log_step "Uploading route locations..."
response=$(api_upload_locations_batch "$DEVICE_ID" "$locations")
code=$(get_http_code "$response")
assert_http_success "$code" "Upload commute locations"

# Generate and upload movement events
log_step "Recording movement events..."
events=$(generate_movement_events_for_path \
    "$TRIP_ID" \
    "$HOME_LAT" "$HOME_LON" \
    "$WORK_LAT" "$WORK_LON" \
    "$NUM_LOCATIONS" \
    "IN_VEHICLE" \
    "$TRIP_START_TIME" \
    "$INTERVAL_MS")

response=$(api_upload_movement_events_batch "$DEVICE_ID" "$events")
code=$(get_http_code "$response")
assert_http_success "$code" "Upload movement events"

# Simulate movement on emulator
if adb_check_connection && adb_is_emulator; then
    log_step "Simulating movement on emulator..."

    # Move through key waypoints
    waypoints=(
        "$HOME_LAT,$HOME_LON"
        "37.7850,-122.4100"
        "37.7950,-122.3900"
        "$WORK_LAT,$WORK_LON"
    )

    for i in "${!waypoints[@]}"; do
        IFS=',' read -r lat lon <<< "${waypoints[$i]}"
        adb_set_location "$lat" "$lon"
        sleep 1
        adb_screenshot "commute_driving_$i.png"
    done
fi

# =============================================================================
# Phase 4: Arrive at work (geofence enter, trip end)
# =============================================================================
log_info "=== Phase 4: Arriving at Work ==="

# Upload final location at work
log_step "Recording work arrival..."
response=$(api_upload_location "$DEVICE_ID" "$WORK_LAT" "$WORK_LON" "8.0")
assert_http_success "$(get_http_code "$response")" "Upload work location"

# Complete trip
TRIP_END_TIME=$(date +%s000)
log_step "Completing trip..."
response=$(api_complete_trip "$TRIP_ID" "$WORK_LAT" "$WORK_LON" "$TRIP_END_TIME")
code=$(get_http_code "$response")
body=$(get_response_body "$response")

assert_http_success "$code" "Complete trip"

trip_state=$(json_get "$body" ".state")
assert_equals "COMPLETED" "$trip_state" "Trip should be COMPLETED"

if adb_check_connection && adb_is_emulator; then
    adb_set_location "$WORK_LAT" "$WORK_LON"
    sleep 1
    adb_screenshot "commute_05_at_work.png"
fi

# =============================================================================
# Phase 5: Verify data
# =============================================================================
log_info "=== Phase 5: Verification ==="

# Verify trip in history
log_step "Verifying trip in history..."
response=$(api_get_device_trips "$DEVICE_ID")
body=$(get_response_body "$response")

assert_contains "$body" "$TRIP_ID" "Trip should be in device history"

# Verify location history
log_step "Verifying location history..."
response=$(api_get_location_history "$DEVICE_ID" 50)
body=$(get_response_body "$response")
location_count=$(json_get "$body" ".locations | length" 2>/dev/null || echo "0")

if [[ "$location_count" -ge "$NUM_LOCATIONS" ]]; then
    log_success "Location history contains $location_count points"
    ((TESTS_PASSED++))
else
    log_warning "Expected at least $NUM_LOCATIONS locations, got $location_count"
fi

# Verify movement events
log_step "Verifying movement events..."
response=$(api_get_trip_movement_events "$TRIP_ID")
body=$(get_response_body "$response")
event_count=$(json_get "$body" ".events | length" 2>/dev/null || json_get "$body" "| length" 2>/dev/null || echo "0")

if [[ "$event_count" -gt 0 ]]; then
    log_success "Trip has $event_count movement events"
    ((TESTS_PASSED++))
else
    log_warning "No movement events found for trip"
fi

# Verify geofences still exist
log_step "Verifying geofences..."
response=$(api_get_geofences "$DEVICE_ID")
body=$(get_response_body "$response")

assert_contains "$body" "$HOME_NAME" "Home geofence should exist"
assert_contains "$body" "$WORK_NAME" "Work geofence should exist"

# =============================================================================
# Cleanup
# =============================================================================
log_info "=== Cleanup ==="

# Delete geofences
log_step "Cleaning up geofences..."
api_delete_geofence "$HOME_GEOFENCE_ID" >/dev/null 2>&1 || true
api_delete_geofence "$WORK_GEOFENCE_ID" >/dev/null 2>&1 || true
log_success "Geofences cleaned up"

# =============================================================================
# Summary
# =============================================================================
test_end

log_info "Commute simulation complete!"
log_info "Trip ID: $TRIP_ID"
log_info "Duration: $((COMMUTE_DURATION_MS / 1000 / 60)) minutes (simulated)"
log_info "Distance: ~15 km (SF to Oakland)"

print_summary
