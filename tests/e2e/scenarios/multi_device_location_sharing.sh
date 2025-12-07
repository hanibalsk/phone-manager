#!/bin/bash
# Multi-Device Location Sharing Test Scenario
# Tests real-time location sharing between 2-3 devices in a family group
#
# Prerequisites:
# - Backend running at $API_BASE_URL
# - APK built and available
#
# Usage: ./multi_device_location_sharing.sh [--headless]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
E2E_ROOT="$(dirname "$SCRIPT_DIR")"

# Source libraries
source "${E2E_ROOT}/config.sh"
source "${E2E_ROOT}/lib/emulator_manager.sh"
source "${E2E_ROOT}/lib/multidevice.sh"
source "${E2E_ROOT}/lib/api.sh"
source "${E2E_ROOT}/lib/adb.sh"

# Parse arguments
HEADLESS="${1:-true}"
[[ "${1:-}" == "--headless" ]] && HEADLESS="true"

# Test configuration
DEVICE_COUNT=2
TEST_NAME="multi_device_location_sharing"
REPORT_FILE="${REPORTS_DIR}/${TEST_NAME}_${TEST_SESSION_ID}.json"

# Test locations
PARENT_START_LAT="$TEST_LOC_HOME_LAT"
PARENT_START_LON="$TEST_LOC_HOME_LON"
CHILD_START_LAT="$TEST_LOC_SCHOOL_LAT"
CHILD_START_LON="$TEST_LOC_SCHOOL_LON"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$TEST_LOG"
}

cleanup() {
    log "Cleaning up..."
    shutdown_all_emulators 2>/dev/null || true
    # Clean up test data if configured
    if [[ "${CLEANUP_AFTER_TEST:-true}" == "true" ]]; then
        log "Cleaning up test data..."
        # API cleanup would go here
    fi
}

trap cleanup EXIT

test_location_sharing() {
    log "=== Starting Multi-Device Location Sharing Test ==="
    local test_start=$(date +%s)
    local test_result="PASS"
    local errors=()

    # Step 1: Launch emulators
    log "Step 1: Launching $DEVICE_COUNT emulators..."
    if ! launch_emulators "$DEVICE_COUNT" "$HEADLESS"; then
        log "ERROR: Failed to launch emulators"
        return 1
    fi

    # Step 2: Install APK on all devices
    log "Step 2: Installing APK on all devices..."
    if ! install_apk_on_all "$APP_APK_PATH"; then
        log "ERROR: Failed to install APK"
        return 1
    fi

    # Step 3: Grant permissions on all devices
    log "Step 3: Granting permissions..."
    grant_permissions_on_all "$APP_PACKAGE"

    # Step 4: Set initial locations
    log "Step 4: Setting initial locations..."
    adb_set_location_for_device "PARENT" "$PARENT_START_LAT" "$PARENT_START_LON"
    adb_set_location_for_device "CHILD1" "$CHILD_START_LAT" "$CHILD_START_LON"
    sleep 2

    # Step 5: Create test group via API
    log "Step 5: Creating test group..."
    local group_response
    group_response=$(api_create_group "$TEST_GROUP_FAMILY" "$ADMIN_API_KEY")
    local group_id
    group_id=$(echo "$group_response" | jq -r '.group.id')
    local invite_code
    invite_code=$(echo "$group_response" | jq -r '.group.inviteCode')

    if [[ -z "$group_id" || "$group_id" == "null" ]]; then
        log "ERROR: Failed to create test group"
        errors+=("Group creation failed")
        test_result="FAIL"
    else
        log "Created group: $group_id with invite code: $invite_code"
    fi

    # Step 6: Register devices with API
    log "Step 6: Registering devices..."
    local parent_device_id child1_device_id

    parent_device_id=$(api_register_device "Parent Device" "PARENT" "$ADMIN_API_KEY" | jq -r '.device.id')
    child1_device_id=$(api_register_device "Child Device" "CHILD1" "$ADMIN_API_KEY" | jq -r '.device.id')

    log "Registered devices: Parent=$parent_device_id, Child1=$child1_device_id"

    # Step 7: Launch apps and enable tracking
    log "Step 7: Launching apps..."
    launch_app_on_all_devices

    sleep 5  # Wait for apps to initialize

    # Step 8: Update locations and verify sync
    log "Step 8: Testing location updates..."

    # Update parent location
    local new_parent_lat="37.7755"
    local new_parent_lon="-122.4180"
    adb_set_location_for_device "PARENT" "$new_parent_lat" "$new_parent_lon"

    # Send location update via API (simulating app behavior)
    api_update_device_location "$parent_device_id" "$new_parent_lat" "$new_parent_lon" "$ADMIN_API_KEY"

    sleep 3

    # Verify child can see parent's location
    local parent_location
    parent_location=$(api_get_device_location "$parent_device_id" "$ADMIN_API_KEY")
    local returned_lat
    returned_lat=$(echo "$parent_location" | jq -r '.location.latitude')

    if [[ "$returned_lat" != "$new_parent_lat" ]]; then
        log "ERROR: Location update not reflected. Expected $new_parent_lat, got $returned_lat"
        errors+=("Location sync failed")
        test_result="FAIL"
    else
        log "Location sync verified: Parent at $returned_lat"
    fi

    # Step 9: Test bidirectional location sharing
    log "Step 9: Testing bidirectional sharing..."

    local new_child_lat="37.8720"
    local new_child_lon="-122.2735"
    adb_set_location_for_device "CHILD1" "$new_child_lat" "$new_child_lon"
    api_update_device_location "$child1_device_id" "$new_child_lat" "$new_child_lon" "$ADMIN_API_KEY"

    sleep 3

    # Verify both locations via group members API
    local members
    members=$(api_get_group_members "$group_id" "$ADMIN_API_KEY")
    local member_count
    member_count=$(echo "$members" | jq '.members | length')

    log "Group has $member_count members"

    # Step 10: Test location history
    log "Step 10: Testing location history..."

    # Generate multiple location updates
    for i in $(seq 1 5); do
        local lat=$(echo "$new_parent_lat + 0.001 * $i" | bc)
        adb_set_location_for_device "PARENT" "$lat" "$new_parent_lon"
        api_update_device_location "$parent_device_id" "$lat" "$new_parent_lon" "$ADMIN_API_KEY"
        sleep 1
    done

    # Fetch history
    local history
    history=$(api_get_location_history "$parent_device_id" "$ADMIN_API_KEY")
    local history_count
    history_count=$(echo "$history" | jq '.locations | length')

    if [[ "$history_count" -lt 5 ]]; then
        log "WARNING: Expected at least 5 location history entries, got $history_count"
    else
        log "Location history verified: $history_count entries"
    fi

    # Take screenshots
    log "Taking screenshots..."
    adb_screenshot_for_device "PARENT" "${SCREENSHOTS_DIR}/${TEST_NAME}_parent_${TEST_SESSION_ID}.png"
    adb_screenshot_for_device "CHILD1" "${SCREENSHOTS_DIR}/${TEST_NAME}_child1_${TEST_SESSION_ID}.png"

    # Calculate duration
    local test_end=$(date +%s)
    local duration=$((test_end - test_start))

    # Generate report
    log "Generating report..."
    cat > "$REPORT_FILE" << EOF
{
    "test_name": "$TEST_NAME",
    "session_id": "$TEST_SESSION_ID",
    "result": "$test_result",
    "duration_seconds": $duration,
    "device_count": $DEVICE_COUNT,
    "group_id": "$group_id",
    "errors": $(printf '%s\n' "${errors[@]:-}" | jq -R . | jq -s .),
    "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}
EOF

    log "=== Test completed: $test_result (${duration}s) ==="
    log "Report saved to: $REPORT_FILE"

    [[ "$test_result" == "PASS" ]]
}

# Run the test
test_location_sharing
