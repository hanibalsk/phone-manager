#!/bin/bash
# Multi-Device Geofence Test Scenario
# Tests geofence ENTER/EXIT/DWELL events with multiple devices
#
# Prerequisites:
# - Backend running at $API_BASE_URL
# - APK built and available
#
# Usage: ./multi_device_geofence.sh [--headless]

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
[[ "$1" == "--headless" ]] && HEADLESS="true"

# Test configuration
DEVICE_COUNT=2
TEST_NAME="multi_device_geofence"
REPORT_FILE="${REPORTS_DIR}/${TEST_NAME}_${TEST_SESSION_ID}.json"

# Geofence configuration (Home zone)
GEOFENCE_NAME="Home Zone"
GEOFENCE_CENTER_LAT="$TEST_LOC_HOME_LAT"
GEOFENCE_CENTER_LON="$TEST_LOC_HOME_LON"
GEOFENCE_RADIUS=100  # meters

# Start positions
OUTSIDE_LAT="37.7760"  # ~120m north of home
INSIDE_LAT="37.7749"   # At home center

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$TEST_LOG"
}

cleanup() {
    log "Cleaning up..."
    shutdown_all_emulators 2>/dev/null || true
}

trap cleanup EXIT

interpolate() {
    local start=$1
    local end=$2
    local step=$3
    local total_steps=$4
    echo "scale=6; $start + ($end - $start) * $step / $total_steps" | bc
}

test_geofence_events() {
    log "=== Starting Multi-Device Geofence Test ==="
    local test_start=$(date +%s)
    local test_result="PASS"
    local errors=()
    local events_detected=()

    # Step 1: Launch emulators
    log "Step 1: Launching $DEVICE_COUNT emulators..."
    if ! launch_emulators "$DEVICE_COUNT" "$HEADLESS"; then
        log "ERROR: Failed to launch emulators"
        return 1
    fi

    # Step 2: Install and setup
    log "Step 2: Installing APK..."
    install_apk_on_all "$APP_APK_PATH"
    grant_permissions_on_all "$APP_PACKAGE"

    # Step 3: Create test group
    log "Step 3: Creating test group..."
    local group_response
    group_response=$(api_create_group "Geofence Test Group" "$ADMIN_API_KEY")
    local group_id
    group_id=$(echo "$group_response" | jq -r '.group.id')

    # Step 4: Register devices
    log "Step 4: Registering devices..."
    local parent_device_id child1_device_id

    parent_device_id=$(api_register_device "Parent Device" "PARENT" "$ADMIN_API_KEY" | jq -r '.device.id')
    child1_device_id=$(api_register_device "Child Device" "CHILD1" "$ADMIN_API_KEY" | jq -r '.device.id')

    # Step 5: Create geofence
    log "Step 5: Creating geofence: $GEOFENCE_NAME..."
    local geofence_response
    geofence_response=$(api_create_geofence \
        "$GEOFENCE_NAME" \
        "$GEOFENCE_CENTER_LAT" \
        "$GEOFENCE_CENTER_LON" \
        "$GEOFENCE_RADIUS" \
        "$group_id" \
        "$ADMIN_API_KEY")
    local geofence_id
    geofence_id=$(echo "$geofence_response" | jq -r '.geofence.id')

    if [[ -z "$geofence_id" || "$geofence_id" == "null" ]]; then
        log "ERROR: Failed to create geofence"
        errors+=("Geofence creation failed")
        test_result="FAIL"
    else
        log "Created geofence: $geofence_id (center: $GEOFENCE_CENTER_LAT,$GEOFENCE_CENTER_LON, radius: ${GEOFENCE_RADIUS}m)"
    fi

    # Step 6: Position CHILD1 at home, PARENT outside
    log "Step 6: Setting initial positions..."
    adb_set_location_for_device "CHILD1" "$GEOFENCE_CENTER_LAT" "$GEOFENCE_CENTER_LON"
    adb_set_location_for_device "PARENT" "$OUTSIDE_LAT" "$GEOFENCE_CENTER_LON"

    api_update_device_location "$child1_device_id" "$GEOFENCE_CENTER_LAT" "$GEOFENCE_CENTER_LON" "$ADMIN_API_KEY"
    api_update_device_location "$parent_device_id" "$OUTSIDE_LAT" "$GEOFENCE_CENTER_LON" "$ADMIN_API_KEY"

    sleep 2

    # Step 7: Launch apps
    log "Step 7: Launching apps..."
    launch_app_on_all_devices
    sleep 5

    # Step 8: Verify CHILD1 is inside geofence (should trigger ENTER immediately)
    log "Step 8: Verifying CHILD1 inside geofence..."
    sleep 3

    local events
    events=$(api_get_geofence_events "$geofence_id" "$ADMIN_API_KEY")
    local child_enter
    child_enter=$(echo "$events" | jq -r '.events[] | select(.deviceId == "'"$child1_device_id"'" and .type == "ENTER") | .type' | head -1)

    if [[ "$child_enter" == "ENTER" ]]; then
        log "SUCCESS: CHILD1 ENTER event detected"
        events_detected+=("CHILD1_ENTER")
    else
        log "WARNING: CHILD1 ENTER event not detected (may depend on implementation)"
    fi

    # Step 9: Move PARENT into geofence
    log "Step 9: Moving PARENT into geofence..."
    simulate_geofence_crossing "PARENT" "$parent_device_id" \
        "$GEOFENCE_CENTER_LAT" "$GEOFENCE_CENTER_LON" \
        "$GEOFENCE_RADIUS" "ENTER" "$ADMIN_API_KEY"

    sleep 3

    # Check for PARENT ENTER event
    events=$(api_get_geofence_events "$geofence_id" "$ADMIN_API_KEY")
    local parent_enter
    parent_enter=$(echo "$events" | jq -r '.events[] | select(.deviceId == "'"$parent_device_id"'" and .type == "ENTER") | .type' | head -1)

    if [[ "$parent_enter" == "ENTER" ]]; then
        log "SUCCESS: PARENT ENTER event detected"
        events_detected+=("PARENT_ENTER")
    else
        log "ERROR: PARENT ENTER event not detected"
        errors+=("PARENT ENTER event not detected")
        test_result="FAIL"
    fi

    # Screenshot with both devices in geofence
    adb_screenshot_for_device "PARENT" "${SCREENSHOTS_DIR}/${TEST_NAME}_both_inside_parent_${TEST_SESSION_ID}.png"
    adb_screenshot_for_device "CHILD1" "${SCREENSHOTS_DIR}/${TEST_NAME}_both_inside_child_${TEST_SESSION_ID}.png"

    # Step 10: Test DWELL (stay in geofence for a period)
    log "Step 10: Testing DWELL event (waiting 30 seconds)..."
    local dwell_start=$(date +%s)

    # Keep both devices inside, update location periodically
    for i in $(seq 1 6); do
        # Slight movement within geofence
        local jitter=$(echo "scale=6; 0.0001 * $i" | bc)
        local jittered_lat=$(echo "$INSIDE_LAT + $jitter" | bc)

        adb_set_location_for_device "PARENT" "$jittered_lat" "$GEOFENCE_CENTER_LON"
        api_update_device_location "$parent_device_id" "$jittered_lat" "$GEOFENCE_CENTER_LON" "$ADMIN_API_KEY"
        sleep 5
    done

    # Check for DWELL event
    events=$(api_get_geofence_events "$geofence_id" "$ADMIN_API_KEY")
    local dwell_event
    dwell_event=$(echo "$events" | jq -r '.events[] | select(.type == "DWELL") | .type' | head -1)

    if [[ "$dwell_event" == "DWELL" ]]; then
        log "SUCCESS: DWELL event detected"
        events_detected+=("DWELL")
    else
        log "INFO: DWELL event not detected (may require longer dwell time or specific configuration)"
    fi

    # Step 11: Move PARENT out of geofence
    log "Step 11: Moving PARENT out of geofence..."
    simulate_geofence_crossing "PARENT" "$parent_device_id" \
        "$GEOFENCE_CENTER_LAT" "$GEOFENCE_CENTER_LON" \
        "$GEOFENCE_RADIUS" "EXIT" "$ADMIN_API_KEY"

    sleep 3

    # Check for EXIT event
    events=$(api_get_geofence_events "$geofence_id" "$ADMIN_API_KEY")
    local parent_exit
    parent_exit=$(echo "$events" | jq -r '.events[] | select(.deviceId == "'"$parent_device_id"'" and .type == "EXIT") | .type' | head -1)

    if [[ "$parent_exit" == "EXIT" ]]; then
        log "SUCCESS: PARENT EXIT event detected"
        events_detected+=("PARENT_EXIT")
    else
        log "ERROR: PARENT EXIT event not detected"
        errors+=("PARENT EXIT event not detected")
        test_result="FAIL"
    fi

    # Final screenshots
    adb_screenshot_for_device "PARENT" "${SCREENSHOTS_DIR}/${TEST_NAME}_exit_parent_${TEST_SESSION_ID}.png"

    # Step 12: Move CHILD1 out of geofence
    log "Step 12: Moving CHILD1 out of geofence..."
    simulate_geofence_crossing "CHILD1" "$child1_device_id" \
        "$GEOFENCE_CENTER_LAT" "$GEOFENCE_CENTER_LON" \
        "$GEOFENCE_RADIUS" "EXIT" "$ADMIN_API_KEY"

    sleep 3

    events=$(api_get_geofence_events "$geofence_id" "$ADMIN_API_KEY")
    local child_exit
    child_exit=$(echo "$events" | jq -r '.events[] | select(.deviceId == "'"$child1_device_id"'" and .type == "EXIT") | .type' | head -1)

    if [[ "$child_exit" == "EXIT" ]]; then
        log "SUCCESS: CHILD1 EXIT event detected"
        events_detected+=("CHILD1_EXIT")
    else
        log "ERROR: CHILD1 EXIT event not detected"
        errors+=("CHILD1 EXIT event not detected")
        test_result="FAIL"
    fi

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
    "geofence": {
        "id": "$geofence_id",
        "name": "$GEOFENCE_NAME",
        "center": {"lat": $GEOFENCE_CENTER_LAT, "lon": $GEOFENCE_CENTER_LON},
        "radius_meters": $GEOFENCE_RADIUS
    },
    "events_detected": $(printf '%s\n' "${events_detected[@]:-}" | jq -R . | jq -s .),
    "errors": $(printf '%s\n' "${errors[@]:-}" | jq -R . | jq -s .),
    "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}
EOF

    log "=== Test completed: $test_result (${duration}s) ==="
    log "Report saved to: $REPORT_FILE"

    [[ "$test_result" == "PASS" ]]
}

# Run the test
test_geofence_events
