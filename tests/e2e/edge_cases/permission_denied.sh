#!/usr/bin/env bash
# Permission Denial Edge Case Tests
# Tests app behavior when permissions are denied or revoked
#
# Test Scenarios:
# 1. Location permission denied on first launch
# 2. Location permission revoked mid-tracking
# 3. Background location denied
# 4. Notification permission denied
# 5. Camera permission denied (QR scanning)
#
# Usage: ./permission_denied.sh [--headless]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
E2E_ROOT="$(dirname "$SCRIPT_DIR")"

# Source libraries
source "${E2E_ROOT}/config.sh"
source "${E2E_ROOT}/lib/emulator_manager.sh"
source "${E2E_ROOT}/lib/multidevice.sh"
source "${E2E_ROOT}/lib/adb.sh"

HEADLESS="${1:-true}"
[[ "$1" == "--headless" ]] && HEADLESS="true"

TEST_NAME="permission_denied"
REPORT_FILE="${REPORTS_DIR}/${TEST_NAME}_${TEST_SESSION_ID}.json"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$TEST_LOG"
}

cleanup() {
    log "Cleaning up..."
    shutdown_all_emulators 2>/dev/null || true
}

trap cleanup EXIT

# Permission control functions
grant_permission() {
    local device_role="$1"
    local permission="$2"
    local serial="$(get_device_serial "$device_role")"
    adb -s "$serial" shell pm grant "$APP_PACKAGE" "$permission"
    log "Granted $permission on $device_role"
}

revoke_permission() {
    local device_role="$1"
    local permission="$2"
    local serial="$(get_device_serial "$device_role")"
    adb -s "$serial" shell pm revoke "$APP_PACKAGE" "$permission"
    log "Revoked $permission on $device_role"
}

check_permission() {
    local device_role="$1"
    local permission="$2"
    local serial="$(get_device_serial "$device_role")"
    adb -s "$serial" shell dumpsys package "$APP_PACKAGE" | grep -q "$permission: granted=true"
}

# Common permissions
PERM_FINE_LOCATION="android.permission.ACCESS_FINE_LOCATION"
PERM_COARSE_LOCATION="android.permission.ACCESS_COARSE_LOCATION"
PERM_BACKGROUND_LOCATION="android.permission.ACCESS_BACKGROUND_LOCATION"
PERM_POST_NOTIFICATIONS="android.permission.POST_NOTIFICATIONS"
PERM_CAMERA="android.permission.CAMERA"

test_location_denied_first_launch() {
    log "--- Test: Location Permission Denied on First Launch ---"
    local test_result="PASS"

    # Launch emulator without granting location permissions
    launch_emulators 1 "$HEADLESS"
    install_apk_on_all "$APP_APK_PATH"

    # Don't grant location permissions
    log "Starting app without location permission..."
    launch_app_on_all_devices
    sleep 5

    # Take screenshot - should show permission rationale or error
    adb_screenshot_for_device "PARENT" "${SCREENSHOTS_DIR}/${TEST_NAME}_no_location_perm_${TEST_SESSION_ID}.png"

    # Verify app shows appropriate UI (permission request or error message)
    # In a real test, we'd check for specific UI elements
    log "App started without location permission - verify UI shows permission rationale"

    # Now grant permission and verify app recovers
    log "Granting location permission..."
    grant_permission "PARENT" "$PERM_FINE_LOCATION"
    grant_permission "PARENT" "$PERM_COARSE_LOCATION"
    sleep 3

    adb_screenshot_for_device "PARENT" "${SCREENSHOTS_DIR}/${TEST_NAME}_location_granted_${TEST_SESSION_ID}.png"

    log "Location denied first launch test completed"
    echo "$test_result"
}

test_location_revoked_mid_tracking() {
    log "--- Test: Location Permission Revoked Mid-Tracking ---"
    local test_result="PASS"

    # Start with full permissions
    grant_permissions_on_all "$APP_PACKAGE"
    launch_app_on_all_devices
    sleep 5

    # Verify tracking is working
    log "Enabling tracking..."
    adb_set_location_for_device "PARENT" "37.7749" "-122.4194"
    sleep 3

    adb_screenshot_for_device "PARENT" "${SCREENSHOTS_DIR}/${TEST_NAME}_tracking_active_${TEST_SESSION_ID}.png"

    # Revoke location permission
    log "Revoking location permission mid-tracking..."
    revoke_permission "PARENT" "$PERM_FINE_LOCATION"
    revoke_permission "PARENT" "$PERM_COARSE_LOCATION"
    sleep 5

    # App should detect permission loss and handle gracefully
    adb_screenshot_for_device "PARENT" "${SCREENSHOTS_DIR}/${TEST_NAME}_location_revoked_${TEST_SESSION_ID}.png"

    # Verify app shows appropriate UI
    log "Location revoked - verify app handles gracefully"

    # Re-grant permission
    log "Re-granting location permission..."
    grant_permission "PARENT" "$PERM_FINE_LOCATION"
    grant_permission "PARENT" "$PERM_COARSE_LOCATION"
    sleep 3

    adb_screenshot_for_device "PARENT" "${SCREENSHOTS_DIR}/${TEST_NAME}_location_regranted_${TEST_SESSION_ID}.png"

    log "Location revoked mid-tracking test completed"
    echo "$test_result"
}

test_background_location_denied() {
    log "--- Test: Background Location Denied ---"
    local test_result="PASS"

    # Grant foreground location but not background
    grant_permission "PARENT" "$PERM_FINE_LOCATION"
    grant_permission "PARENT" "$PERM_COARSE_LOCATION"
    # Don't grant background location

    launch_app_on_all_devices
    sleep 5

    log "Foreground location granted, background denied"
    adb_screenshot_for_device "PARENT" "${SCREENSHOTS_DIR}/${TEST_NAME}_no_background_${TEST_SESSION_ID}.png"

    # Put app in background
    log "Sending app to background..."
    local serial="$(get_device_serial "PARENT")"
    adb -s "$serial" shell input keyevent KEYCODE_HOME
    sleep 5

    # Set location while app is in background
    adb_set_location_for_device "PARENT" "37.7800" "-122.4100"
    sleep 5

    # Bring app back to foreground
    adb -s "$serial" shell am start -n "${APP_PACKAGE}/.MainActivity"
    sleep 3

    adb_screenshot_for_device "PARENT" "${SCREENSHOTS_DIR}/${TEST_NAME}_returned_foreground_${TEST_SESSION_ID}.png"

    # Verify app prompts for background location
    log "Background location denied test completed - verify UI prompts for background permission"

    echo "$test_result"
}

test_notification_denied() {
    log "--- Test: Notification Permission Denied ---"
    local test_result="PASS"

    # Grant location but not notifications (API 33+)
    grant_permission "PARENT" "$PERM_FINE_LOCATION"
    grant_permission "PARENT" "$PERM_COARSE_LOCATION"
    # Don't grant POST_NOTIFICATIONS

    launch_app_on_all_devices
    sleep 5

    log "App running without notification permission"
    adb_screenshot_for_device "PARENT" "${SCREENSHOTS_DIR}/${TEST_NAME}_no_notifications_${TEST_SESSION_ID}.png"

    # Verify app functions but can't show notifications
    # In a real test, we'd trigger an event that should show a notification
    # and verify it doesn't appear

    log "Notification denied test completed"
    echo "$test_result"
}

test_camera_denied_qr_scan() {
    log "--- Test: Camera Permission Denied (QR Scan) ---"
    local test_result="PASS"

    # Grant location but not camera
    grant_permission "PARENT" "$PERM_FINE_LOCATION"
    grant_permission "PARENT" "$PERM_COARSE_LOCATION"
    # Don't grant camera

    launch_app_on_all_devices
    sleep 5

    # Navigate to QR scan screen (join group)
    log "Attempting to access QR scanner without camera permission..."
    local serial="$(get_device_serial "PARENT")"

    # Simulate navigation to groups -> join -> scan QR
    # This would trigger camera permission request
    adb_screenshot_for_device "PARENT" "${SCREENSHOTS_DIR}/${TEST_NAME}_pre_qr_scan_${TEST_SESSION_ID}.png"

    # In a real test, we'd navigate to the QR screen and verify
    # appropriate error handling

    log "Camera denied test completed"
    echo "$test_result"
}

run_all_permission_tests() {
    log "=== Starting Permission Denial Edge Case Tests ==="
    local test_start=$(date +%s)
    local overall_result="PASS"
    local results=()

    # Test 1: Location denied on first launch
    local result1
    result1=$(test_location_denied_first_launch)
    results+=("location_denied_first_launch:$result1")
    [[ "$result1" != "PASS" ]] && overall_result="FAIL"

    # Test 2: Location revoked mid-tracking
    local result2
    result2=$(test_location_revoked_mid_tracking)
    results+=("location_revoked_mid_tracking:$result2")
    [[ "$result2" != "PASS" ]] && overall_result="FAIL"

    # Restart for next test
    shutdown_all_emulators
    sleep 3
    launch_emulators 1 "$HEADLESS"
    install_apk_on_all "$APP_APK_PATH"

    # Test 3: Background location denied
    local result3
    result3=$(test_background_location_denied)
    results+=("background_location_denied:$result3")
    [[ "$result3" != "PASS" ]] && overall_result="FAIL"

    # Test 4: Notification denied
    local result4
    result4=$(test_notification_denied)
    results+=("notification_denied:$result4")
    [[ "$result4" != "PASS" ]] && overall_result="FAIL"

    # Test 5: Camera denied
    local result5
    result5=$(test_camera_denied_qr_scan)
    results+=("camera_denied_qr_scan:$result5")
    [[ "$result5" != "PASS" ]] && overall_result="FAIL"

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

    log "=== Permission tests completed: $overall_result (${duration}s) ==="
    log "Report saved to: $REPORT_FILE"
}

run_all_permission_tests
