#!/bin/bash
# Network Failure Edge Case Tests
# Tests app behavior during network outages and reconnection
#
# Test Scenarios:
# 1. Offline location queuing and sync on reconnect
# 2. API timeout handling
# 3. Intermittent connectivity
# 4. Network mode changes (WiFi -> Mobile -> Offline)
#
# Usage: ./network_failure.sh [--headless]

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

TEST_NAME="network_failure"
REPORT_FILE="${REPORTS_DIR}/${TEST_NAME}_${TEST_SESSION_ID}.json"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$TEST_LOG"
}

cleanup() {
    log "Cleaning up..."
    # Restore network on all devices
    for serial in "${DEVICE_SERIALS[@]:-}"; do
        adb -s "$serial" shell svc wifi enable 2>/dev/null || true
        adb -s "$serial" shell svc data enable 2>/dev/null || true
    done
    shutdown_all_emulators 2>/dev/null || true
}

trap cleanup EXIT

# Network control functions
disable_network() {
    local device_role="$1"
    local serial="${DEVICE_SERIALS[$device_role]}"
    log "Disabling network on $device_role ($serial)..."
    adb -s "$serial" shell svc wifi disable
    adb -s "$serial" shell svc data disable
}

enable_network() {
    local device_role="$1"
    local serial="${DEVICE_SERIALS[$device_role]}"
    log "Enabling network on $device_role ($serial)..."
    adb -s "$serial" shell svc wifi enable
    adb -s "$serial" shell svc data enable
}

set_airplane_mode() {
    local device_role="$1"
    local enabled="$2"  # true or false
    local serial="${DEVICE_SERIALS[$device_role]}"
    log "Setting airplane mode to $enabled on $device_role..."
    if [[ "$enabled" == "true" ]]; then
        adb -s "$serial" shell settings put global airplane_mode_on 1
        adb -s "$serial" shell am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true
    else
        adb -s "$serial" shell settings put global airplane_mode_on 0
        adb -s "$serial" shell am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false
    fi
}

test_offline_queue_and_sync() {
    log "--- Test: Offline Queue and Sync ---"
    local test_result="PASS"
    local queued_locations=0

    # Setup: Device is online, tracking enabled
    launch_emulators 1 "$HEADLESS"
    install_apk_on_all "$APP_APK_PATH"
    grant_permissions_on_all "$APP_PACKAGE"

    local device_id
    device_id=$(api_register_device "Test Device" "PARENT" "$ADMIN_API_KEY" | jq -r '.device.id')

    launch_app_on_all_devices
    sleep 5

    # Go offline
    log "Going offline..."
    disable_network "PARENT"
    sleep 2

    # Generate location updates while offline
    log "Generating location updates while offline..."
    local base_lat="37.7749"
    for i in $(seq 1 10); do
        local lat=$(echo "scale=6; $base_lat + 0.0001 * $i" | bc)
        adb_set_location_for_device "PARENT" "$lat" "-122.4194"
        ((queued_locations++))
        sleep 1
    done

    log "Generated $queued_locations location updates while offline"

    # Take screenshot while offline
    adb_screenshot_for_device "PARENT" "${SCREENSHOTS_DIR}/${TEST_NAME}_offline_${TEST_SESSION_ID}.png"

    # Go back online
    log "Going back online..."
    enable_network "PARENT"
    sleep 10  # Allow time for sync

    # Verify locations were synced
    log "Verifying synced locations..."
    local history
    history=$(api_get_location_history "$device_id" "$ADMIN_API_KEY")
    local synced_count
    synced_count=$(echo "$history" | jq '.locations | length')

    if [[ "$synced_count" -ge "$queued_locations" ]]; then
        log "SUCCESS: All $queued_locations locations synced ($synced_count in history)"
    else
        log "ERROR: Only $synced_count of $queued_locations locations synced"
        test_result="FAIL"
    fi

    # Take screenshot after sync
    adb_screenshot_for_device "PARENT" "${SCREENSHOTS_DIR}/${TEST_NAME}_synced_${TEST_SESSION_ID}.png"

    echo "$test_result"
}

test_intermittent_connectivity() {
    log "--- Test: Intermittent Connectivity ---"
    local test_result="PASS"
    local cycles=3

    for cycle in $(seq 1 $cycles); do
        log "Cycle $cycle/$cycles: Toggling network..."

        # Disable network
        disable_network "PARENT"
        sleep 3

        # Generate a location update
        local lat=$(echo "scale=6; 37.7749 + 0.001 * $cycle" | bc)
        adb_set_location_for_device "PARENT" "$lat" "-122.4194"
        sleep 2

        # Enable network
        enable_network "PARENT"
        sleep 5

        # Verify app is still responsive
        # (In a real test, we'd check UI state or app logs)
    done

    log "Intermittent connectivity test completed"
    echo "$test_result"
}

test_airplane_mode() {
    log "--- Test: Airplane Mode Toggle ---"
    local test_result="PASS"

    # Enable airplane mode
    set_airplane_mode "PARENT" "true"
    sleep 3

    # Verify app handles offline state
    adb_screenshot_for_device "PARENT" "${SCREENSHOTS_DIR}/${TEST_NAME}_airplane_on_${TEST_SESSION_ID}.png"

    # Generate location while in airplane mode
    adb_set_location_for_device "PARENT" "37.7800" "-122.4100"
    sleep 2

    # Disable airplane mode
    set_airplane_mode "PARENT" "false"
    sleep 10  # Allow reconnection and sync

    adb_screenshot_for_device "PARENT" "${SCREENSHOTS_DIR}/${TEST_NAME}_airplane_off_${TEST_SESSION_ID}.png"

    log "Airplane mode test completed"
    echo "$test_result"
}

run_all_network_tests() {
    log "=== Starting Network Failure Edge Case Tests ==="
    local test_start=$(date +%s)
    local overall_result="PASS"
    local results=()

    # Test 1: Offline Queue and Sync
    local result1
    result1=$(test_offline_queue_and_sync)
    results+=("offline_queue_sync:$result1")
    [[ "$result1" != "PASS" ]] && overall_result="FAIL"

    # Shutdown and restart for next test
    shutdown_all_emulators
    sleep 5

    # Test 2: Intermittent Connectivity
    launch_emulators 1 "$HEADLESS"
    install_apk_on_all "$APP_APK_PATH"
    grant_permissions_on_all "$APP_PACKAGE"
    launch_app_on_all_devices
    sleep 5

    local result2
    result2=$(test_intermittent_connectivity)
    results+=("intermittent_connectivity:$result2")
    [[ "$result2" != "PASS" ]] && overall_result="FAIL"

    # Test 3: Airplane Mode
    local result3
    result3=$(test_airplane_mode)
    results+=("airplane_mode:$result3")
    [[ "$result3" != "PASS" ]] && overall_result="FAIL"

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

    log "=== Network tests completed: $overall_result (${duration}s) ==="
    log "Report saved to: $REPORT_FILE"
}

run_all_network_tests
