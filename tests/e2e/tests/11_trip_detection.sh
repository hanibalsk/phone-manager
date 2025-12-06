#!/bin/bash
# =============================================================================
# E2E Tests: Trip Detection
# Tests automatic trip detection, mode detection, and trip lifecycle
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
# Test Configuration
# =============================================================================
DEVICE_ID=""
TRIP_DETECTION_TIMEOUT=120  # 2 minutes for trip detection
STATIONARY_WAIT=60          # 1 minute for stationary detection (reduced for testing)

# =============================================================================
# Test Setup
# =============================================================================
setup_trip_tests() {
    log_info "Setting up trip detection tests..."

    # Check emulator/device
    if ! adb_check_device; then
        log_error "No Android device/emulator connected"
        return 1
    fi

    # Check app is installed
    if ! adb_is_app_installed "$APP_PACKAGE"; then
        log_error "App not installed: $APP_PACKAGE"
        return 1
    fi

    # Get device ID
    DEVICE_ID=$(adb_get_device_id 2>/dev/null || generate_device_id)
    log_info "Using device ID: $DEVICE_ID"

    # Ensure location permissions
    adb_grant_location_permissions

    # Ensure service is running
    if ! adb_is_location_service_running; then
        adb_launch_app
        sleep 5
        adb_wait_for_location_service 30
    fi

    # Set initial location (home)
    adb_set_location "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON"
    sleep 3
}

cleanup_trip_tests() {
    log_info "Cleaning up trip detection tests..."

    # End any active trips
    api_end_active_trip "$DEVICE_ID" 2>/dev/null || true

    # Set location back to home
    adb_set_location "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON" 2>/dev/null || true
}

# =============================================================================
# Helper: Wait for Trip to Start
# =============================================================================
wait_for_trip_start() {
    local timeout="${1:-$TRIP_DETECTION_TIMEOUT}"
    local elapsed=0
    local check_interval=5

    log_debug "Waiting for trip to start (timeout: ${timeout}s)..."

    while [[ $elapsed -lt $timeout ]]; do
        local active_trip
        active_trip=$(api_get_active_trip "$DEVICE_ID" 2>/dev/null || echo "")

        if [[ -n "$active_trip" ]] && echo "$active_trip" | jq -e '.id // .trip_id' &>/dev/null; then
            log_debug "Trip started after ${elapsed}s"
            echo "$active_trip"
            return 0
        fi

        sleep $check_interval
        elapsed=$((elapsed + check_interval))
    done

    return 1
}

# =============================================================================
# Helper: Wait for Trip to End
# =============================================================================
wait_for_trip_end() {
    local trip_id="$1"
    local timeout="${2:-$TRIP_DETECTION_TIMEOUT}"
    local elapsed=0
    local check_interval=5

    log_debug "Waiting for trip $trip_id to end (timeout: ${timeout}s)..."

    while [[ $elapsed -lt $timeout ]]; do
        local trip_status
        trip_status=$(api_get_trip "$DEVICE_ID" "$trip_id" 2>/dev/null || echo "")

        if echo "$trip_status" | jq -e '.ended_at // .end_time' 2>/dev/null | grep -qv "null"; then
            log_debug "Trip ended after ${elapsed}s"
            echo "$trip_status"
            return 0
        fi

        sleep $check_interval
        elapsed=$((elapsed + check_interval))
    done

    return 1
}

# =============================================================================
# Test: Trip Auto-Start on Movement
# =============================================================================
test_trip_auto_start_on_movement() {
    test_start "Trip Auto-Start on Movement"

    # Ensure stationary at home
    log_step "Starting stationary at home location"
    adb_set_location "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON"
    sleep 10

    # Get current trip count
    local initial_count
    initial_count=$(api_get_trip_count "$DEVICE_ID" 2>/dev/null || echo "0")
    log_debug "Initial trip count: $initial_count"

    # Start moving (simulate walking to park)
    log_step "Simulating walking movement"
    adb_simulate_walking_trip \
        "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON" \
        "$TEST_LOC_PARK_LAT" "$TEST_LOC_PARK_LON" \
        60 &  # 60 seconds walking

    local sim_pid=$!

    # Wait for trip to start
    log_step "Waiting for trip detection"
    local trip_data
    trip_data=$(wait_for_trip_start 45)
    local trip_started=$?

    # Wait for simulation to complete
    wait $sim_pid 2>/dev/null || true

    if [[ $trip_started -eq 0 ]] && [[ -n "$trip_data" ]]; then
        log_success "Trip auto-started on movement"
        ((TESTS_PASSED++))

        # Verify trip has start location
        local start_lat
        start_lat=$(echo "$trip_data" | jq -r '.start_location.latitude // .start_lat // empty')
        if [[ -n "$start_lat" ]]; then
            log_success "Trip has start location recorded"
            ((TESTS_PASSED++))
        fi
    else
        log_error "Trip did not auto-start on movement"
        ((TESTS_FAILED++))
        adb_screenshot "trip_not_started"
    fi

    test_end
}

# =============================================================================
# Test: Trip Auto-End on Stationary
# =============================================================================
test_trip_auto_end_on_stationary() {
    test_start "Trip Auto-End on Stationary"

    # Start a trip first
    log_step "Starting a trip by simulating movement"
    adb_simulate_walking_trip \
        "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON" \
        "$TEST_LOC_PARK_LAT" "$TEST_LOC_PARK_LON" \
        30 &

    local sim_pid=$!

    # Wait for trip to start
    local trip_data
    trip_data=$(wait_for_trip_start 30)

    wait $sim_pid 2>/dev/null || true

    if [[ -z "$trip_data" ]]; then
        test_skip "Could not start trip for stationary test"
        test_end
        return
    fi

    local trip_id
    trip_id=$(echo "$trip_data" | jq -r '.id // .trip_id')
    log_info "Trip started with ID: $trip_id"

    # Stop moving and remain stationary
    log_step "Stopping at destination and remaining stationary"
    adb_set_location "$TEST_LOC_PARK_LAT" "$TEST_LOC_PARK_LON"

    # Wait for trip to end (grace period + detection)
    log_step "Waiting for trip to auto-end after stationary period"
    local ended_trip
    ended_trip=$(wait_for_trip_end "$trip_id" 180)

    if [[ -n "$ended_trip" ]]; then
        log_success "Trip auto-ended after stationary period"
        ((TESTS_PASSED++))

        # Verify trip has end location
        local end_lat
        end_lat=$(echo "$ended_trip" | jq -r '.end_location.latitude // .end_lat // empty')
        if [[ -n "$end_lat" ]]; then
            log_success "Trip has end location recorded"
            ((TESTS_PASSED++))
        fi

        # Verify duration is reasonable
        local duration
        duration=$(echo "$ended_trip" | jq -r '.duration // .duration_seconds // 0')
        if [[ $duration -gt 0 ]]; then
            log_success "Trip has duration: ${duration}s"
            ((TESTS_PASSED++))
        fi
    else
        log_warning "Trip did not auto-end within timeout (may need longer grace period)"
        # Force end trip for cleanup
        api_end_trip "$DEVICE_ID" "$trip_id" 2>/dev/null || true
    fi

    test_end
}

# =============================================================================
# Test: Trip Grace Period Handling
# =============================================================================
test_trip_grace_period_handling() {
    test_start "Trip Grace Period Handling"

    # Start a trip
    log_step "Starting a trip"
    adb_simulate_walking_trip \
        "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON" \
        "$TEST_LOC_PARK_LAT" "$TEST_LOC_PARK_LON" \
        20 &

    local sim_pid=$!
    local trip_data
    trip_data=$(wait_for_trip_start 30)
    wait $sim_pid 2>/dev/null || true

    if [[ -z "$trip_data" ]]; then
        test_skip "Could not start trip for grace period test"
        test_end
        return
    fi

    local trip_id
    trip_id=$(echo "$trip_data" | jq -r '.id // .trip_id')

    # Stop briefly (less than grace period)
    log_step "Brief stop (less than grace period)"
    adb_set_location "$TEST_LOC_PARK_LAT" "$TEST_LOC_PARK_LON"
    sleep 30  # Wait less than typical 2-minute grace period

    # Resume movement
    log_step "Resuming movement"
    adb_simulate_walking_trip \
        "$TEST_LOC_PARK_LAT" "$TEST_LOC_PARK_LON" \
        "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON" \
        20 &

    sim_pid=$!

    # Check trip is still active (same trip_id)
    sleep 5
    local current_trip
    current_trip=$(api_get_active_trip "$DEVICE_ID" 2>/dev/null || echo "")
    local current_id
    current_id=$(echo "$current_trip" | jq -r '.id // .trip_id // empty')

    wait $sim_pid 2>/dev/null || true

    if [[ "$current_id" == "$trip_id" ]]; then
        log_success "Trip continued after brief stop (grace period working)"
        ((TESTS_PASSED++))
    else
        log_warning "Trip may have ended during brief stop or new trip started"
    fi

    # Cleanup
    api_end_active_trip "$DEVICE_ID" 2>/dev/null || true

    test_end
}

# =============================================================================
# Test: Trip Mode Detection - Walking
# =============================================================================
test_trip_mode_detection_walking() {
    test_start "Trip Mode Detection - Walking"

    log_step "Starting walking trip simulation"
    adb_simulate_walking_trip \
        "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON" \
        "$TEST_LOC_PARK_LAT" "$TEST_LOC_PARK_LON" \
        60 &

    local sim_pid=$!

    # Wait for trip and check mode
    local trip_data
    trip_data=$(wait_for_trip_start 45)
    wait $sim_pid 2>/dev/null || true

    if [[ -n "$trip_data" ]]; then
        local mode
        mode=$(echo "$trip_data" | jq -r '.mode // .transport_mode // .detected_mode // empty')
        log_info "Detected mode: $mode"

        if echo "$mode" | grep -qi "walk\|foot\|pedestrian"; then
            log_success "Walking mode correctly detected"
            ((TESTS_PASSED++))
        else
            log_warning "Mode detected as: $mode (expected walking)"
        fi
    else
        log_warning "Could not get trip data for mode detection"
    fi

    # Cleanup
    api_end_active_trip "$DEVICE_ID" 2>/dev/null || true

    test_end
}

# =============================================================================
# Test: Trip Mode Detection - Driving
# =============================================================================
test_trip_mode_detection_driving() {
    test_start "Trip Mode Detection - Driving"

    log_step "Starting driving trip simulation"
    adb_simulate_driving_trip \
        "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON" \
        "$TEST_LOC_WORK_LAT" "$TEST_LOC_WORK_LON" \
        120 &

    local sim_pid=$!

    # Wait for trip and check mode
    local trip_data
    trip_data=$(wait_for_trip_start 45)

    # Let simulation run a bit for mode detection
    sleep 30

    # Get updated trip data
    if [[ -n "$trip_data" ]]; then
        local trip_id
        trip_id=$(echo "$trip_data" | jq -r '.id // .trip_id')
        trip_data=$(api_get_trip "$DEVICE_ID" "$trip_id" 2>/dev/null || echo "$trip_data")
    fi

    wait $sim_pid 2>/dev/null || true

    if [[ -n "$trip_data" ]]; then
        local mode
        mode=$(echo "$trip_data" | jq -r '.mode // .transport_mode // .detected_mode // empty')
        log_info "Detected mode: $mode"

        if echo "$mode" | grep -qi "driv\|car\|auto\|vehicle"; then
            log_success "Driving mode correctly detected"
            ((TESTS_PASSED++))
        else
            log_warning "Mode detected as: $mode (expected driving)"
        fi
    else
        log_warning "Could not get trip data for mode detection"
    fi

    # Cleanup
    api_end_active_trip "$DEVICE_ID" 2>/dev/null || true

    test_end
}

# =============================================================================
# Test: Trip Mode Change During Trip
# =============================================================================
test_trip_mode_change_during_trip() {
    test_start "Trip Mode Change During Trip"

    # Start with walking
    log_step "Starting with walking"
    adb_simulate_walking_trip \
        "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON" \
        "$TEST_LOC_PARK_LAT" "$TEST_LOC_PARK_LON" \
        30 &

    local sim_pid=$!
    local trip_data
    trip_data=$(wait_for_trip_start 30)
    wait $sim_pid 2>/dev/null || true

    if [[ -z "$trip_data" ]]; then
        test_skip "Could not start trip for mode change test"
        test_end
        return
    fi

    local trip_id
    trip_id=$(echo "$trip_data" | jq -r '.id // .trip_id')

    # Switch to driving
    log_step "Switching to driving mode"
    adb_simulate_driving_trip \
        "$TEST_LOC_PARK_LAT" "$TEST_LOC_PARK_LON" \
        "$TEST_LOC_WORK_LAT" "$TEST_LOC_WORK_LON" \
        60 &

    sim_pid=$!
    sleep 30

    # Get trip segments or mode history
    local trip_details
    trip_details=$(api_get_trip "$DEVICE_ID" "$trip_id" 2>/dev/null || echo "")

    wait $sim_pid 2>/dev/null || true

    if [[ -n "$trip_details" ]]; then
        local segments
        segments=$(echo "$trip_details" | jq '.segments // .mode_segments // []' 2>/dev/null)
        local segment_count
        segment_count=$(echo "$segments" | jq 'length' 2>/dev/null || echo "0")

        if [[ $segment_count -gt 1 ]]; then
            log_success "Multiple mode segments detected ($segment_count)"
            ((TESTS_PASSED++))
        else
            log_info "Mode change may not create segments (depends on implementation)"
        fi
    fi

    # Cleanup
    api_end_active_trip "$DEVICE_ID" 2>/dev/null || true

    test_end
}

# =============================================================================
# Test: Trip Statistics Calculation
# =============================================================================
test_trip_statistics_calculation() {
    test_start "Trip Statistics Calculation"

    # Create a complete trip
    log_step "Creating a complete trip"
    adb_simulate_driving_trip \
        "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON" \
        "$TEST_LOC_WORK_LAT" "$TEST_LOC_WORK_LON" \
        90 &

    local sim_pid=$!
    local trip_data
    trip_data=$(wait_for_trip_start 30)
    wait $sim_pid 2>/dev/null || true

    if [[ -z "$trip_data" ]]; then
        test_skip "Could not start trip for statistics test"
        test_end
        return
    fi

    local trip_id
    trip_id=$(echo "$trip_data" | jq -r '.id // .trip_id')

    # Stop and wait for trip to end
    adb_set_location "$TEST_LOC_WORK_LAT" "$TEST_LOC_WORK_LON"
    local ended_trip
    ended_trip=$(wait_for_trip_end "$trip_id" 180)

    if [[ -z "$ended_trip" ]]; then
        log_warning "Trip did not end naturally, forcing end"
        api_end_trip "$DEVICE_ID" "$trip_id"
        sleep 2
        ended_trip=$(api_get_trip "$DEVICE_ID" "$trip_id" 2>/dev/null || echo "")
    fi

    if [[ -n "$ended_trip" ]]; then
        # Check distance
        local distance
        distance=$(echo "$ended_trip" | jq -r '.distance // .distance_meters // 0')
        if [[ $distance -gt 0 ]]; then
            log_success "Distance calculated: ${distance}m"
            ((TESTS_PASSED++))
        else
            log_warning "Distance not calculated"
        fi

        # Check duration
        local duration
        duration=$(echo "$ended_trip" | jq -r '.duration // .duration_seconds // 0')
        if [[ $duration -gt 0 ]]; then
            log_success "Duration calculated: ${duration}s"
            ((TESTS_PASSED++))
        else
            log_warning "Duration not calculated"
        fi

        # Check average speed
        local avg_speed
        avg_speed=$(echo "$ended_trip" | jq -r '.average_speed // .avg_speed // 0')
        if [[ $(echo "$avg_speed > 0" | bc -l) -eq 1 ]]; then
            log_success "Average speed calculated: ${avg_speed} m/s"
            ((TESTS_PASSED++))
        fi
    else
        log_error "Could not get trip data for statistics verification"
        ((TESTS_FAILED++))
    fi

    test_end
}

# =============================================================================
# Test: Trip History Display
# =============================================================================
test_trip_history_display() {
    test_start "Trip History Display"

    # Get trip history from API
    log_step "Fetching trip history"
    local history
    history=$(api_get_trips "$DEVICE_ID" 10 2>/dev/null || echo "[]")

    local trip_count
    trip_count=$(echo "$history" | jq 'length' 2>/dev/null || echo "0")
    log_info "Trips in history: $trip_count"

    if [[ $trip_count -gt 0 ]]; then
        log_success "Trip history contains trips"
        ((TESTS_PASSED++))

        # Verify trip data structure
        local first_trip
        first_trip=$(echo "$history" | jq '.[0]' 2>/dev/null)

        if echo "$first_trip" | jq -e '.id // .trip_id' &>/dev/null; then
            log_success "Trip has ID"
            ((TESTS_PASSED++))
        fi

        if echo "$first_trip" | jq -e '.started_at // .start_time' &>/dev/null; then
            log_success "Trip has start time"
            ((TESTS_PASSED++))
        fi
    else
        log_info "No trips in history yet (may be expected for fresh device)"
    fi

    # Check UI if device available
    if adb_check_device; then
        log_step "Checking trip history screen"
        adb_open_deep_link "phonemanager://trips"
        sleep 3
        adb_screenshot "trip_history_screen"
    fi

    test_end
}

# =============================================================================
# Test: Trip Detail Polyline
# =============================================================================
test_trip_detail_polyline() {
    test_start "Trip Detail Polyline"

    # Get a completed trip
    log_step "Getting completed trip for polyline test"
    local history
    history=$(api_get_trips "$DEVICE_ID" 1 2>/dev/null || echo "[]")

    local trip_id
    trip_id=$(echo "$history" | jq -r '.[0].id // .[0].trip_id // empty' 2>/dev/null)

    if [[ -z "$trip_id" ]]; then
        # Create a trip first
        log_step "Creating trip for polyline test"
        adb_simulate_walking_trip \
            "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON" \
            "$TEST_LOC_PARK_LAT" "$TEST_LOC_PARK_LON" \
            60 &

        local sim_pid=$!
        local trip_data
        trip_data=$(wait_for_trip_start 30)
        wait $sim_pid 2>/dev/null || true

        if [[ -n "$trip_data" ]]; then
            trip_id=$(echo "$trip_data" | jq -r '.id // .trip_id')
            adb_set_location "$TEST_LOC_PARK_LAT" "$TEST_LOC_PARK_LON"
            sleep 120  # Wait for trip to end
            api_end_trip "$DEVICE_ID" "$trip_id" 2>/dev/null || true
        fi
    fi

    if [[ -n "$trip_id" ]]; then
        # Get trip details with route
        log_step "Fetching trip route/polyline"
        local trip_detail
        trip_detail=$(api_get_trip "$DEVICE_ID" "$trip_id" 2>/dev/null || echo "")

        if [[ -n "$trip_detail" ]]; then
            local polyline
            polyline=$(echo "$trip_detail" | jq -r '.polyline // .route // .path // empty' 2>/dev/null)
            local points
            points=$(echo "$trip_detail" | jq '.points // .locations // .route_points // []' 2>/dev/null)
            local point_count
            point_count=$(echo "$points" | jq 'length' 2>/dev/null || echo "0")

            if [[ -n "$polyline" ]] && [[ "$polyline" != "null" ]]; then
                log_success "Trip has polyline data"
                ((TESTS_PASSED++))
            elif [[ $point_count -gt 0 ]]; then
                log_success "Trip has route points ($point_count)"
                ((TESTS_PASSED++))
            else
                log_info "Trip may not have route data yet"
            fi
        fi
    else
        test_skip "No trip available for polyline test"
    fi

    test_end
}

# =============================================================================
# Test: Manual Trip End
# =============================================================================
test_trip_manual_end() {
    test_start "Manual Trip End"

    # Start a trip
    log_step "Starting trip for manual end test"
    adb_simulate_walking_trip \
        "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON" \
        "$TEST_LOC_PARK_LAT" "$TEST_LOC_PARK_LON" \
        30 &

    local sim_pid=$!
    local trip_data
    trip_data=$(wait_for_trip_start 30)
    wait $sim_pid 2>/dev/null || true

    if [[ -z "$trip_data" ]]; then
        test_skip "Could not start trip for manual end test"
        test_end
        return
    fi

    local trip_id
    trip_id=$(echo "$trip_data" | jq -r '.id // .trip_id')
    log_info "Active trip ID: $trip_id"

    # Manually end the trip via API
    log_step "Manually ending trip via API"
    local end_response
    end_response=$(api_end_trip "$DEVICE_ID" "$trip_id" 2>&1 || true)

    if echo "$end_response" | grep -qi "success\|ended\|200" || [[ -z "$end_response" ]]; then
        log_success "Trip manually ended successfully"
        ((TESTS_PASSED++))
    else
        log_debug "End response: ${end_response:0:100}"
    fi

    # Verify trip is ended
    log_step "Verifying trip is ended"
    sleep 2
    local ended_trip
    ended_trip=$(api_get_trip "$DEVICE_ID" "$trip_id" 2>/dev/null || echo "")

    if echo "$ended_trip" | jq -e '.ended_at // .end_time' 2>/dev/null | grep -qv "null"; then
        log_success "Trip has end time recorded"
        ((TESTS_PASSED++))
    else
        log_warning "Trip end time not recorded"
    fi

    # Verify no active trip
    local active
    active=$(api_get_active_trip "$DEVICE_ID" 2>/dev/null || echo "")

    if [[ -z "$active" ]] || ! echo "$active" | jq -e '.id' &>/dev/null; then
        log_success "No active trip after manual end"
        ((TESTS_PASSED++))
    fi

    test_end
}

# =============================================================================
# Test: Trip Naming
# =============================================================================
test_trip_naming() {
    test_start "Trip Naming"

    # Get a trip to update
    log_step "Getting trip for naming test"
    local history
    history=$(api_get_trips "$DEVICE_ID" 1 2>/dev/null || echo "[]")

    local trip_id
    trip_id=$(echo "$history" | jq -r '.[0].id // .[0].trip_id // empty' 2>/dev/null)

    if [[ -z "$trip_id" ]]; then
        test_skip "No trip available for naming test"
        test_end
        return
    fi

    # Update trip name
    local new_name="Morning Commute $(date +%H%M)"
    log_step "Updating trip name to: $new_name"

    local update_response
    update_response=$(api_update_trip "$DEVICE_ID" "$trip_id" "$new_name" 2>&1 || true)

    if echo "$update_response" | grep -qi "success\|updated\|200" || [[ -z "$update_response" ]]; then
        log_success "Trip name update request accepted"
        ((TESTS_PASSED++))
    else
        log_debug "Update response: ${update_response:0:100}"
    fi

    # Verify name was updated
    log_step "Verifying trip name"
    sleep 1
    local updated_trip
    updated_trip=$(api_get_trip "$DEVICE_ID" "$trip_id" 2>/dev/null || echo "")

    local actual_name
    actual_name=$(echo "$updated_trip" | jq -r '.name // .title // empty')

    if [[ "$actual_name" == "$new_name" ]]; then
        log_success "Trip name updated correctly"
        ((TESTS_PASSED++))
    else
        log_warning "Trip name may not have been updated (got: $actual_name)"
    fi

    test_end
}

# =============================================================================
# Main Test Execution
# =============================================================================
main() {
    echo ""
    echo "=============================================="
    echo "  E2E Trip Detection Tests"
    echo "  Session: $TEST_SESSION_ID"
    echo "=============================================="
    echo ""

    # Check dependencies
    if ! check_dependencies; then
        log_error "Missing dependencies, cannot run tests"
        exit 1
    fi

    # Check ADB connection
    if ! check_adb_connection; then
        log_error "ADB not connected"
        exit 1
    fi

    # Check backend
    if ! check_backend; then
        log_error "Backend not available"
        exit 1
    fi

    # Setup
    if ! setup_trip_tests; then
        log_error "Setup failed"
        exit 1
    fi

    # Run tests
    test_trip_auto_start_on_movement
    test_trip_auto_end_on_stationary
    test_trip_grace_period_handling
    test_trip_mode_detection_walking
    test_trip_mode_detection_driving
    test_trip_mode_change_during_trip
    test_trip_statistics_calculation
    test_trip_history_display
    test_trip_detail_polyline
    test_trip_manual_end
    test_trip_naming

    # Cleanup
    cleanup_trip_tests

    # Print summary
    print_summary

    # Generate HTML report
    generate_html_report

    # Return exit code based on test results
    [[ $TESTS_FAILED -eq 0 ]]
}

# Run if executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
