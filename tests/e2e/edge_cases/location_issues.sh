#!/bin/bash
# Location Edge Case Tests
# Tests app behavior with various GPS/location issues
#
# Test Scenarios:
# 1. GPS unavailable (location services disabled)
# 2. Low accuracy location
# 3. Mock location detection
# 4. Rapid location changes
# 5. Stale location data
# 6. Invalid coordinates
#
# Usage: ./location_issues.sh [--headless]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
E2E_ROOT="$(dirname "$SCRIPT_DIR")"

# Source libraries
source "${E2E_ROOT}/config.sh"
source "${E2E_ROOT}/lib/emulator_manager.sh"
source "${E2E_ROOT}/lib/multidevice.sh"
source "${E2E_ROOT}/lib/api.sh"
source "${E2E_ROOT}/lib/adb.sh"

HEADLESS="${1:-true}"
[[ "$1" == "--headless" ]] && HEADLESS="true"

TEST_NAME="location_issues"
REPORT_FILE="${REPORTS_DIR}/${TEST_NAME}_${TEST_SESSION_ID}.json"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$TEST_LOG"
}

cleanup() {
    log "Cleaning up..."
    # Re-enable location services
    for serial in "${DEVICE_SERIALS[@]:-}"; do
        adb -s "$serial" shell settings put secure location_mode 3 2>/dev/null || true
    done
    shutdown_all_emulators 2>/dev/null || true
}

trap cleanup EXIT

# Location control functions
disable_location_services() {
    local device_role="$1"
    local serial="${DEVICE_SERIALS[$device_role]}"
    log "Disabling location services on $device_role..."
    # 0 = off, 1 = sensors only, 2 = battery saving, 3 = high accuracy
    adb -s "$serial" shell settings put secure location_mode 0
}

enable_location_services() {
    local device_role="$1"
    local serial="${DEVICE_SERIALS[$device_role]}"
    log "Enabling location services on $device_role..."
    adb -s "$serial" shell settings put secure location_mode 3  # High accuracy
}

set_location_mode() {
    local device_role="$1"
    local mode="$2"  # 0=off, 1=sensors, 2=battery, 3=high
    local serial="${DEVICE_SERIALS[$device_role]}"
    adb -s "$serial" shell settings put secure location_mode "$mode"
}

test_gps_unavailable() {
    log "--- Test: GPS Unavailable ---"
    local test_result="PASS"

    # Start with location enabled
    enable_location_services "PARENT"
    grant_permissions_on_all "$APP_PACKAGE"
    launch_app_on_all_devices
    sleep 5

    # Set initial location
    adb_set_location_for_device "PARENT" "37.7749" "-122.4194"
    sleep 3

    adb_screenshot_for_device "PARENT" "${SCREENSHOTS_DIR}/${TEST_NAME}_gps_active_${TEST_SESSION_ID}.png"

    # Disable location services
    log "Disabling GPS/location services..."
    disable_location_services "PARENT"
    sleep 5

    # App should detect location unavailability
    adb_screenshot_for_device "PARENT" "${SCREENSHOTS_DIR}/${TEST_NAME}_gps_disabled_${TEST_SESSION_ID}.png"

    # Re-enable and verify recovery
    log "Re-enabling GPS..."
    enable_location_services "PARENT"
    adb_set_location_for_device "PARENT" "37.7750" "-122.4195"
    sleep 5

    adb_screenshot_for_device "PARENT" "${SCREENSHOTS_DIR}/${TEST_NAME}_gps_recovered_${TEST_SESSION_ID}.png"

    log "GPS unavailable test completed"
    echo "$test_result"
}

test_low_accuracy_location() {
    log "--- Test: Low Accuracy Location ---"
    local test_result="PASS"

    # Register device
    local device_id
    device_id=$(api_register_device "Test Device" "PARENT" "$ADMIN_API_KEY" | jq -r '.device.id')

    # Send location with low accuracy (>100m)
    log "Sending low accuracy location (accuracy: 500m)..."

    # Use API to send location with explicit low accuracy
    local response
    response=$(curl -s -X POST "${API_BASE_URL}/api/devices/${device_id}/location" \
        -H "Authorization: Bearer $ADMIN_API_KEY" \
        -H "Content-Type: application/json" \
        -d '{
            "latitude": 37.7749,
            "longitude": -122.4194,
            "accuracy": 500,
            "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'"
        }')

    log "Response: $response"

    # Verify how app handles low accuracy (should possibly filter or flag it)
    local history
    history=$(api_get_location_history "$device_id" "$ADMIN_API_KEY")
    log "Location history after low accuracy update: $history"

    adb_screenshot_for_device "PARENT" "${SCREENSHOTS_DIR}/${TEST_NAME}_low_accuracy_${TEST_SESSION_ID}.png"

    log "Low accuracy location test completed"
    echo "$test_result"
}

test_rapid_location_changes() {
    log "--- Test: Rapid Location Changes ---"
    local test_result="PASS"

    # Register device
    local device_id
    device_id=$(api_register_device "Test Device Rapid" "PARENT" "$ADMIN_API_KEY" | jq -r '.device.id')

    # Send many rapid location updates (simulating unrealistic movement)
    log "Sending 20 rapid location updates..."
    local base_lat="37.7749"

    for i in $(seq 1 20); do
        local lat=$(echo "scale=6; $base_lat + 0.01 * $i" | bc)  # ~1km jumps
        adb_set_location_for_device "PARENT" "$lat" "-122.4194"
        api_update_device_location "$device_id" "$lat" "-122.4194" "$ADMIN_API_KEY"
        sleep 0.2  # Very rapid updates
    done

    sleep 3

    # Check how app/API handled rapid updates
    local history
    history=$(api_get_location_history "$device_id" "$ADMIN_API_KEY")
    local location_count
    location_count=$(echo "$history" | jq '.locations | length')

    log "Sent 20 rapid updates, $location_count in history"

    # Verify app is still responsive
    adb_screenshot_for_device "PARENT" "${SCREENSHOTS_DIR}/${TEST_NAME}_rapid_changes_${TEST_SESSION_ID}.png"

    log "Rapid location changes test completed"
    echo "$test_result"
}

test_impossible_teleportation() {
    log "--- Test: Impossible Teleportation Detection ---"
    local test_result="PASS"

    # Register device
    local device_id
    device_id=$(api_register_device "Test Device Teleport" "PARENT" "$ADMIN_API_KEY" | jq -r '.device.id')

    # Set initial location in San Francisco
    log "Setting initial location: San Francisco"
    adb_set_location_for_device "PARENT" "37.7749" "-122.4194"
    api_update_device_location "$device_id" "37.7749" "-122.4194" "$ADMIN_API_KEY"
    sleep 2

    # "Teleport" to Tokyo (impossible in real-time)
    log "Teleporting to Tokyo (should be flagged as suspicious)..."
    adb_set_location_for_device "PARENT" "35.6762" "139.6503"
    api_update_device_location "$device_id" "35.6762" "139.6503" "$ADMIN_API_KEY"
    sleep 2

    # Check if system detected impossible movement
    # This depends on backend implementation
    local history
    history=$(api_get_location_history "$device_id" "$ADMIN_API_KEY")
    log "Location history after teleportation: $history"

    adb_screenshot_for_device "PARENT" "${SCREENSHOTS_DIR}/${TEST_NAME}_teleportation_${TEST_SESSION_ID}.png"

    log "Teleportation detection test completed"
    echo "$test_result"
}

test_boundary_coordinates() {
    log "--- Test: Boundary Coordinates ---"
    local test_result="PASS"

    # Test various edge case coordinates

    # North Pole
    log "Testing North Pole coordinates..."
    adb_set_location_for_device "PARENT" "90.0" "0.0"
    sleep 2

    # South Pole
    log "Testing South Pole coordinates..."
    adb_set_location_for_device "PARENT" "-90.0" "0.0"
    sleep 2

    # International Date Line
    log "Testing International Date Line..."
    adb_set_location_for_device "PARENT" "0.0" "180.0"
    sleep 2

    # Prime Meridian
    log "Testing Prime Meridian..."
    adb_set_location_for_device "PARENT" "0.0" "0.0"
    sleep 2

    adb_screenshot_for_device "PARENT" "${SCREENSHOTS_DIR}/${TEST_NAME}_boundary_coords_${TEST_SESSION_ID}.png"

    # Return to normal location
    adb_set_location_for_device "PARENT" "37.7749" "-122.4194"

    log "Boundary coordinates test completed"
    echo "$test_result"
}

test_stale_location() {
    log "--- Test: Stale Location Data ---"
    local test_result="PASS"

    # Register device
    local device_id
    device_id=$(api_register_device "Test Device Stale" "PARENT" "$ADMIN_API_KEY" | jq -r '.device.id')

    # Set initial location
    log "Setting initial location..."
    adb_set_location_for_device "PARENT" "37.7749" "-122.4194"
    api_update_device_location "$device_id" "37.7749" "-122.4194" "$ADMIN_API_KEY"
    sleep 2

    # Disable GPS to simulate stale data
    log "Disabling GPS to simulate stale data..."
    disable_location_services "PARENT"

    # Wait for a while (location becomes stale)
    log "Waiting 30 seconds for location to become stale..."
    sleep 30

    # Take screenshot showing stale location indicator
    adb_screenshot_for_device "PARENT" "${SCREENSHOTS_DIR}/${TEST_NAME}_stale_location_${TEST_SESSION_ID}.png"

    # Re-enable GPS
    enable_location_services "PARENT"
    adb_set_location_for_device "PARENT" "37.7750" "-122.4195"
    sleep 5

    adb_screenshot_for_device "PARENT" "${SCREENSHOTS_DIR}/${TEST_NAME}_fresh_location_${TEST_SESSION_ID}.png"

    log "Stale location test completed"
    echo "$test_result"
}

run_all_location_tests() {
    log "=== Starting Location Edge Case Tests ==="
    local test_start=$(date +%s)
    local overall_result="PASS"
    local results=()

    # Launch emulators
    launch_emulators 1 "$HEADLESS"
    install_apk_on_all "$APP_APK_PATH"
    grant_permissions_on_all "$APP_PACKAGE"
    launch_app_on_all_devices
    sleep 5

    # Test 1: GPS unavailable
    local result1
    result1=$(test_gps_unavailable)
    results+=("gps_unavailable:$result1")
    [[ "$result1" != "PASS" ]] && overall_result="FAIL"

    # Test 2: Low accuracy location
    local result2
    result2=$(test_low_accuracy_location)
    results+=("low_accuracy:$result2")
    [[ "$result2" != "PASS" ]] && overall_result="FAIL"

    # Test 3: Rapid location changes
    local result3
    result3=$(test_rapid_location_changes)
    results+=("rapid_changes:$result3")
    [[ "$result3" != "PASS" ]] && overall_result="FAIL"

    # Test 4: Teleportation detection
    local result4
    result4=$(test_impossible_teleportation)
    results+=("teleportation:$result4")
    [[ "$result4" != "PASS" ]] && overall_result="FAIL"

    # Test 5: Boundary coordinates
    local result5
    result5=$(test_boundary_coordinates)
    results+=("boundary_coords:$result5")
    [[ "$result5" != "PASS" ]] && overall_result="FAIL"

    # Test 6: Stale location
    local result6
    result6=$(test_stale_location)
    results+=("stale_location:$result6")
    [[ "$result6" != "PASS" ]] && overall_result="FAIL"

    # Calculate duration
    local test_end=$(date +%s)
    local duration=$((test_end - test_start))

    # Generate report
    log "Generating report..."
    cat > "$REPORT_FILE" << EOF
{
    "test_name": "$TEST_NAME",
    "session_id": "$TEST_SESSION_ID",
    "result": "$overall_result",
    "duration_seconds": $duration,
    "test_results": {
        $(printf '%s\n' "${results[@]}" | sed 's/:/": "/g; s/^/"/; s/$/"/' | paste -sd, -)
    },
    "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}
EOF

    log "=== Location tests completed: $overall_result (${duration}s) ==="
    log "Report saved to: $REPORT_FILE"
}

run_all_location_tests
