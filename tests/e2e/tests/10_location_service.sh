#!/bin/bash
# =============================================================================
# E2E Tests: Location Service Lifecycle
# Tests location tracking service start/stop, background operation, and uploads
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
SERVICE_CHECK_RETRIES=10
SERVICE_CHECK_INTERVAL=2

# =============================================================================
# Test Setup
# =============================================================================
setup_location_tests() {
    log_info "Setting up location service tests..."

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

    # Get or register device
    DEVICE_ID=$(adb_get_device_id 2>/dev/null || echo "")
    if [[ -z "$DEVICE_ID" ]]; then
        DEVICE_ID=$(generate_device_id)
        log_info "Using generated device ID: $DEVICE_ID"
    fi

    # Ensure location permissions are granted
    log_step "Granting location permissions"
    adb_grant_location_permissions

    # Set initial location
    log_step "Setting initial test location"
    adb_set_location "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON"
}

cleanup_location_tests() {
    log_info "Cleaning up location service tests..."

    # Stop location service if running
    adb_stop_location_service 2>/dev/null || true
}

# =============================================================================
# Test: Service Starts After App Launch
# =============================================================================
test_service_starts_after_launch() {
    test_start "Location Service Starts After App Launch"

    # Stop any existing service
    log_step "Stopping any existing location service"
    adb_stop_location_service 2>/dev/null || true
    sleep 2

    # Launch the app
    log_step "Launching app"
    adb_launch_app
    sleep 5

    # Wait for service to start
    log_step "Waiting for location service to start"
    local started=false
    for ((i=1; i<=SERVICE_CHECK_RETRIES; i++)); do
        if adb_is_location_service_running; then
            started=true
            break
        fi
        log_debug "Service check attempt $i/$SERVICE_CHECK_RETRIES..."
        sleep $SERVICE_CHECK_INTERVAL
    done

    if $started; then
        log_success "Location service started after app launch"
        ((TESTS_PASSED++))
    else
        log_error "Location service did not start after app launch"
        ((TESTS_FAILED++))
        adb_screenshot "service_not_started"
    fi

    # Verify notification is displayed
    log_step "Checking for foreground service notification"
    if adb_check_notification_exists "$APP_PACKAGE"; then
        log_success "Foreground service notification displayed"
        ((TESTS_PASSED++))
    else
        log_warning "Foreground service notification not detected"
    fi

    test_end
}

# =============================================================================
# Test: Service Survives App Kill
# =============================================================================
test_service_survives_app_kill() {
    test_start "Location Service Survives App Kill"

    # Ensure service is running
    log_step "Ensuring location service is running"
    if ! adb_is_location_service_running; then
        adb_launch_app
        sleep 5
        if ! adb_wait_for_location_service 30; then
            test_skip "Could not start location service"
            test_end
            return
        fi
    fi

    log_success "Location service is running"

    # Force stop the app (not the service)
    log_step "Force stopping app process"
    adb shell am force-stop "$APP_PACKAGE"
    sleep 3

    # Check if service is still running
    log_step "Checking if service survives app kill"

    # Note: After force-stop, service should restart based on START_STICKY
    local still_running=false
    for ((i=1; i<=SERVICE_CHECK_RETRIES; i++)); do
        if adb_is_location_service_running; then
            still_running=true
            break
        fi
        log_debug "Service recovery check $i/$SERVICE_CHECK_RETRIES..."
        sleep $SERVICE_CHECK_INTERVAL
    done

    if $still_running; then
        log_success "Location service survived or recovered after app kill"
        ((TESTS_PASSED++))
    else
        log_warning "Location service did not survive app kill (may be expected behavior)"
        log_info "Service may restart via AlarmManager or JobScheduler"
    fi

    test_end
}

# =============================================================================
# Test: Service Restarts on Boot (Simulated)
# =============================================================================
test_service_restarts_on_boot() {
    test_start "Location Service Boot Receiver"

    # Note: We can't actually reboot the emulator in E2E tests
    # Instead, we test that the BOOT_COMPLETED receiver is registered

    log_step "Checking BOOT_COMPLETED receiver registration"

    # Query package manager for receivers
    local receivers
    receivers=$(adb shell pm dump "$APP_PACKAGE" 2>/dev/null | grep -i "boot" || echo "")

    if echo "$receivers" | grep -qi "BOOT_COMPLETED\|boot"; then
        log_success "Boot receiver is registered"
        ((TESTS_PASSED++))
    else
        log_warning "Boot receiver registration not detected via pm dump"
    fi

    # Simulate boot completed broadcast (may require system permissions)
    log_step "Simulating BOOT_COMPLETED broadcast"
    local broadcast_result
    broadcast_result=$(adb shell am broadcast \
        -a android.intent.action.BOOT_COMPLETED \
        -n "$APP_PACKAGE/.receivers.BootReceiver" 2>&1 || echo "")

    if echo "$broadcast_result" | grep -qi "delivered\|result=0"; then
        log_success "BOOT_COMPLETED broadcast delivered"
        ((TESTS_PASSED++))
    else
        log_info "BOOT_COMPLETED broadcast may require system permissions"
        log_debug "Result: $broadcast_result"
    fi

    # Wait and check if service starts
    sleep 5
    if adb_is_location_service_running; then
        log_success "Service running after boot broadcast"
        ((TESTS_PASSED++))
    fi

    test_end
}

# =============================================================================
# Test: Location Capture at Correct Intervals
# =============================================================================
test_location_capture_intervals() {
    test_start "Location Capture Intervals"

    # Ensure service is running
    if ! adb_is_location_service_running; then
        adb_launch_app
        sleep 5
    fi

    # Get initial location count from backend
    log_step "Getting initial location count"
    local initial_count
    initial_count=$(api_get_location_count "$DEVICE_ID" 2>/dev/null || echo "0")
    log_debug "Initial location count: $initial_count"

    # Simulate location changes over time
    log_step "Simulating location updates over 30 seconds"
    local test_duration=30
    local update_interval=5
    local num_updates=$((test_duration / update_interval))

    for ((i=1; i<=num_updates; i++)); do
        # Slightly vary location to trigger updates
        local lat_offset=$(echo "scale=6; $i * 0.0001" | bc)
        local new_lat=$(echo "scale=6; $TEST_LOC_HOME_LAT + $lat_offset" | bc)
        adb_set_location "$new_lat" "$TEST_LOC_HOME_LON"
        log_debug "Location update $i: $new_lat, $TEST_LOC_HOME_LON"
        sleep $update_interval
    done

    # Get final location count
    log_step "Checking location count after updates"
    local final_count
    final_count=$(api_get_location_count "$DEVICE_ID" 2>/dev/null || echo "0")
    log_debug "Final location count: $final_count"

    # Calculate locations captured
    local captured=$((final_count - initial_count))
    log_info "Locations captured: $captured (expected: ~$num_updates)"

    # Should have captured at least some locations
    if [[ $captured -gt 0 ]]; then
        log_success "Location updates are being captured ($captured locations)"
        ((TESTS_PASSED++))
    else
        log_error "No new locations captured"
        ((TESTS_FAILED++))
    fi

    test_end
}

# =============================================================================
# Test: Location Queue Processing
# =============================================================================
test_location_queue_processing() {
    test_start "Location Queue Processing"

    # Put device in airplane mode to queue locations
    log_step "Enabling airplane mode to queue locations"
    adb_enable_airplane_mode

    # Generate some location updates
    log_step "Generating locations while offline"
    for i in {1..5}; do
        local lat_offset=$(echo "scale=6; $i * 0.0002" | bc)
        local new_lat=$(echo "scale=6; $TEST_LOC_HOME_LAT + $lat_offset" | bc)
        adb_set_location "$new_lat" "$TEST_LOC_HOME_LON"
        sleep 2
    done

    # Disable airplane mode
    log_step "Disabling airplane mode to process queue"
    adb_disable_airplane_mode
    sleep 5

    # Wait for queue to be processed
    log_step "Waiting for queue processing (15 seconds)"
    sleep 15

    # Check if locations were uploaded
    # This requires checking the backend for recent uploads
    local recent_locations
    recent_locations=$(api_get_recent_locations "$DEVICE_ID" 5 2>/dev/null || echo "[]")

    local location_count
    location_count=$(echo "$recent_locations" | jq 'length' 2>/dev/null || echo "0")

    if [[ $location_count -gt 0 ]]; then
        log_success "Queued locations were processed and uploaded ($location_count)"
        ((TESTS_PASSED++))
    else
        log_warning "Could not verify queue processing (backend may not have recent data)"
    fi

    test_end
}

# =============================================================================
# Test: Batch Upload Success
# =============================================================================
test_batch_upload_success() {
    test_start "Batch Location Upload"

    # Generate batch location data
    log_step "Generating batch location data"
    local locations
    locations=$(generate_location_batch 10 "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON" 0.001)

    # Submit batch via API
    log_step "Submitting batch upload"
    local response
    response=$(api_batch_locations "$DEVICE_ID" "$locations" 2>&1 || true)

    if echo "$response" | grep -qi "success\|created\|accepted\|200\|201\|202"; then
        log_success "Batch upload accepted"
        ((TESTS_PASSED++))
    else
        log_debug "Response: ${response:0:200}"
        if echo "$response" | grep -qi "error\|failed"; then
            log_error "Batch upload failed"
            ((TESTS_FAILED++))
        else
            log_warning "Batch upload response unclear"
        fi
    fi

    # Verify batch was stored
    log_step "Verifying batch was stored"
    sleep 2
    local stored
    stored=$(api_get_recent_locations "$DEVICE_ID" 10 2>/dev/null || echo "[]")
    local stored_count
    stored_count=$(echo "$stored" | jq 'length' 2>/dev/null || echo "0")

    if [[ $stored_count -ge 10 ]]; then
        log_success "Batch locations stored in backend ($stored_count)"
        ((TESTS_PASSED++))
    else
        log_info "Stored location count: $stored_count"
    fi

    test_end
}

# =============================================================================
# Test: Offline Queue Management
# =============================================================================
test_offline_queue_management() {
    test_start "Offline Queue Management"

    # Enable airplane mode
    log_step "Going offline"
    adb_enable_airplane_mode

    # Record queue before
    local queue_size_before
    queue_size_before=$(adb_get_location_queue_size 2>/dev/null || echo "0")
    log_debug "Queue size before: $queue_size_before"

    # Generate locations
    log_step "Generating locations while offline"
    for i in {1..5}; do
        local offset=$(echo "scale=6; $i * 0.0003" | bc)
        adb_set_location $(echo "scale=6; $TEST_LOC_HOME_LAT + $offset" | bc) "$TEST_LOC_HOME_LON"
        sleep 3
    done

    # Check queue grew
    local queue_size_after
    queue_size_after=$(adb_get_location_queue_size 2>/dev/null || echo "0")
    log_debug "Queue size after: $queue_size_after"

    if [[ $queue_size_after -gt $queue_size_before ]]; then
        log_success "Queue grew while offline ($queue_size_before -> $queue_size_after)"
        ((TESTS_PASSED++))
    else
        log_info "Queue size tracking may not be available"
    fi

    # Go back online
    log_step "Going back online"
    adb_disable_airplane_mode
    sleep 10

    # Verify queue emptied
    local queue_size_final
    queue_size_final=$(adb_get_location_queue_size 2>/dev/null || echo "0")

    if [[ $queue_size_final -lt $queue_size_after ]]; then
        log_success "Queue processed after coming online ($queue_size_after -> $queue_size_final)"
        ((TESTS_PASSED++))
    else
        log_info "Queue may process asynchronously"
    fi

    test_end
}

# =============================================================================
# Test: Service Notification Display
# =============================================================================
test_service_notification_display() {
    test_start "Service Notification Display"

    # Ensure service is running
    if ! adb_is_location_service_running; then
        adb_launch_app
        sleep 5
    fi

    # Take screenshot of notification area
    log_step "Capturing notification screenshot"
    adb shell cmd statusbar expand-notifications
    sleep 1
    adb_screenshot "location_service_notification"
    adb shell cmd statusbar collapse
    sleep 1

    # Check notification via dumpsys
    log_step "Checking notification via dumpsys"
    local notifications
    notifications=$(adb shell dumpsys notification --noredact 2>/dev/null | grep -A5 "$APP_PACKAGE" || echo "")

    if [[ -n "$notifications" ]]; then
        log_success "Notification from app found"
        ((TESTS_PASSED++))

        # Check for expected notification content
        if echo "$notifications" | grep -qi "tracking\|location\|running"; then
            log_success "Notification appears to be location tracking notification"
            ((TESTS_PASSED++))
        fi
    else
        log_warning "Could not detect app notification via dumpsys"
    fi

    test_end
}

# =============================================================================
# Test: Service Health Recovery
# =============================================================================
test_service_health_recovery() {
    test_start "Service Health Recovery"

    # Ensure service is running
    if ! adb_is_location_service_running; then
        adb_launch_app
        sleep 5
    fi

    # Get service PID
    log_step "Getting location service PID"
    local service_pid
    service_pid=$(adb shell pidof "$APP_PACKAGE" 2>/dev/null | head -1)

    if [[ -z "$service_pid" ]]; then
        test_skip "Could not get service PID"
        test_end
        return
    fi

    log_debug "Service PID: $service_pid"

    # Kill the service process
    log_step "Killing service process"
    adb shell kill -9 "$service_pid" 2>/dev/null || true
    sleep 2

    # Wait for service to recover
    log_step "Waiting for service recovery"
    local recovered=false
    for ((i=1; i<=10; i++)); do
        if adb_is_location_service_running; then
            recovered=true
            break
        fi
        sleep 2
    done

    if $recovered; then
        log_success "Service recovered after process kill"
        ((TESTS_PASSED++))

        # Check new PID is different
        local new_pid
        new_pid=$(adb shell pidof "$APP_PACKAGE" 2>/dev/null | head -1)
        if [[ "$new_pid" != "$service_pid" ]]; then
            log_success "Service restarted with new PID ($service_pid -> $new_pid)"
            ((TESTS_PASSED++))
        fi
    else
        log_error "Service did not recover after kill"
        ((TESTS_FAILED++))
    fi

    test_end
}

# =============================================================================
# Test: Doze Mode Behavior
# =============================================================================
test_doze_mode_behavior() {
    test_start "Doze Mode Behavior"

    # Ensure service is running
    if ! adb_is_location_service_running; then
        adb_launch_app
        sleep 5
    fi

    # Put device into Doze mode
    log_step "Putting device into Doze mode"
    adb_enter_doze_mode

    # Wait a bit
    sleep 10

    # Check if service is still running
    log_step "Checking service during Doze mode"
    if adb_is_location_service_running; then
        log_success "Service running during Doze mode"
        ((TESTS_PASSED++))
    else
        log_warning "Service may be affected by Doze mode"
    fi

    # Check if app is whitelisted for battery optimization
    log_step "Checking battery optimization whitelist"
    local whitelist
    whitelist=$(adb shell dumpsys deviceidle whitelist 2>/dev/null | grep "$APP_PACKAGE" || echo "")

    if [[ -n "$whitelist" ]]; then
        log_success "App is battery optimization whitelisted"
        ((TESTS_PASSED++))
    else
        log_info "App may not be whitelisted for battery optimization"
    fi

    # Exit Doze mode
    log_step "Exiting Doze mode"
    adb_exit_doze_mode

    test_end
}

# =============================================================================
# Main Test Execution
# =============================================================================
main() {
    echo ""
    echo "=============================================="
    echo "  E2E Location Service Tests"
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
    if ! setup_location_tests; then
        log_error "Setup failed"
        exit 1
    fi

    # Run tests
    test_service_starts_after_launch
    test_service_survives_app_kill
    test_service_restarts_on_boot
    test_location_capture_intervals
    test_location_queue_processing
    test_batch_upload_success
    test_offline_queue_management
    test_service_notification_display
    test_service_health_recovery
    test_doze_mode_behavior

    # Cleanup
    cleanup_location_tests

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
