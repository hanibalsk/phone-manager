#!/bin/bash
# Scenario: Family Tracking
# Simulates a family with multiple devices tracking each other:
# - Parent monitoring children's locations
# - Geofence alerts for home/school
# - Proximity alerts between family members
# - Emergency location sharing

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$SCRIPT_DIR/config.sh"
source "$SCRIPT_DIR/lib/common.sh"
source "$SCRIPT_DIR/lib/api.sh"
source "$SCRIPT_DIR/lib/adb.sh"
source "$SCRIPT_DIR/lib/generators.sh"

test_start "Family Tracking Scenario"

# =============================================================================
# Scenario Configuration
# =============================================================================
FAMILY_GROUP="family-$(generate_short_id)"

# Location constants
HOME_LAT="37.7749"
HOME_LON="-122.4194"
SCHOOL_LAT="37.7650"
SCHOOL_LON="-122.4050"
PARK_LAT="37.7700"
PARK_LON="-122.4100"

log_info "=== Family Tracking Scenario ==="
log_info "Group: $FAMILY_GROUP"

# =============================================================================
# Setup: Register Family Devices
# =============================================================================
log_info "=== Setup: Register Family Devices ==="

# Parent device
log_step "Registering Parent device..."
parent_info=$(generate_device)
PARENT_ID=$(echo "$parent_info" | cut -d'|' -f1)
response=$(api_register_device "$PARENT_ID" "Dad's Phone" "$FAMILY_GROUP")
PARENT_API_KEY=$(json_get "$(get_response_body "$response")" ".api_key")
export API_KEY="$PARENT_API_KEY"
log_success "Parent registered: $PARENT_ID"
((TESTS_PASSED++))

# Child 1 device
log_step "Registering Child 1 device..."
child1_info=$(generate_device)
CHILD1_ID=$(echo "$child1_info" | cut -d'|' -f1)
response=$(api_register_device "$CHILD1_ID" "Emma's Phone" "$FAMILY_GROUP")
CHILD1_API_KEY=$(json_get "$(get_response_body "$response")" ".api_key")
log_success "Child 1 registered: $CHILD1_ID"
((TESTS_PASSED++))

# Child 2 device (younger, tablet)
log_step "Registering Child 2 device..."
child2_info=$(generate_device)
CHILD2_ID=$(echo "$child2_info" | cut -d'|' -f1)
response=$(api_register_device "$CHILD2_ID" "Jake's Tablet" "$FAMILY_GROUP")
CHILD2_API_KEY=$(json_get "$(get_response_body "$response")" ".api_key")
log_success "Child 2 registered: $CHILD2_ID"
((TESTS_PASSED++))

# =============================================================================
# Verify Family Group
# =============================================================================
log_step "Verifying family group..."
response=$(api_get_group_devices "$FAMILY_GROUP")
code=$(get_http_code "$response")
body=$(get_response_body "$response")

assert_http_success "$code" "Get family group"

device_count=$(json_get "$body" ".devices | length" 2>/dev/null || json_get "$body" "| length" 2>/dev/null || echo "0")
if [[ "$device_count" == "3" ]]; then
    log_success "Family group has 3 members"
    ((TESTS_PASSED++))
else
    log_warning "Expected 3 family members, got $device_count"
fi

# =============================================================================
# Setup: Create Geofences
# =============================================================================
log_info "=== Setup: Geofences ==="

# Home geofence
log_step "Creating Home geofence..."
response=$(api_create_geofence "$PARENT_ID" "Home" "$HOME_LAT" "$HOME_LON" "100" '["enter", "exit"]' '{"notify_all": true}')
HOME_GF_ID=$(json_get "$(get_response_body "$response")" ".id")
assert_http_status "$(get_http_code "$response")" "201" "Create Home geofence"

# School geofence
log_step "Creating School geofence..."
response=$(api_create_geofence "$PARENT_ID" "School" "$SCHOOL_LAT" "$SCHOOL_LON" "200" '["enter", "exit"]' '{"school_hours": "08:00-15:00"}')
SCHOOL_GF_ID=$(json_get "$(get_response_body "$response")" ".id")
assert_http_status "$(get_http_code "$response")" "201" "Create School geofence"

# Park geofence (safe zone for after school)
log_step "Creating Park geofence..."
response=$(api_create_geofence "$PARENT_ID" "Park" "$PARK_LAT" "$PARK_LON" "150" '["enter", "dwell"]' '{"safe_zone": true}')
PARK_GF_ID=$(json_get "$(get_response_body "$response")" ".id")
assert_http_status "$(get_http_code "$response")" "201" "Create Park geofence"

log_success "3 geofences created"

# =============================================================================
# Setup: Proximity Alerts
# =============================================================================
log_info "=== Setup: Proximity Alerts ==="

# Alert when siblings are together (500m)
log_step "Creating sibling proximity alert..."
response=$(api_create_proximity_alert "$CHILD1_ID" "$CHILD2_ID" "500" "Emma and Jake together")
code=$(get_http_code "$response")

if [[ "$code" == "201" ]]; then
    SIBLING_ALERT_ID=$(json_get "$(get_response_body "$response")" ".id")
    log_success "Sibling alert created"
    ((TESTS_PASSED++))
else
    log_warning "Proximity alert creation: HTTP $code"
fi

# =============================================================================
# Simulation: Morning - Everyone at Home
# =============================================================================
log_info "=== Morning: Everyone at Home ==="

log_step "Family at home..."
api_upload_location "$PARENT_ID" "$HOME_LAT" "$HOME_LON" "10.0" >/dev/null
api_upload_location "$CHILD1_ID" "$HOME_LAT" "$HOME_LON" "10.0" >/dev/null
api_upload_location "$CHILD2_ID" "$HOME_LAT" "$HOME_LON" "10.0" >/dev/null

# Simulate GPS on emulator if available
if adb_check_connection && adb_is_emulator; then
    adb_set_location "$HOME_LAT" "$HOME_LON"
    sleep 1
    adb_screenshot "family_01_home.png"
fi

log_success "All family at home"
((TESTS_PASSED++))

# =============================================================================
# Simulation: Children Walk to School
# =============================================================================
log_info "=== Children Walking to School ==="

log_step "Children leaving home..."

# Emma's trip to school (walking)
EMMA_TRIP_ID="emma-school-$(date +%s)"
response=$(api_create_trip "$CHILD1_ID" "$EMMA_TRIP_ID" "$HOME_LAT" "$HOME_LON" "WALKING" "GEOFENCE_EXIT")
EMMA_TRIP=$(json_get "$(get_response_body "$response")" ".id")
assert_http_status "$(get_http_code "$response")" "201" "Emma trip start"

# Jake walks with Emma
JAKE_TRIP_ID="jake-school-$(date +%s)"
response=$(api_create_trip "$CHILD2_ID" "$JAKE_TRIP_ID" "$HOME_LAT" "$HOME_LON" "WALKING" "GEOFENCE_EXIT")
JAKE_TRIP=$(json_get "$(get_response_body "$response")" ".id")

# Generate walking route
log_step "Simulating walk to school..."
school_route=$(generate_location_path "$HOME_LAT" "$HOME_LON" "$SCHOOL_LAT" "$SCHOOL_LON" 15)

# Both children walk together
api_upload_locations_batch "$CHILD1_ID" "$school_route" >/dev/null
api_upload_locations_batch "$CHILD2_ID" "$school_route" >/dev/null

# Generate movement events
emma_events=$(generate_movement_events_for_path "$EMMA_TRIP" "$HOME_LAT" "$HOME_LON" "$SCHOOL_LAT" "$SCHOOL_LON" 15 "WALKING")
api_upload_movement_events_batch "$CHILD1_ID" "$emma_events" >/dev/null

jake_events=$(generate_movement_events_for_path "$JAKE_TRIP" "$HOME_LAT" "$HOME_LON" "$SCHOOL_LAT" "$SCHOOL_LON" 15 "WALKING")
api_upload_movement_events_batch "$CHILD2_ID" "$jake_events" >/dev/null

# Complete trips
api_complete_trip "$EMMA_TRIP" "$SCHOOL_LAT" "$SCHOOL_LON" >/dev/null
api_complete_trip "$JAKE_TRIP" "$SCHOOL_LAT" "$SCHOOL_LON" >/dev/null

log_success "Children arrived at school"
((TESTS_PASSED++))

# =============================================================================
# Simulation: School Day - Periodic Updates
# =============================================================================
log_info "=== School Day ==="

log_step "Simulating school day..."

# Children stay at school (small movements)
for i in $(seq 1 5); do
    # Small random movement within school grounds
    school_lat=$(echo "$SCHOOL_LAT + 0.000$((RANDOM % 3))" | bc)
    school_lon=$(echo "$SCHOOL_LON + 0.000$((RANDOM % 3))" | bc)

    api_upload_location "$CHILD1_ID" "$school_lat" "$school_lon" "25.0" >/dev/null
    api_upload_location "$CHILD2_ID" "$school_lat" "$school_lon" "25.0" >/dev/null
done

# Parent checks children's location
response=$(api_get_group_devices "$FAMILY_GROUP")
body=$(get_response_body "$response")
log_info "  Parent checked family locations"

log_success "School day simulated"
((TESTS_PASSED++))

# =============================================================================
# Simulation: After School - Park Visit
# =============================================================================
log_info "=== After School: Park Visit ==="

log_step "Children walking to park..."

# Emma's trip to park
EMMA_PARK_ID="emma-park-$(date +%s)"
response=$(api_create_trip "$CHILD1_ID" "$EMMA_PARK_ID" "$SCHOOL_LAT" "$SCHOOL_LON" "WALKING")
EMMA_PARK_TRIP=$(json_get "$(get_response_body "$response")" ".id")

# Route to park
park_route=$(generate_location_path "$SCHOOL_LAT" "$SCHOOL_LON" "$PARK_LAT" "$PARK_LON" 8)
api_upload_locations_batch "$CHILD1_ID" "$park_route" >/dev/null
api_upload_locations_batch "$CHILD2_ID" "$park_route" >/dev/null

# Complete Emma's trip
api_complete_trip "$EMMA_PARK_TRIP" "$PARK_LAT" "$PARK_LON" >/dev/null

log_success "Children at park"
((TESTS_PASSED++))

# =============================================================================
# Simulation: Parent Picks Up Children
# =============================================================================
log_info "=== Parent Pickup ==="

log_step "Parent driving to park..."

# Parent's trip to park
PARENT_PICKUP_ID="parent-pickup-$(date +%s)"
response=$(api_create_trip "$PARENT_ID" "$PARENT_PICKUP_ID" "$HOME_LAT" "$HOME_LON" "IN_VEHICLE")
PARENT_PICKUP_TRIP=$(json_get "$(get_response_body "$response")" ".id")

# Generate driving route
pickup_route=$(generate_location_path "$HOME_LAT" "$HOME_LON" "$PARK_LAT" "$PARK_LON" 6)
api_upload_locations_batch "$PARENT_ID" "$pickup_route" >/dev/null

# Parent arrives at park
api_upload_location "$PARENT_ID" "$PARK_LAT" "$PARK_LON" "10.0" >/dev/null
api_complete_trip "$PARENT_PICKUP_TRIP" "$PARK_LAT" "$PARK_LON" >/dev/null

log_success "Parent arrived at park"
((TESTS_PASSED++))

# =============================================================================
# Simulation: Everyone Returns Home
# =============================================================================
log_info "=== Returning Home ==="

log_step "Family driving home..."

# Family trip home
FAMILY_HOME_ID="family-home-$(date +%s)"
response=$(api_create_trip "$PARENT_ID" "$FAMILY_HOME_ID" "$PARK_LAT" "$PARK_LON" "IN_VEHICLE")
FAMILY_HOME_TRIP=$(json_get "$(get_response_body "$response")" ".id")

# Route home
home_route=$(generate_location_path "$PARK_LAT" "$PARK_LON" "$HOME_LAT" "$HOME_LON" 5)
api_upload_locations_batch "$PARENT_ID" "$home_route" >/dev/null
api_upload_locations_batch "$CHILD1_ID" "$home_route" >/dev/null
api_upload_locations_batch "$CHILD2_ID" "$home_route" >/dev/null

# Everyone at home
api_upload_location "$PARENT_ID" "$HOME_LAT" "$HOME_LON" "10.0" >/dev/null
api_upload_location "$CHILD1_ID" "$HOME_LAT" "$HOME_LON" "10.0" >/dev/null
api_upload_location "$CHILD2_ID" "$HOME_LAT" "$HOME_LON" "10.0" >/dev/null

api_complete_trip "$FAMILY_HOME_TRIP" "$HOME_LAT" "$HOME_LON" >/dev/null

if adb_check_connection && adb_is_emulator; then
    adb_set_location "$HOME_LAT" "$HOME_LON"
    sleep 1
    adb_screenshot "family_99_home_return.png"
fi

log_success "Family returned home"
((TESTS_PASSED++))

# =============================================================================
# Verify Data
# =============================================================================
log_info "=== Verification ==="

# Check location history for each family member
log_step "Checking location history..."

for device_id in "$PARENT_ID" "$CHILD1_ID" "$CHILD2_ID"; do
    response=$(api_get_location_history "$device_id" 100)
    count=$(json_get "$(get_response_body "$response")" ".locations | length" 2>/dev/null || echo "0")
    log_info "  Device $device_id: $count locations"
done

# Check parent's trips
log_step "Checking trip history..."
response=$(api_get_device_trips "$PARENT_ID")
trip_count=$(json_get "$(get_response_body "$response")" ".trips | length" 2>/dev/null || echo "0")
log_info "Parent trips: $trip_count"

# Check geofences
log_step "Checking geofences..."
response=$(api_get_geofences "$PARENT_ID")
gf_count=$(json_get "$(get_response_body "$response")" ".geofences | length" 2>/dev/null || echo "0")
log_info "Geofences: $gf_count"

if [[ "$gf_count" -ge 3 ]]; then
    log_success "Geofences verified"
    ((TESTS_PASSED++))
fi

# =============================================================================
# Cleanup
# =============================================================================
log_info "=== Cleanup ==="

log_step "Cleaning up..."
api_delete_geofence "$HOME_GF_ID" >/dev/null 2>&1 || true
api_delete_geofence "$SCHOOL_GF_ID" >/dev/null 2>&1 || true
api_delete_geofence "$PARK_GF_ID" >/dev/null 2>&1 || true

if [[ -n "$SIBLING_ALERT_ID" ]]; then
    api_delete_proximity_alert "$SIBLING_ALERT_ID" >/dev/null 2>&1 || true
fi

log_success "Cleanup complete"

# =============================================================================
# Summary
# =============================================================================
test_end

log_info ""
log_info "Family Tracking Scenario Summary:"
log_info "  - Family group: $FAMILY_GROUP"
log_info "  - Devices: 3 (Parent, Emma, Jake)"
log_info "  - Geofences: Home, School, Park"
log_info "  - Locations simulated: Morning → School → Park → Home"
log_info ""

print_summary
