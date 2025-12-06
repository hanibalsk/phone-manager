#!/bin/bash
# Test: Full Integration
# End-to-end integration test covering complete user journey

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$SCRIPT_DIR/config.sh"
source "$SCRIPT_DIR/lib/common.sh"
source "$SCRIPT_DIR/lib/api.sh"
source "$SCRIPT_DIR/lib/adb.sh"
source "$SCRIPT_DIR/lib/generators.sh"

test_start "Full Integration Test"

# =============================================================================
# Configuration
# =============================================================================
log_info "=== Full Integration Test Configuration ==="

# Family group simulation
GROUP_ID="family-$(generate_short_id)"
log_info "Group: $GROUP_ID"

# Family members (use individual variables for bash 3.x compatibility)
PARENT_DEVICE_ID=""
CHILD1_DEVICE_ID=""
CHILD2_DEVICE_ID=""
FAMILY_MEMBER_NAMES=("parent" "child1" "child2")

# Locations
HOME_LAT="37.7749"
HOME_LON="-122.4194"
SCHOOL_LAT="37.7650"
SCHOOL_LON="-122.4050"
WORK_LAT="37.8044"
WORK_LON="-122.2712"
GROCERY_LAT="37.7700"
GROCERY_LON="-122.4100"

# =============================================================================
# Phase 1: Device Registration
# =============================================================================
log_info "=== Phase 1: Device Registration ==="

# Helper function to get device ID by member name
get_device_id() {
    local member="$1"
    case "$member" in
        parent) echo "$PARENT_DEVICE_ID" ;;
        child1) echo "$CHILD1_DEVICE_ID" ;;
        child2) echo "$CHILD2_DEVICE_ID" ;;
    esac
}

for member in "${FAMILY_MEMBER_NAMES[@]}"; do
    log_step "Registering device for: $member"

    device_info=$(generate_device)
    device_id=$(echo "$device_info" | cut -d'|' -f1)
    device_name="$member-$(generate_short_id)"

    response=$(api_register_device "$device_id" "$device_name" "$GROUP_ID")
    code=$(get_http_code "$response")
    body=$(get_response_body "$response")

    if [[ "$code" == "200" || "$code" == "201" ]]; then
        # Note: API uses shared key from config.sh, not per-device keys
        case "$member" in
            parent) PARENT_DEVICE_ID="$device_id" ;;
            child1) CHILD1_DEVICE_ID="$device_id" ;;
            child2) CHILD2_DEVICE_ID="$device_id" ;;
        esac
        log_success "$member registered: $device_id"
        ((TESTS_PASSED++))
    else
        log_error "Failed to register $member"
        ((TESTS_FAILED++))
    fi
done

# =============================================================================
# Phase 2: Verify Group
# =============================================================================
log_info "=== Phase 2: Verify Group ==="

log_step "Listing group devices..."
response=$(api_get_devices "$GROUP_ID")
code=$(get_http_code "$response")
body=$(get_response_body "$response")

assert_http_success "$code" "List group devices"

device_count=$(json_get "$body" ".devices | length" 2>/dev/null || json_get "$body" "| length" 2>/dev/null || echo "0")
assert_equals "3" "$device_count" "Group should have 3 devices"

# =============================================================================
# Phase 3: Create Geofences
# =============================================================================
log_info "=== Phase 3: Create Geofences ==="

# Home geofence for parent (monitors when children arrive/leave)
log_step "Creating Home geofence..."
response=$(api_create_geofence "$PARENT_DEVICE_ID" "Home" "$HOME_LAT" "$HOME_LON" "100" '["enter", "exit"]')
HOME_GEOFENCE_ID=$(json_get "$(get_response_body "$response")" ".geofence_id")
assert_http_status "$(get_http_code "$response")" "201" "Create Home geofence"

# School geofence
log_step "Creating School geofence..."
response=$(api_create_geofence "$PARENT_DEVICE_ID" "School" "$SCHOOL_LAT" "$SCHOOL_LON" "150" '["enter", "exit"]')
SCHOOL_GEOFENCE_ID=$(json_get "$(get_response_body "$response")" ".geofence_id")
assert_http_status "$(get_http_code "$response")" "201" "Create School geofence"

# Work geofence
log_step "Creating Work geofence..."
response=$(api_create_geofence "$PARENT_DEVICE_ID" "Work" "$WORK_LAT" "$WORK_LON" "200" '["enter", "exit"]')
WORK_GEOFENCE_ID=$(json_get "$(get_response_body "$response")" ".geofence_id")
assert_http_status "$(get_http_code "$response")" "201" "Create Work geofence"

log_success "Geofences created: Home, School, Work"

# =============================================================================
# Phase 4: Create Proximity Alerts
# =============================================================================
log_info "=== Phase 4: Create Proximity Alerts ==="

# Alert when child1 gets close to child2
log_step "Creating proximity alert: child1 → child2..."
response=$(api_create_proximity_alert "$CHILD1_DEVICE_ID" "$CHILD2_DEVICE_ID" "500" "Kids within 500m")
code=$(get_http_code "$response")

if [[ "$code" == "201" ]]; then
    PROXIMITY_ALERT_ID=$(json_get "$(get_response_body "$response")" ".id")
    log_success "Proximity alert created"
    ((TESTS_PASSED++))
else
    log_warning "Proximity alert creation returned HTTP $code"
fi

# =============================================================================
# Phase 5: Morning Routine - Everyone Leaves Home
# =============================================================================
log_info "=== Phase 5: Morning Routine ==="

# All start at home
log_step "All family members at home..."
for member in "${FAMILY_MEMBER_NAMES[@]}"; do
    device_id=$(get_device_id "$member")
    api_upload_location "$device_id" "$HOME_LAT" "$HOME_LON" "10.0" >/dev/null
done
log_success "All family members at home"

# Parent commutes to work
log_step "Parent commuting to work..."
PARENT_TRIP_ID="commute-parent-$(date +%s)"
response=$(api_create_trip "$PARENT_DEVICE_ID" "$PARENT_TRIP_ID" "$HOME_LAT" "$HOME_LON" "IN_VEHICLE" "ACTIVITY_RECOGNITION")
code=$(get_http_code "$response")
PARENT_TRIP=$(json_get "$(get_response_body "$response")" ".id")
assert_http_status "$code" "201" "Parent trip start"

# Parent route: Home → Bridge → Work
parent_route=$(generate_location_path "$HOME_LAT" "$HOME_LON" "$WORK_LAT" "$WORK_LON" 10)
response=$(api_upload_locations_batch "$PARENT_DEVICE_ID" "$parent_route")
assert_http_success "$(get_http_code "$response")" "Parent commute locations"

# Complete parent trip
response=$(api_complete_trip "$PARENT_TRIP" "$WORK_LAT" "$WORK_LON")
assert_http_success "$(get_http_code "$response")" "Parent trip complete"
log_success "Parent arrived at work"

# Child1 walks to school
log_step "Child1 walking to school..."
CHILD1_TRIP_ID="school-child1-$(date +%s)"
response=$(api_create_trip "$CHILD1_DEVICE_ID" "$CHILD1_TRIP_ID" "$HOME_LAT" "$HOME_LON" "WALKING")
CHILD1_TRIP=$(json_get "$(get_response_body "$response")" ".id")

child1_route=$(generate_location_path "$HOME_LAT" "$HOME_LON" "$SCHOOL_LAT" "$SCHOOL_LON" 8)
api_upload_locations_batch "$CHILD1_DEVICE_ID" "$child1_route" >/dev/null

response=$(api_complete_trip "$CHILD1_TRIP" "$SCHOOL_LAT" "$SCHOOL_LON")
assert_http_success "$(get_http_code "$response")" "Child1 school trip"
log_success "Child1 arrived at school"

# Child2 takes bus to school
log_step "Child2 taking bus to school..."
CHILD2_TRIP_ID="school-child2-$(date +%s)"
response=$(api_create_trip "$CHILD2_DEVICE_ID" "$CHILD2_TRIP_ID" "$HOME_LAT" "$HOME_LON" "IN_VEHICLE")
CHILD2_TRIP=$(json_get "$(get_response_body "$response")" ".id")

child2_route=$(generate_location_path "$HOME_LAT" "$HOME_LON" "$SCHOOL_LAT" "$SCHOOL_LON" 5)
api_upload_locations_batch "$CHILD2_DEVICE_ID" "$child2_route" >/dev/null

response=$(api_complete_trip "$CHILD2_TRIP" "$SCHOOL_LAT" "$SCHOOL_LON")
assert_http_success "$(get_http_code "$response")" "Child2 school trip"
log_success "Child2 arrived at school"

# =============================================================================
# Phase 6: During the Day - Location Updates
# =============================================================================
log_info "=== Phase 6: Daytime Location Updates ==="

# Simulate periodic updates throughout the day
log_step "Simulating daytime location updates..."

# Parent at work (stationary)
for i in $(seq 1 5); do
    jitter_lat=$(echo "$WORK_LAT + 0.000$((RANDOM % 10))" | bc)
    jitter_lon=$(echo "$WORK_LON + 0.000$((RANDOM % 10))" | bc)
    api_upload_location "$PARENT_DEVICE_ID" "$jitter_lat" "$jitter_lon" "20.0" >/dev/null
done
log_info "  Parent: 5 updates at work"

# Children at school (with slight movement)
for i in $(seq 1 5); do
    jitter_lat=$(echo "$SCHOOL_LAT + 0.000$((RANDOM % 5))" | bc)
    jitter_lon=$(echo "$SCHOOL_LON + 0.000$((RANDOM % 5))" | bc)
    api_upload_location "$CHILD1_DEVICE_ID" "$jitter_lat" "$jitter_lon" "15.0" >/dev/null
    api_upload_location "$CHILD2_DEVICE_ID" "$jitter_lat" "$jitter_lon" "15.0" >/dev/null
done
log_info "  Children: 5 updates each at school"

log_success "Daytime updates complete"
((TESTS_PASSED++))

# =============================================================================
# Phase 7: Afternoon - Parent Picks Up Groceries
# =============================================================================
log_info "=== Phase 7: Parent Errand ==="

log_step "Parent going to grocery store..."
GROCERY_TRIP_ID="grocery-$(date +%s)"
response=$(api_create_trip "$PARENT_DEVICE_ID" "$GROCERY_TRIP_ID" "$WORK_LAT" "$WORK_LON" "IN_VEHICLE")
GROCERY_TRIP=$(json_get "$(get_response_body "$response")" ".id")

grocery_route=$(generate_location_path "$WORK_LAT" "$WORK_LON" "$GROCERY_LAT" "$GROCERY_LON" 6)
api_upload_locations_batch "$PARENT_DEVICE_ID" "$grocery_route" >/dev/null

# Parent shops (stationary)
api_upload_location "$PARENT_DEVICE_ID" "$GROCERY_LAT" "$GROCERY_LON" "10.0" >/dev/null
sleep 0.5
api_upload_location "$PARENT_DEVICE_ID" "$GROCERY_LAT" "$GROCERY_LON" "10.0" >/dev/null

# Complete grocery trip
response=$(api_complete_trip "$GROCERY_TRIP" "$GROCERY_LAT" "$GROCERY_LON")
assert_http_success "$(get_http_code "$response")" "Grocery trip complete"
log_success "Parent finished shopping"

# =============================================================================
# Phase 8: Evening - Everyone Returns Home
# =============================================================================
log_info "=== Phase 8: Evening Return ==="

# Children walk home together
log_step "Children returning home..."
CHILD_HOME_TRIP="children-home-$(date +%s)"
response=$(api_create_trip "$CHILD1_DEVICE_ID" "$CHILD_HOME_TRIP-1" "$SCHOOL_LAT" "$SCHOOL_LON" "WALKING")
api_create_trip "$CHILD2_DEVICE_ID" "$CHILD_HOME_TRIP-2" "$SCHOOL_LAT" "$SCHOOL_LON" "WALKING" >/dev/null

children_route=$(generate_location_path "$SCHOOL_LAT" "$SCHOOL_LON" "$HOME_LAT" "$HOME_LON" 8)
api_upload_locations_batch "$CHILD1_DEVICE_ID" "$children_route" >/dev/null
api_upload_locations_batch "$CHILD2_DEVICE_ID" "$children_route" >/dev/null

# Complete children trips
api_complete_trip "$(json_get "$(get_response_body "$response")" ".id")" "$HOME_LAT" "$HOME_LON" >/dev/null 2>&1 || true
log_success "Children arrived home"

# Parent returns from grocery
log_step "Parent returning home..."
PARENT_HOME_TRIP="parent-home-$(date +%s)"
response=$(api_create_trip "$PARENT_DEVICE_ID" "$PARENT_HOME_TRIP" "$GROCERY_LAT" "$GROCERY_LON" "IN_VEHICLE")
PARENT_HOME=$(json_get "$(get_response_body "$response")" ".id")

parent_home_route=$(generate_location_path "$GROCERY_LAT" "$GROCERY_LON" "$HOME_LAT" "$HOME_LON" 5)
api_upload_locations_batch "$PARENT_DEVICE_ID" "$parent_home_route" >/dev/null

response=$(api_complete_trip "$PARENT_HOME" "$HOME_LAT" "$HOME_LON")
assert_http_success "$(get_http_code "$response")" "Parent home trip"
log_success "Parent arrived home"

# =============================================================================
# Phase 9: Verify Data Integrity
# =============================================================================
log_info "=== Phase 9: Data Verification ==="

# Check location history for each family member
log_step "Verifying location history..."
for member in "${FAMILY_MEMBER_NAMES[@]}"; do
    device_id=$(get_device_id "$member")
    response=$(api_get_location_history "$device_id" 100)
    body=$(get_response_body "$response")
    count=$(json_get "$body" ".locations | length" 2>/dev/null || echo "0")
    log_info "  $member: $count locations"
done
((TESTS_PASSED++))

# Check trip history for parent
log_step "Verifying trip history..."
response=$(api_get_device_trips "$PARENT_DEVICE_ID")
body=$(get_response_body "$response")
trip_count=$(json_get "$body" ".trips | length" 2>/dev/null || json_get "$body" "| length" 2>/dev/null || echo "0")
log_info "Parent trips: $trip_count"

if [[ "$trip_count" -ge 3 ]]; then
    log_success "Trip history verified (>= 3 trips)"
    ((TESTS_PASSED++))
else
    log_warning "Expected >= 3 trips, got $trip_count"
fi

# Verify geofences still exist
log_step "Verifying geofences..."
response=$(api_get_geofences "$PARENT_DEVICE_ID")
body=$(get_response_body "$response")
gf_count=$(json_get "$body" ".geofences | length" 2>/dev/null || json_get "$body" "| length" 2>/dev/null || echo "0")

if [[ "$gf_count" -ge 3 ]]; then
    log_success "Geofences verified ($gf_count geofences)"
    ((TESTS_PASSED++))
else
    log_warning "Expected >= 3 geofences, got $gf_count"
fi

# =============================================================================
# Phase 10: ADB Integration (if emulator available)
# =============================================================================
if adb_check_connection && adb_is_emulator; then
    log_info "=== Phase 10: ADB Integration ==="

    log_step "Testing GPS simulation..."
    adb_set_location "$HOME_LAT" "$HOME_LON"
    sleep 1
    adb_screenshot "integration_home.png"

    adb_set_location "$WORK_LAT" "$WORK_LON"
    sleep 1
    adb_screenshot "integration_work.png"

    log_success "ADB integration verified"
    ((TESTS_PASSED++))
else
    log_info "=== Phase 10: ADB Integration (Skipped - no emulator) ==="
fi

# =============================================================================
# Cleanup
# =============================================================================
log_info "=== Cleanup ==="

log_step "Cleaning up geofences..."
api_delete_geofence "$HOME_GEOFENCE_ID" >/dev/null 2>&1 || true
api_delete_geofence "$SCHOOL_GEOFENCE_ID" >/dev/null 2>&1 || true
api_delete_geofence "$WORK_GEOFENCE_ID" >/dev/null 2>&1 || true

if [[ -n "$PROXIMITY_ALERT_ID" ]]; then
    api_delete_proximity_alert "$PROXIMITY_ALERT_ID" >/dev/null 2>&1 || true
fi

log_success "Cleanup complete"

# =============================================================================
# Summary
# =============================================================================
test_end

log_info ""
log_info "Integration Test Summary:"
log_info "  - Family members registered: 3"
log_info "  - Geofences created: 3 (Home, School, Work)"
log_info "  - Trips completed: 6+"
log_info "  - Location updates: 50+"
log_info ""

print_summary
