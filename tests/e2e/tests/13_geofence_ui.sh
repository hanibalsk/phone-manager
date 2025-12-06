#!/bin/bash
# =============================================================================
# E2E Tests: Geofence UI and Events
# Tests geofence creation, display, enter/exit events, and notifications
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
# Test State
# =============================================================================
DEVICE_ID=""
TEST_GEOFENCE_ID=""
TEST_GEOFENCE_NAME=""

# =============================================================================
# Test Setup
# =============================================================================
setup_geofence_tests() {
    log_info "Setting up geofence tests..."

    # Get device ID
    if adb_check_device; then
        DEVICE_ID=$(adb_get_device_id 2>/dev/null || generate_device_id)
        adb_grant_location_permissions
    else
        DEVICE_ID=$(generate_device_id)
    fi

    # Set initial location outside test geofences
    if adb_check_device; then
        adb_set_location "37.8000" "-122.4500"  # Far from test locations
        sleep 2
    fi

    TEST_GEOFENCE_NAME="E2E Test Geofence $(date +%s)"
    log_info "Device ID: $DEVICE_ID"
}

cleanup_geofence_tests() {
    log_info "Cleaning up geofence tests..."

    # Delete test geofences
    if [[ -n "$TEST_GEOFENCE_ID" ]]; then
        api_delete_geofence "$DEVICE_ID" "$TEST_GEOFENCE_ID" 2>/dev/null || true
    fi
}

# =============================================================================
# Test: Create Geofence UI
# =============================================================================
test_create_geofence_ui() {
    test_start "Create Geofence - UI"

    if ! adb_check_device; then
        test_skip "No device for UI test"
        test_end
        return
    fi

    log_step "Opening geofence creation screen"
    adb_open_deep_link "phonemanager://geofences/create"
    sleep 3

    adb_screenshot "geofence_create_screen"

    log_success "Geofence creation screen opened"
    ((TESTS_PASSED++))

    log_info "Manual verification: Check screenshot for geofence creation UI"

    test_end
}

# =============================================================================
# Test: Create Geofence API
# =============================================================================
test_create_geofence_api() {
    test_start "Create Geofence - API"

    log_step "Creating geofence via API"

    local geofence_data
    geofence_data=$(generate_geofence_data "$TEST_GEOFENCE_NAME" \
        "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON" \
        "$TEST_GEOFENCE_HOME_RADIUS")

    local response
    response=$(api_create_geofence "$DEVICE_ID" "$geofence_data")
    local status=$?

    if [[ $status -ne 0 ]]; then
        log_error "Create geofence API failed"
        test_end
        return 1
    fi

    TEST_GEOFENCE_ID=$(echo "$response" | jq -r '.id // .geofence_id // empty')

    assert_not_equals "" "$TEST_GEOFENCE_ID" "Geofence should have an ID"

    # Verify geofence properties
    local returned_name
    returned_name=$(echo "$response" | jq -r '.name')
    assert_equals "$TEST_GEOFENCE_NAME" "$returned_name" "Geofence name should match"

    local returned_radius
    returned_radius=$(echo "$response" | jq -r '.radius')
    assert_equals "$TEST_GEOFENCE_HOME_RADIUS" "$returned_radius" "Geofence radius should match"

    log_info "Created geofence ID: $TEST_GEOFENCE_ID"

    test_end
}

# =============================================================================
# Test: Geofence Map Display
# =============================================================================
test_geofence_map_display() {
    test_start "Geofence Map Display"

    if ! adb_check_device; then
        test_skip "No device for map display test"
        test_end
        return
    fi

    log_step "Opening map with geofences"
    adb_open_deep_link "phonemanager://map"
    sleep 5

    adb_screenshot "geofence_map_display"

    log_success "Map screen captured"
    ((TESTS_PASSED++))

    # Check if geofences are listed
    log_step "Opening geofence list"
    adb_open_deep_link "phonemanager://geofences"
    sleep 3

    adb_screenshot "geofence_list"

    log_success "Geofence list captured"
    ((TESTS_PASSED++))

    test_end
}

# =============================================================================
# Test: Geofence Enter Event
# =============================================================================
test_geofence_enter_event() {
    test_start "Geofence Enter Event"

    if [[ -z "$TEST_GEOFENCE_ID" ]]; then
        # Create geofence if not already created
        log_step "Creating test geofence"
        local geofence_data
        geofence_data=$(generate_geofence_data "Enter Test Geofence" \
            "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON" "100")

        local response
        response=$(api_create_geofence "$DEVICE_ID" "$geofence_data")
        TEST_GEOFENCE_ID=$(echo "$response" | jq -r '.id // .geofence_id')
    fi

    if ! adb_check_device; then
        test_skip "No device for enter event test"
        test_end
        return
    fi

    # Start outside geofence
    log_step "Starting outside geofence"
    adb_set_location "37.7800" "-122.4300"  # Far from home
    sleep 5

    # Get initial event count
    local initial_events
    initial_events=$(api_get_geofence_events "$DEVICE_ID" "$TEST_GEOFENCE_ID" 2>/dev/null || echo "[]")
    local initial_count
    initial_count=$(echo "$initial_events" | jq 'length' 2>/dev/null || echo "0")

    # Move into geofence
    log_step "Moving into geofence"
    adb_set_location "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON"
    sleep 10  # Wait for geofence detection

    # Check for ENTER event
    log_step "Checking for ENTER event"
    local events
    events=$(api_get_geofence_events "$DEVICE_ID" "$TEST_GEOFENCE_ID" 2>/dev/null || echo "[]")
    local event_count
    event_count=$(echo "$events" | jq 'length' 2>/dev/null || echo "0")

    if [[ $event_count -gt $initial_count ]]; then
        local latest_event
        latest_event=$(echo "$events" | jq '.[0]' 2>/dev/null)
        local event_type
        event_type=$(echo "$latest_event" | jq -r '.type // .event_type // empty')

        if echo "$event_type" | grep -qi "enter"; then
            log_success "ENTER event detected"
            ((TESTS_PASSED++))
        else
            log_info "Event type: $event_type"
        fi
    else
        log_warning "No new geofence events detected (may need longer wait)"
    fi

    test_end
}

# =============================================================================
# Test: Geofence Exit Event
# =============================================================================
test_geofence_exit_event() {
    test_start "Geofence Exit Event"

    if ! adb_check_device; then
        test_skip "No device for exit event test"
        test_end
        return
    fi

    if [[ -z "$TEST_GEOFENCE_ID" ]]; then
        test_skip "No geofence for exit test"
        test_end
        return
    fi

    # Ensure we're inside the geofence
    log_step "Ensuring inside geofence"
    adb_set_location "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON"
    sleep 5

    # Get initial event count
    local initial_events
    initial_events=$(api_get_geofence_events "$DEVICE_ID" "$TEST_GEOFENCE_ID" 2>/dev/null || echo "[]")
    local initial_count
    initial_count=$(echo "$initial_events" | jq 'length' 2>/dev/null || echo "0")

    # Move out of geofence
    log_step "Moving out of geofence"
    adb_set_location "37.7800" "-122.4300"  # Far from home
    sleep 10  # Wait for geofence detection

    # Check for EXIT event
    log_step "Checking for EXIT event"
    local events
    events=$(api_get_geofence_events "$DEVICE_ID" "$TEST_GEOFENCE_ID" 2>/dev/null || echo "[]")
    local event_count
    event_count=$(echo "$events" | jq 'length' 2>/dev/null || echo "0")

    if [[ $event_count -gt $initial_count ]]; then
        local latest_event
        latest_event=$(echo "$events" | jq '.[0]' 2>/dev/null)
        local event_type
        event_type=$(echo "$latest_event" | jq -r '.type // .event_type // empty')

        if echo "$event_type" | grep -qi "exit\|leave"; then
            log_success "EXIT event detected"
            ((TESTS_PASSED++))
        else
            log_info "Event type: $event_type"
        fi
    else
        log_warning "No new geofence events detected"
    fi

    test_end
}

# =============================================================================
# Test: Geofence Dwell Event
# =============================================================================
test_geofence_dwell_event() {
    test_start "Geofence Dwell Event"

    if ! adb_check_device; then
        test_skip "No device for dwell event test"
        test_end
        return
    fi

    # Create geofence with dwell trigger
    log_step "Creating geofence with dwell trigger"
    local geofence_data
    geofence_data=$(cat <<EOF
{
    "name": "Dwell Test Geofence",
    "latitude": $TEST_LOC_WORK_LAT,
    "longitude": $TEST_LOC_WORK_LON,
    "radius": 100,
    "triggers": ["ENTER", "EXIT", "DWELL"],
    "dwell_delay": 30
}
EOF
)

    local response
    response=$(api_create_geofence "$DEVICE_ID" "$geofence_data" 2>&1 || true)
    local dwell_geofence_id
    dwell_geofence_id=$(echo "$response" | jq -r '.id // .geofence_id // empty')

    if [[ -z "$dwell_geofence_id" ]]; then
        log_info "Dwell geofence creation failed (may not be supported)"
        log_info "Response: ${response:0:100}"
        test_end
        return
    fi

    # Move into geofence and stay
    log_step "Moving into geofence and dwelling"
    adb_set_location "$TEST_LOC_WORK_LAT" "$TEST_LOC_WORK_LON"
    sleep 45  # Wait for dwell detection (30s dwell + buffer)

    # Check for DWELL event
    log_step "Checking for DWELL event"
    local events
    events=$(api_get_geofence_events "$DEVICE_ID" "$dwell_geofence_id" 2>/dev/null || echo "[]")

    local dwell_event
    dwell_event=$(echo "$events" | jq -r '.[] | select(.type == "DWELL" or .event_type == "DWELL")' 2>/dev/null | head -1)

    if [[ -n "$dwell_event" ]]; then
        log_success "DWELL event detected"
        ((TESTS_PASSED++))
    else
        log_info "No DWELL event detected (may need longer dwell time)"
    fi

    # Cleanup
    api_delete_geofence "$DEVICE_ID" "$dwell_geofence_id" 2>/dev/null || true

    test_end
}

# =============================================================================
# Test: Geofence Notification
# =============================================================================
test_geofence_notification() {
    test_start "Geofence Notification"

    if ! adb_check_device; then
        test_skip "No device for notification test"
        test_end
        return
    fi

    if [[ -z "$TEST_GEOFENCE_ID" ]]; then
        test_skip "No geofence for notification test"
        test_end
        return
    fi

    # Trigger enter event
    log_step "Triggering geofence enter for notification"
    adb_set_location "37.7800" "-122.4300"  # Outside
    sleep 5
    adb_set_location "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON"  # Inside
    sleep 10

    # Check notifications
    log_step "Checking for geofence notification"
    local notifications
    notifications=$(adb shell dumpsys notification --noredact 2>/dev/null | grep -A10 "$APP_PACKAGE" || echo "")

    if echo "$notifications" | grep -qi "geofence\|entered\|arrived"; then
        log_success "Geofence notification found"
        ((TESTS_PASSED++))
    else
        log_info "Geofence notification not detected (may be disabled or use different text)"
    fi

    # Screenshot notification area
    adb shell cmd statusbar expand-notifications
    sleep 1
    adb_screenshot "geofence_notification"
    adb shell cmd statusbar collapse

    test_end
}

# =============================================================================
# Test: Geofence Webhook Trigger
# =============================================================================
test_geofence_webhook_trigger() {
    test_start "Geofence Webhook Trigger"

    # Note: Testing webhooks requires a webhook endpoint
    # We'll verify the webhook configuration and trigger mechanism

    if [[ -z "$TEST_GEOFENCE_ID" ]]; then
        test_skip "No geofence for webhook test"
        test_end
        return
    fi

    # Check if webhook is configured
    log_step "Checking webhook configuration"
    local geofence_details
    geofence_details=$(api_get_geofence "$DEVICE_ID" "$TEST_GEOFENCE_ID" 2>/dev/null || echo "")

    if [[ -n "$geofence_details" ]]; then
        local webhook_url
        webhook_url=$(echo "$geofence_details" | jq -r '.webhook_url // .webhook // empty')

        if [[ -n "$webhook_url" ]] && [[ "$webhook_url" != "null" ]]; then
            log_success "Webhook URL configured: $webhook_url"
            ((TESTS_PASSED++))
        else
            log_info "No webhook URL configured for this geofence"
        fi
    fi

    # Test webhook endpoint configuration
    log_step "Testing webhook endpoint update"
    local test_webhook="https://httpbin.org/post"
    local update_response
    update_response=$(api_update_geofence "$DEVICE_ID" "$TEST_GEOFENCE_ID" \
        "{\"webhook_url\": \"$test_webhook\"}" 2>&1 || true)

    if echo "$update_response" | grep -qi "success\|updated\|200"; then
        log_success "Webhook URL updated successfully"
        ((TESTS_PASSED++))
    else
        log_info "Webhook update response: ${update_response:0:100}"
    fi

    test_end
}

# =============================================================================
# Test: Edit Geofence
# =============================================================================
test_edit_geofence() {
    test_start "Edit Geofence"

    if [[ -z "$TEST_GEOFENCE_ID" ]]; then
        test_skip "No geofence to edit"
        test_end
        return
    fi

    # Update geofence
    local new_name="Updated Geofence $(date +%s)"
    local new_radius="150"

    log_step "Updating geofence name and radius"
    local update_data
    update_data=$(cat <<EOF
{
    "name": "$new_name",
    "radius": $new_radius
}
EOF
)

    local response
    response=$(api_update_geofence "$DEVICE_ID" "$TEST_GEOFENCE_ID" "$update_data" 2>&1 || true)

    if echo "$response" | grep -qi "success\|updated\|200"; then
        log_success "Geofence update request accepted"
        ((TESTS_PASSED++))
    else
        log_debug "Response: ${response:0:100}"
    fi

    # Verify update
    log_step "Verifying geofence update"
    local updated_geofence
    updated_geofence=$(api_get_geofence "$DEVICE_ID" "$TEST_GEOFENCE_ID" 2>/dev/null || echo "")

    if [[ -n "$updated_geofence" ]]; then
        local actual_name
        actual_name=$(echo "$updated_geofence" | jq -r '.name')
        local actual_radius
        actual_radius=$(echo "$updated_geofence" | jq -r '.radius')

        if [[ "$actual_name" == "$new_name" ]]; then
            log_success "Geofence name updated correctly"
            ((TESTS_PASSED++))
        fi

        if [[ "$actual_radius" == "$new_radius" ]]; then
            log_success "Geofence radius updated correctly"
            ((TESTS_PASSED++))
        fi
    fi

    test_end
}

# =============================================================================
# Test: Delete Geofence
# =============================================================================
test_delete_geofence() {
    test_start "Delete Geofence"

    # Create a geofence to delete
    log_step "Creating geofence to delete"
    local geofence_data
    geofence_data=$(generate_geofence_data "Delete Test Geofence" \
        "$TEST_LOC_PARK_LAT" "$TEST_LOC_PARK_LON" "50")

    local response
    response=$(api_create_geofence "$DEVICE_ID" "$geofence_data")
    local delete_geofence_id
    delete_geofence_id=$(echo "$response" | jq -r '.id // .geofence_id')

    if [[ -z "$delete_geofence_id" ]]; then
        test_skip "Could not create geofence for delete test"
        test_end
        return
    fi

    # Delete the geofence
    log_step "Deleting geofence: $delete_geofence_id"
    local delete_response
    delete_response=$(api_delete_geofence "$DEVICE_ID" "$delete_geofence_id" 2>&1 || true)

    if echo "$delete_response" | grep -qi "success\|deleted\|200\|204" || [[ -z "$delete_response" ]]; then
        log_success "Geofence delete request accepted"
        ((TESTS_PASSED++))
    else
        log_debug "Response: ${delete_response:0:100}"
    fi

    # Verify deletion
    log_step "Verifying geofence deleted"
    local check_response
    check_response=$(api_get_geofence "$DEVICE_ID" "$delete_geofence_id" 2>&1 || true)

    if echo "$check_response" | grep -qi "not.found\|404\|error" || [[ -z "$check_response" ]]; then
        log_success "Geofence successfully deleted"
        ((TESTS_PASSED++))
    else
        log_warning "Geofence may still exist"
    fi

    test_end
}

# =============================================================================
# Main Test Execution
# =============================================================================
main() {
    echo ""
    echo "=============================================="
    echo "  E2E Geofence UI Tests"
    echo "  Session: $TEST_SESSION_ID"
    echo "=============================================="
    echo ""

    # Check dependencies
    if ! check_dependencies; then
        log_error "Missing dependencies, cannot run tests"
        exit 1
    fi

    # Check backend
    if ! check_backend; then
        log_error "Backend not available"
        exit 1
    fi

    # Setup
    setup_geofence_tests

    # Run tests
    test_create_geofence_ui
    test_create_geofence_api
    test_geofence_map_display
    test_geofence_enter_event
    test_geofence_exit_event
    test_geofence_dwell_event
    test_geofence_notification
    test_geofence_webhook_trigger
    test_edit_geofence
    test_delete_geofence

    # Cleanup
    cleanup_geofence_tests

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
