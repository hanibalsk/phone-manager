#!/bin/bash
# Multi-Device Proximity Alert Test Scenario
# Tests proximity alerts between devices (ENTER/EXIT events)
#
# Prerequisites:
# - Backend running at $API_BASE_URL
# - APK built and available
#
# Usage: ./multi_device_proximity.sh [--headless]

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
TEST_NAME="multi_device_proximity"
REPORT_FILE="${REPORTS_DIR}/${TEST_NAME}_${TEST_SESSION_ID}.json"

# Proximity test configuration
PROXIMITY_RADIUS=500  # meters
CENTER_LAT="37.7749"
CENTER_LON="-122.4194"
OUTSIDE_LAT="37.7800"  # ~500m north of center
INSIDE_LAT="37.7752"   # ~30m north of center

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$TEST_LOG"
}

cleanup() {
    log "Cleaning up..."
    shutdown_all_emulators 2>/dev/null || true
}

trap cleanup EXIT

# Helper to interpolate between two coordinates
interpolate() {
    local start=$1
    local end=$2
    local step=$3
    local total_steps=$4
    echo "scale=6; $start + ($end - $start) * $step / $total_steps" | bc
}

test_proximity_alerts() {
    log "=== Starting Multi-Device Proximity Alert Test ==="
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
    group_response=$(api_create_group "Proximity Test Group" "$ADMIN_API_KEY")
    local group_id
    group_id=$(echo "$group_response" | jq -r '.group.id')

    # Step 4: Register devices
    log "Step 4: Registering devices..."
    local parent_device_id child1_device_id

    parent_device_id=$(api_register_device "Parent Device" "PARENT" "$ADMIN_API_KEY" | jq -r '.device.id')
    child1_device_id=$(api_register_device "Child Device" "CHILD1" "$ADMIN_API_KEY" | jq -r '.device.id')

    # Step 5: Position CHILD1 at center (stationary), PARENT outside radius
    log "Step 5: Setting initial positions..."
    adb_set_location_for_device "CHILD1" "$CENTER_LAT" "$CENTER_LON"
    adb_set_location_for_device "PARENT" "$OUTSIDE_LAT" "$CENTER_LON"

    api_update_device_location "$child1_device_id" "$CENTER_LAT" "$CENTER_LON" "$ADMIN_API_KEY"
    api_update_device_location "$parent_device_id" "$OUTSIDE_LAT" "$CENTER_LON" "$ADMIN_API_KEY"

    sleep 2

    # Step 6: Create proximity alert for PARENT approaching CHILD1
    log "Step 6: Creating proximity alert..."
    local alert_response
    alert_response=$(api_create_proximity_alert "$child1_device_id" "$PROXIMITY_RADIUS" "$ADMIN_API_KEY")
    local alert_id
    alert_id=$(echo "$alert_response" | jq -r '.alert.id')

    if [[ -z "$alert_id" || "$alert_id" == "null" ]]; then
        log "ERROR: Failed to create proximity alert"
        errors+=("Alert creation failed")
        test_result="FAIL"
    else
        log "Created proximity alert: $alert_id (radius: ${PROXIMITY_RADIUS}m)"
    fi

    # Step 7: Launch apps
    log "Step 7: Launching apps..."
    launch_app_on_all_devices
    sleep 5

    # Step 8: Move PARENT towards CHILD1 (gradual approach)
    log "Step 8: Simulating approach (ENTER event)..."

    local total_steps=5
    for step in $(seq 0 $total_steps); do
        local current_lat
        current_lat=$(interpolate "$OUTSIDE_LAT" "$INSIDE_LAT" "$step" "$total_steps")

        log "  Step $step/$total_steps: Moving PARENT to ($current_lat, $CENTER_LON)"
        adb_set_location_for_device "PARENT" "$current_lat" "$CENTER_LON"
        api_update_device_location "$parent_device_id" "$current_lat" "$CENTER_LON" "$ADMIN_API_KEY"
        sleep 2
    done

    # Step 9: Check for ENTER event
    log "Step 9: Checking for ENTER event..."
    sleep 3  # Allow time for event processing

    local events
    events=$(api_get_proximity_events "$alert_id" "$ADMIN_API_KEY")
    local enter_event
    enter_event=$(echo "$events" | jq -r '.events[] | select(.type == "ENTER") | .type' | head -1)

    if [[ "$enter_event" == "ENTER" ]]; then
        log "SUCCESS: ENTER event detected"
        events_detected+=("ENTER")
    else
        log "ERROR: ENTER event not detected"
        errors+=("ENTER event not detected")
        test_result="FAIL"
    fi

    # Take screenshot at proximity
    adb_screenshot_for_device "PARENT" "${SCREENSHOTS_DIR}/${TEST_NAME}_enter_parent_${TEST_SESSION_ID}.png"
    adb_screenshot_for_device "CHILD1" "${SCREENSHOTS_DIR}/${TEST_NAME}_enter_child_${TEST_SESSION_ID}.png"

    # Step 10: Move PARENT away from CHILD1 (gradual departure)
    log "Step 10: Simulating departure (EXIT event)..."

    for step in $(seq 0 $total_steps); do
        local current_lat
        current_lat=$(interpolate "$INSIDE_LAT" "$OUTSIDE_LAT" "$step" "$total_steps")

        log "  Step $step/$total_steps: Moving PARENT to ($current_lat, $CENTER_LON)"
        adb_set_location_for_device "PARENT" "$current_lat" "$CENTER_LON"
        api_update_device_location "$parent_device_id" "$current_lat" "$CENTER_LON" "$ADMIN_API_KEY"
        sleep 2
    done

    # Step 11: Check for EXIT event
    log "Step 11: Checking for EXIT event..."
    sleep 3

    events=$(api_get_proximity_events "$alert_id" "$ADMIN_API_KEY")
    local exit_event
    exit_event=$(echo "$events" | jq -r '.events[] | select(.type == "EXIT") | .type' | head -1)

    if [[ "$exit_event" == "EXIT" ]]; then
        log "SUCCESS: EXIT event detected"
        events_detected+=("EXIT")
    else
        log "ERROR: EXIT event not detected"
        errors+=("EXIT event not detected")
        test_result="FAIL"
    fi

    # Take screenshot after exit
    adb_screenshot_for_device "PARENT" "${SCREENSHOTS_DIR}/${TEST_NAME}_exit_parent_${TEST_SESSION_ID}.png"

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
    "proximity_radius_meters": $PROXIMITY_RADIUS,
    "alert_id": "$alert_id",
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
test_proximity_alerts
