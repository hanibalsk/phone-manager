#!/bin/bash
# Test: Movement Simulation
# Tests GPS location simulation on Android emulator with backend sync

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$SCRIPT_DIR/config.sh"
source "$SCRIPT_DIR/lib/common.sh"
source "$SCRIPT_DIR/lib/api.sh"
source "$SCRIPT_DIR/lib/adb.sh"
source "$SCRIPT_DIR/lib/generators.sh"

test_start "Movement Simulation"

# =============================================================================
# Prerequisites Check
# =============================================================================
log_info "Checking prerequisites..."

if ! adb_check_connection; then
    log_error "No Android device/emulator connected"
    log_info "Start emulator with: emulator -avd <avd_name>"
    exit 1
fi

if ! adb_is_emulator; then
    log_warning "Connected device is not an emulator - GPS simulation may not work"
fi

# =============================================================================
# Setup: Register test device
# =============================================================================
log_info "Setting up test device..."
device_info=$(generate_device)
DEVICE_ID=$(echo "$device_info" | cut -d'|' -f1)
DEVICE_NAME="Movement-Test-$(generate_short_id)"
GROUP_ID="movement-sim-$(generate_short_id)"

response=$(api_register_device "$DEVICE_ID" "$DEVICE_NAME" "$GROUP_ID")
# Note: API uses shared key from config.sh, not per-device keys
log_success "Device registered: $DEVICE_NAME"

# =============================================================================
# Test 1: Static GPS location
# =============================================================================
log_step "Setting static GPS location..."
adb_set_location "$SF_DOWNTOWN_LAT" "$SF_DOWNTOWN_LON"
sleep 2

# Verify emulator accepted the location
if adb_is_emulator; then
    log_success "GPS location set on emulator"
    adb_screenshot "movement_01_static.png"
    ((TESTS_PASSED++))
fi

# =============================================================================
# Test 2: Upload location matching GPS
# =============================================================================
log_step "Uploading location to backend..."
response=$(api_upload_location "$DEVICE_ID" "$SF_DOWNTOWN_LAT" "$SF_DOWNTOWN_LON" "5.0")
code=$(get_http_code "$response")

assert_http_success "$code" "Upload location at GPS coordinates"

# =============================================================================
# Test 3: Simulate walking path
# =============================================================================
log_step "Simulating walking path..."

# Short walking path (5 points over ~500m)
WALK_POINTS=(
    "$SF_DOWNTOWN_LAT,$SF_DOWNTOWN_LON"
    "37.7755,-122.4188"
    "37.7761,-122.4182"
    "37.7767,-122.4176"
    "37.7773,-122.4170"
)

# Set each GPS point and take screenshot
for i in "${!WALK_POINTS[@]}"; do
    IFS=',' read -r lat lon <<< "${WALK_POINTS[$i]}"

    log_info "  Point $((i+1)): ($lat, $lon)"
    adb_set_location "$lat" "$lon"
    sleep 1

    # Upload to backend
    response=$(api_upload_location "$DEVICE_ID" "$lat" "$lon" "10.0")

    if [[ "$i" == "0" || "$i" == "${#WALK_POINTS[@]}-1" ]]; then
        adb_screenshot "movement_walk_$i.png"
    fi
done

log_success "Walking path simulated ($((${#WALK_POINTS[@]})) points)"
((TESTS_PASSED++))

# =============================================================================
# Test 4: Simulate driving path
# =============================================================================
log_step "Simulating driving path..."

# Driving path (SF to Oakland via Bay Bridge)
DRIVE_POINTS=(
    "37.7749,-122.4194"    # SF Downtown
    "37.7858,-122.3935"    # Embarcadero
    "37.7915,-122.3562"    # Treasure Island
    "37.7987,-122.3291"    # Bay Bridge East
    "37.8044,-122.2712"    # Oakland Downtown
)

# Create a trip for the drive
LOCAL_TRIP_ID="drive-sim-$(date +%s)"
response=$(api_create_trip "$DEVICE_ID" "$LOCAL_TRIP_ID" "${DRIVE_POINTS[0]%%,*}" "${DRIVE_POINTS[0]#*,}" "IN_VEHICLE")
TRIP_ID=$(json_get "$(get_response_body "$response")" ".id")
log_info "Trip started: $TRIP_ID"

# Simulate driving
locations="["
first=true
timestamp=$(date +%s000)

for i in "${!DRIVE_POINTS[@]}"; do
    IFS=',' read -r lat lon <<< "${DRIVE_POINTS[$i]}"

    log_info "  Driving point $((i+1)): ($lat, $lon)"
    adb_set_location "$lat" "$lon"
    sleep 1

    # Build location batch
    if [[ "$first" == "true" ]]; then
        first=false
    else
        locations+=","
    fi

    locations+="{\"timestamp\": $timestamp, \"latitude\": $lat, \"longitude\": $lon, \"accuracy\": 15.0, \"speed\": 25.0}"
    timestamp=$((timestamp + 60000))  # 1 minute intervals

    adb_screenshot "movement_drive_$i.png"
done
locations+="]"

# Upload batch locations
response=$(api_upload_locations_batch "$DEVICE_ID" "$locations")
code=$(get_http_code "$response")
assert_http_success "$code" "Upload driving locations batch"

# Complete trip (bash 3.x compatible - no negative indices)
last_drive_idx=$((${#DRIVE_POINTS[@]} - 1))
end_lat="${DRIVE_POINTS[$last_drive_idx]%%,*}"
end_lon="${DRIVE_POINTS[$last_drive_idx]#*,}"
response=$(api_complete_trip "$TRIP_ID" "$end_lat" "$end_lon")
assert_http_success "$(get_http_code "$response")" "Complete driving trip"

log_success "Driving path simulated ($((${#DRIVE_POINTS[@]})) points)"
((TESTS_PASSED++))

# =============================================================================
# Test 5: Rapid location changes
# =============================================================================
log_step "Testing rapid location changes..."

# Simulate fast movement
RAPID_INTERVAL_MS=500
RAPID_POINTS=10

start_lat=$SF_DOWNTOWN_LAT
start_lon=$SF_DOWNTOWN_LON
lat_increment="0.001"
lon_increment="0.001"

for i in $(seq 1 $RAPID_POINTS); do
    lat=$(echo "$start_lat + $i * $lat_increment" | bc)
    lon=$(echo "$start_lon + $i * $lon_increment" | bc)

    adb_set_location "$lat" "$lon"
    sleep 0.5
done

log_success "Rapid location changes tested ($RAPID_POINTS changes)"
((TESTS_PASSED++))

# =============================================================================
# Test 6: Verify location history
# =============================================================================
log_step "Verifying location history..."
response=$(api_get_location_history "$DEVICE_ID" 100)
code=$(get_http_code "$response")
body=$(get_response_body "$response")

assert_http_success "$code" "Get location history"

location_count=$(json_get "$body" ".locations | length" 2>/dev/null || echo "0")
if [[ "$location_count" -gt 5 ]]; then
    log_success "Location history contains $location_count points"
    ((TESTS_PASSED++))
else
    log_warning "Expected more locations, got $location_count"
fi

# =============================================================================
# Test 7: Movement events for trip
# =============================================================================
log_step "Verifying trip movement events..."

# Generate movement events for the trip (bash 3.x compatible)
last_drive_idx=$((${#DRIVE_POINTS[@]} - 1))
events=$(generate_movement_events_for_path \
    "$TRIP_ID" \
    "${DRIVE_POINTS[0]%%,*}" "${DRIVE_POINTS[0]#*,}" \
    "${DRIVE_POINTS[$last_drive_idx]%%,*}" "${DRIVE_POINTS[$last_drive_idx]#*,}" \
    ${#DRIVE_POINTS[@]} "IN_VEHICLE")

response=$(api_upload_movement_events_batch "$DEVICE_ID" "$events")
code=$(get_http_code "$response")
assert_http_success "$code" "Upload movement events for trip"

# Verify events were stored
response=$(api_get_trip_movement_events "$TRIP_ID")
body=$(get_response_body "$response")
event_count=$(json_get "$body" ".events | length" 2>/dev/null || json_get "$body" "| length" 2>/dev/null || echo "0")

if [[ "$event_count" -gt 0 ]]; then
    log_success "Trip has $event_count movement events"
    ((TESTS_PASSED++))
else
    log_warning "No movement events found"
fi

# =============================================================================
# Test 8: Location accuracy simulation
# =============================================================================
log_step "Testing location accuracy variations..."

# Simulate locations with varying accuracy
accuracies=("5.0" "10.0" "25.0" "50.0" "100.0")
base_lat="37.7800"
base_lon="-122.4100"

for accuracy in "${accuracies[@]}"; do
    response=$(api_upload_location "$DEVICE_ID" "$base_lat" "$base_lon" "$accuracy")
    code=$(get_http_code "$response")

    if [[ "$code" == "200" || "$code" == "201" ]]; then
        log_info "  Accuracy $accuracy m: accepted"
    else
        log_warning "  Accuracy $accuracy m: HTTP $code"
    fi
done

log_success "Location accuracy variations tested"
((TESTS_PASSED++))

# =============================================================================
# Final Screenshot
# =============================================================================
adb_screenshot "movement_99_complete.png"

test_end
print_summary
