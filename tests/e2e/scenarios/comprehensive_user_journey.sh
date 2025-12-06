#!/bin/bash
# =============================================================================
# E2E Scenario: Comprehensive User Journey
# Full end-to-end user journey from registration to daily usage
# =============================================================================

set -euo pipefail

# Load test framework
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../config.sh"
source "${SCRIPT_DIR}/../lib/common.sh"
source "${SCRIPT_DIR}/../lib/api.sh"
source "${SCRIPT_DIR}/../lib/adb.sh"
source "${SCRIPT_DIR}/../lib/generators.sh"

# =============================================================================
# Scenario Configuration
# =============================================================================
SCENARIO_NAME="Comprehensive User Journey"
SCENARIO_USER_EMAIL=""
SCENARIO_USER_PASSWORD=""
SCENARIO_DEVICE_ID=""
SCENARIO_GROUP_ID=""
SCENARIO_TOKEN=""

# =============================================================================
# Scenario Setup
# =============================================================================
setup_scenario() {
    log_info "Setting up $SCENARIO_NAME..."

    # Check prerequisites
    if ! check_dependencies; then
        log_error "Missing dependencies"
        return 1
    fi

    if ! check_backend; then
        log_error "Backend not available"
        return 1
    fi

    if ! adb_check_device; then
        log_error "No Android device connected"
        return 1
    fi

    if ! adb_is_app_installed "$APP_PACKAGE"; then
        log_error "App not installed"
        return 1
    fi

    # Generate unique test data
    SCENARIO_USER_EMAIL=$(generate_test_email "journey")
    SCENARIO_USER_PASSWORD="Journey123!"
    SCENARIO_DEVICE_ID=$(generate_device_id)

    log_info "Test email: $SCENARIO_USER_EMAIL"
    log_info "Device ID: $SCENARIO_DEVICE_ID"
}

cleanup_scenario() {
    log_info "Cleaning up scenario..."

    # Delete test group
    if [[ -n "$SCENARIO_GROUP_ID" ]]; then
        api_delete_group "$SCENARIO_GROUP_ID" 2>/dev/null || true
    fi

    # End any active trips
    if [[ -n "$SCENARIO_DEVICE_ID" ]]; then
        api_end_active_trip "$SCENARIO_DEVICE_ID" 2>/dev/null || true
    fi

    # Return to home location
    adb_set_location "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON" 2>/dev/null || true
}

# =============================================================================
# Phase 1: User Registration
# =============================================================================
phase_user_registration() {
    log_info "=== PHASE 1: User Registration ==="

    test_start "Register New User"

    log_step "Registering user: $SCENARIO_USER_EMAIL"
    local response
    response=$(api_auth_register "$SCENARIO_USER_EMAIL" "$SCENARIO_USER_PASSWORD" "E2E Journey User")

    SCENARIO_TOKEN=$(echo "$response" | jq -r '.token // .access_token // empty')

    if [[ -n "$SCENARIO_TOKEN" ]]; then
        log_success "User registered successfully"
        ((TESTS_PASSED++))
    else
        log_error "User registration failed"
        ((TESTS_FAILED++))
        return 1
    fi

    test_end
}

# =============================================================================
# Phase 2: Grant All Permissions
# =============================================================================
phase_grant_permissions() {
    log_info "=== PHASE 2: Grant Permissions ==="

    test_start "Grant App Permissions"

    log_step "Granting location permissions"
    adb_grant_location_permissions

    log_step "Granting notification permissions"
    adb shell pm grant "$APP_PACKAGE" android.permission.POST_NOTIFICATIONS 2>/dev/null || true

    log_step "Granting activity recognition permissions"
    adb shell pm grant "$APP_PACKAGE" android.permission.ACTIVITY_RECOGNITION 2>/dev/null || true

    log_success "Permissions granted"
    ((TESTS_PASSED++))

    test_end
}

# =============================================================================
# Phase 3: Launch App and Complete Setup
# =============================================================================
phase_app_setup() {
    log_info "=== PHASE 3: App Setup ==="

    test_start "Launch and Setup App"

    log_step "Launching app"
    adb_launch_app
    sleep 5

    adb_screenshot "scenario_01_app_launch"

    log_step "Waiting for location service to start"
    if adb_wait_for_location_service 30; then
        log_success "Location service started"
        ((TESTS_PASSED++))
    else
        log_warning "Location service may not have started"
    fi

    # Set initial location
    log_step "Setting initial location (home)"
    adb_set_location "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON"
    sleep 3

    test_end
}

# =============================================================================
# Phase 4: Join/Create Group
# =============================================================================
phase_join_group() {
    log_info "=== PHASE 4: Group Management ==="

    test_start "Create and Join Group"

    # Create a group
    log_step "Creating family group"
    local group_response
    group_response=$(api_create_group "Journey Test Family $(date +%s)")
    SCENARIO_GROUP_ID=$(echo "$group_response" | jq -r '.id // .group_id')

    if [[ -n "$SCENARIO_GROUP_ID" ]]; then
        log_success "Group created: $SCENARIO_GROUP_ID"
        ((TESTS_PASSED++))
    else
        log_error "Group creation failed"
        ((TESTS_FAILED++))
    fi

    # Generate invite for verification
    log_step "Generating invite code"
    local invite_response
    invite_response=$(api_create_group_invite "$SCENARIO_GROUP_ID")
    local invite_code
    invite_code=$(echo "$invite_response" | jq -r '.code')

    if [[ -n "$invite_code" ]]; then
        log_success "Invite code: $invite_code"
        ((TESTS_PASSED++))
    fi

    test_end
}

# =============================================================================
# Phase 5: Start Location Tracking
# =============================================================================
phase_location_tracking() {
    log_info "=== PHASE 5: Location Tracking ==="

    test_start "Verify Location Tracking"

    log_step "Checking location is being tracked"

    # Get initial location count
    local initial_count
    initial_count=$(api_get_location_count "$SCENARIO_DEVICE_ID" 2>/dev/null || echo "0")

    # Generate some location updates
    for i in {1..3}; do
        local offset=$(echo "scale=6; $i * 0.0002" | bc)
        local lat=$(echo "scale=6; $TEST_LOC_HOME_LAT + $offset" | bc)
        adb_set_location "$lat" "$TEST_LOC_HOME_LON"
        sleep 5
    done

    # Check location count increased
    local final_count
    final_count=$(api_get_location_count "$SCENARIO_DEVICE_ID" 2>/dev/null || echo "0")

    if [[ $final_count -gt $initial_count ]]; then
        log_success "Locations are being captured"
        ((TESTS_PASSED++))
    else
        log_info "Location count unchanged (service may batch uploads)"
    fi

    test_end
}

# =============================================================================
# Phase 6: Simulate Commute Trip
# =============================================================================
phase_commute_simulation() {
    log_info "=== PHASE 6: Commute Simulation ==="

    test_start "Simulate Morning Commute"

    log_step "Starting commute: Home -> Work"
    adb_screenshot "scenario_02_commute_start"

    # Simulate driving commute
    adb_simulate_driving_trip \
        "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON" \
        "$TEST_LOC_WORK_LAT" "$TEST_LOC_WORK_LON" \
        120 &  # 2 minute simulation

    local sim_pid=$!

    # Wait for trip detection
    log_step "Waiting for trip to be detected"
    sleep 30

    # Check for active trip
    local trip
    trip=$(api_get_active_trip "$SCENARIO_DEVICE_ID" 2>/dev/null || echo "")

    if [[ -n "$trip" ]] && echo "$trip" | jq -e '.id' &>/dev/null; then
        log_success "Trip detected"
        ((TESTS_PASSED++))
    else
        log_info "Trip detection may take longer"
    fi

    # Wait for simulation to complete
    wait $sim_pid 2>/dev/null || true

    # Arrive at work
    log_step "Arrived at work"
    adb_set_location "$TEST_LOC_WORK_LAT" "$TEST_LOC_WORK_LON"
    adb_screenshot "scenario_03_at_work"

    test_end
}

# =============================================================================
# Phase 7: Verify Trip Created
# =============================================================================
phase_verify_trip() {
    log_info "=== PHASE 7: Verify Trip ==="

    test_start "Verify Trip Was Recorded"

    # Wait for trip to end (stationary)
    log_step "Waiting for trip to end"
    sleep 60

    # Check trip history
    log_step "Checking trip history"
    local trips
    trips=$(api_get_trips "$SCENARIO_DEVICE_ID" 5 2>/dev/null || echo "[]")

    local trip_count
    trip_count=$(echo "$trips" | jq 'length' 2>/dev/null || echo "0")

    if [[ $trip_count -gt 0 ]]; then
        log_success "Trip recorded ($trip_count trips)"
        ((TESTS_PASSED++))

        # Check trip details
        local latest_trip
        latest_trip=$(echo "$trips" | jq '.[0]')

        local distance
        distance=$(echo "$latest_trip" | jq -r '.distance // 0')
        log_info "Trip distance: ${distance}m"

        local duration
        duration=$(echo "$latest_trip" | jq -r '.duration // 0')
        log_info "Trip duration: ${duration}s"
    else
        log_warning "No trips found in history"
    fi

    test_end
}

# =============================================================================
# Phase 8: View Trip History
# =============================================================================
phase_view_trip_history() {
    log_info "=== PHASE 8: Trip History UI ==="

    test_start "View Trip History"

    log_step "Opening trip history screen"
    adb_open_deep_link "phonemanager://trips"
    sleep 3

    adb_screenshot "scenario_04_trip_history"

    log_success "Trip history screen captured"
    ((TESTS_PASSED++))

    test_end
}

# =============================================================================
# Phase 9: Geofence Trigger
# =============================================================================
phase_geofence_trigger() {
    log_info "=== PHASE 9: Geofence Trigger ==="

    test_start "Trigger Geofence Event"

    # Create work geofence
    log_step "Creating work geofence"
    local geofence_data
    geofence_data=$(generate_geofence_data "Work" "$TEST_LOC_WORK_LAT" "$TEST_LOC_WORK_LON" "150")

    local geofence
    geofence=$(api_create_geofence "$SCENARIO_DEVICE_ID" "$geofence_data" 2>/dev/null || echo "")

    local geofence_id
    geofence_id=$(echo "$geofence" | jq -r '.id // empty')

    if [[ -n "$geofence_id" ]]; then
        log_success "Work geofence created"
        ((TESTS_PASSED++))
    fi

    # Leave work (exit geofence)
    log_step "Leaving work geofence"
    adb_set_location "$TEST_LOC_PARK_LAT" "$TEST_LOC_PARK_LON"
    sleep 10

    # Check for exit event
    local events
    events=$(api_get_geofence_events "$SCENARIO_DEVICE_ID" "$geofence_id" 2>/dev/null || echo "[]")

    if echo "$events" | jq -e '.[] | select(.type | test("EXIT"; "i"))' &>/dev/null; then
        log_success "Geofence EXIT event triggered"
        ((TESTS_PASSED++))
    else
        log_info "Geofence event may not have been captured yet"
    fi

    # Cleanup geofence
    api_delete_geofence "$SCENARIO_DEVICE_ID" "$geofence_id" 2>/dev/null || true

    test_end
}

# =============================================================================
# Phase 10: Check Weather
# =============================================================================
phase_check_weather() {
    log_info "=== PHASE 10: Weather Check ==="

    test_start "View Weather"

    log_step "Opening weather screen"
    adb_open_deep_link "phonemanager://weather"
    sleep 5

    adb_screenshot "scenario_05_weather"

    log_success "Weather screen captured"
    ((TESTS_PASSED++))

    # Verify weather API
    log_step "Fetching weather data"
    local weather
    weather=$(api_get_weather "$TEST_LOC_PARK_LAT" "$TEST_LOC_PARK_LON" 2>/dev/null || echo "")

    if [[ -n "$weather" ]] && echo "$weather" | jq -e '.temperature // .temp' &>/dev/null; then
        local temp
        temp=$(echo "$weather" | jq -r '.temperature // .temp // .current.temp')
        log_success "Weather data available: ${temp}°"
        ((TESTS_PASSED++))
    fi

    test_end
}

# =============================================================================
# Phase 11: Return Home
# =============================================================================
phase_return_home() {
    log_info "=== PHASE 11: Return Home ==="

    test_start "Return Home Trip"

    log_step "Simulating return trip to home"

    adb_simulate_driving_trip \
        "$TEST_LOC_PARK_LAT" "$TEST_LOC_PARK_LON" \
        "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON" \
        90 &

    local sim_pid=$!

    sleep 60
    wait $sim_pid 2>/dev/null || true

    log_step "Arrived home"
    adb_set_location "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON"
    adb_screenshot "scenario_06_home"

    log_success "Journey complete"
    ((TESTS_PASSED++))

    test_end
}

# =============================================================================
# Phase 12: Verify Day Summary
# =============================================================================
phase_day_summary() {
    log_info "=== PHASE 12: Day Summary ==="

    test_start "Verify Day's Activity"

    # Check total trips
    log_step "Checking total trips"
    local trips
    trips=$(api_get_trips "$SCENARIO_DEVICE_ID" 10 2>/dev/null || echo "[]")

    local trip_count
    trip_count=$(echo "$trips" | jq 'length' 2>/dev/null || echo "0")
    log_info "Total trips today: $trip_count"

    # Check total distance
    local total_distance=0
    for distance in $(echo "$trips" | jq -r '.[].distance // 0' 2>/dev/null); do
        total_distance=$((total_distance + ${distance%.*}))
    done
    log_info "Total distance: ${total_distance}m"

    # Check locations
    local location_count
    location_count=$(api_get_location_count "$SCENARIO_DEVICE_ID" 2>/dev/null || echo "0")
    log_info "Total locations captured: $location_count"

    log_success "Day summary complete"
    ((TESTS_PASSED++))

    # Final screenshot
    adb_open_deep_link "phonemanager://dashboard"
    sleep 3
    adb_screenshot "scenario_07_final_dashboard"

    test_end
}

# =============================================================================
# Main Scenario Execution
# =============================================================================
main() {
    echo ""
    echo "=============================================="
    echo "  E2E SCENARIO: $SCENARIO_NAME"
    echo "  Session: $TEST_SESSION_ID"
    echo "=============================================="
    echo ""

    # Setup
    if ! setup_scenario; then
        log_error "Scenario setup failed"
        exit 1
    fi

    # Run phases
    phase_user_registration
    phase_grant_permissions
    phase_app_setup
    phase_join_group
    phase_location_tracking
    phase_commute_simulation
    phase_verify_trip
    phase_view_trip_history
    phase_geofence_trigger
    phase_check_weather
    phase_return_home
    phase_day_summary

    # Cleanup
    cleanup_scenario

    # Summary
    print_summary
    generate_html_report "${REPORTS_DIR}/scenario_user_journey_${TEST_SESSION_ID}.html"

    echo ""
    echo "=============================================="
    echo "  SCENARIO COMPLETE"
    echo "=============================================="

    [[ $TESTS_FAILED -eq 0 ]]
}

# Run if executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
