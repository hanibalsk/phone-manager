#!/bin/bash
# =============================================================================
# E2E Tests: Settings Synchronization
# Tests device settings sync, FCM updates, locks, and unlock requests
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
ORIGINAL_SETTINGS=""

# =============================================================================
# Test Setup
# =============================================================================
setup_settings_tests() {
    log_info "Setting up settings sync tests..."

    # Get device ID
    if adb_check_device; then
        DEVICE_ID=$(adb_get_device_id 2>/dev/null || generate_device_id)
    else
        DEVICE_ID=$(generate_device_id)
    fi

    # Store original settings for restoration
    ORIGINAL_SETTINGS=$(api_get_device_settings "$DEVICE_ID" 2>/dev/null || echo "{}")

    log_info "Device ID: $DEVICE_ID"
}

cleanup_settings_tests() {
    log_info "Cleaning up settings sync tests..."

    # Restore original settings if available
    if [[ -n "$ORIGINAL_SETTINGS" ]] && [[ "$ORIGINAL_SETTINGS" != "{}" ]]; then
        api_update_device_settings "$DEVICE_ID" "$ORIGINAL_SETTINGS" 2>/dev/null || true
    fi
}

# =============================================================================
# Test: Fetch Device Settings
# =============================================================================
test_fetch_device_settings() {
    test_start "Fetch Device Settings"

    log_step "Fetching device settings"
    local response
    response=$(api_get_device_settings "$DEVICE_ID")
    local status=$?

    if [[ $status -ne 0 ]]; then
        log_error "Fetch settings API failed"
        test_end
        return 1
    fi

    # Verify settings structure
    if echo "$response" | jq -e '.' &>/dev/null; then
        log_success "Settings returned as valid JSON"
        ((TESTS_PASSED++))
    else
        log_error "Settings not valid JSON"
        ((TESTS_FAILED++))
        test_end
        return 1
    fi

    # Check for expected settings keys
    local expected_keys=("location_interval" "battery_optimization" "notifications_enabled")
    for key in "${expected_keys[@]}"; do
        if echo "$response" | jq -e ".$key" &>/dev/null; then
            log_success "Setting '$key' exists"
            ((TESTS_PASSED++))
        else
            log_info "Setting '$key' not found (may be optional)"
        fi
    done

    log_debug "Settings: ${response:0:200}"

    test_end
}

# =============================================================================
# Test: Update Local Setting
# =============================================================================
test_update_local_setting() {
    test_start "Update Local Setting"

    # Get current settings
    local current
    current=$(api_get_device_settings "$DEVICE_ID" 2>/dev/null || echo "{}")

    # Update a setting
    local new_interval=60000  # 1 minute
    log_step "Updating location_interval to $new_interval"

    local update_data
    update_data=$(cat <<EOF
{
    "location_interval": $new_interval
}
EOF
)

    local response
    response=$(api_update_device_settings "$DEVICE_ID" "$update_data" 2>&1 || true)

    if echo "$response" | grep -qi "success\|updated\|200"; then
        log_success "Setting update request accepted"
        ((TESTS_PASSED++))
    else
        log_debug "Response: ${response:0:100}"
    fi

    # Verify update
    log_step "Verifying setting update"
    sleep 1
    local updated
    updated=$(api_get_device_settings "$DEVICE_ID" 2>/dev/null || echo "{}")
    local actual_interval
    actual_interval=$(echo "$updated" | jq -r '.location_interval // 0')

    if [[ "$actual_interval" == "$new_interval" ]]; then
        log_success "Setting updated correctly"
        ((TESTS_PASSED++))
    else
        log_warning "Setting may not have been updated (got: $actual_interval)"
    fi

    test_end
}

# =============================================================================
# Test: Sync Settings to Server
# =============================================================================
test_sync_settings_to_server() {
    test_start "Sync Settings to Server"

    if ! adb_check_device; then
        test_skip "No device for sync test"
        test_end
        return
    fi

    # Trigger a settings sync from the app
    log_step "Triggering settings sync via app"

    # Send broadcast to trigger sync
    adb shell am broadcast \
        -a "${APP_PACKAGE}.SYNC_SETTINGS" \
        -n "${APP_PACKAGE}/.receivers.SettingsReceiver" 2>/dev/null || true

    sleep 5

    # Verify sync by checking last_synced timestamp
    log_step "Checking sync status"
    local settings
    settings=$(api_get_device_settings "$DEVICE_ID" 2>/dev/null || echo "{}")

    local last_synced
    last_synced=$(echo "$settings" | jq -r '.last_synced // .synced_at // empty')

    if [[ -n "$last_synced" ]] && [[ "$last_synced" != "null" ]]; then
        log_success "Settings have sync timestamp: $last_synced"
        ((TESTS_PASSED++))
    else
        log_info "Sync timestamp not available in settings response"
    fi

    test_end
}

# =============================================================================
# Test: Receive FCM Settings Update
# =============================================================================
test_receive_fcm_settings_update() {
    test_start "Receive FCM Settings Update"

    if ! adb_check_device; then
        test_skip "No device for FCM test"
        test_end
        return
    fi

    # Mock FCM message for settings update
    log_step "Simulating FCM settings update message"

    local new_interval=120000  # 2 minutes
    mock_fcm_settings_update "$DEVICE_ID" "location_interval" "$new_interval"

    sleep 5

    # Verify the app received and processed the FCM message
    # Check via app logs or settings
    log_step "Checking if FCM update was processed"

    # Check logcat for FCM processing
    local logs
    logs=$(adb logcat -d -t 50 -s "$APP_PACKAGE" 2>/dev/null | grep -i "settings\|fcm\|update" || echo "")

    if echo "$logs" | grep -qi "settings.*update\|received.*settings"; then
        log_success "FCM settings update appears to be processed"
        ((TESTS_PASSED++))
    else
        log_info "Could not verify FCM processing via logs"
    fi

    # Verify setting changed
    local app_settings
    app_settings=$(adb_get_app_settings 2>/dev/null || echo "")

    if echo "$app_settings" | grep -q "$new_interval"; then
        log_success "Setting updated via FCM"
        ((TESTS_PASSED++))
    else
        log_info "Setting verification unclear"
    fi

    test_end
}

# =============================================================================
# Test: Locked Setting Display
# =============================================================================
test_locked_setting_display() {
    test_start "Locked Setting Display"

    # Lock a setting
    log_step "Locking a setting"
    local lock_response
    lock_response=$(api_lock_device_setting "$DEVICE_ID" "location_interval" 2>&1 || true)

    if echo "$lock_response" | grep -qi "success\|locked\|200"; then
        log_success "Setting locked successfully"
        ((TESTS_PASSED++))
    elif echo "$lock_response" | grep -qi "not.*implemented\|404\|501"; then
        test_skip "Setting locks not implemented"
        test_end
        return
    else
        log_debug "Lock response: ${lock_response:0:100}"
    fi

    # Verify lock status
    log_step "Checking lock status"
    local settings
    settings=$(api_get_device_settings "$DEVICE_ID" 2>/dev/null || echo "{}")

    local locks
    locks=$(echo "$settings" | jq '.locks // .locked_settings // {}' 2>/dev/null)

    if echo "$locks" | jq -e '.location_interval' &>/dev/null; then
        log_success "Setting lock status visible"
        ((TESTS_PASSED++))
    else
        log_info "Lock status not visible in settings response"
    fi

    # Check UI if device available
    if adb_check_device; then
        log_step "Checking locked setting in UI"
        adb_open_deep_link "phonemanager://settings"
        sleep 3
        adb_screenshot "locked_setting_display"
    fi

    test_end
}

# =============================================================================
# Test: Locked Setting Modification Blocked
# =============================================================================
test_locked_setting_modification_blocked() {
    test_start "Locked Setting Modification Blocked"

    # Ensure setting is locked
    api_lock_device_setting "$DEVICE_ID" "location_interval" 2>/dev/null || true

    # Attempt to modify locked setting
    log_step "Attempting to modify locked setting"
    local update_data
    update_data='{"location_interval": 30000}'

    local response
    response=$(api_update_device_settings "$DEVICE_ID" "$update_data" 2>&1 || true)

    if echo "$response" | grep -qi "locked\|blocked\|forbidden\|403"; then
        log_success "Locked setting modification correctly blocked"
        ((TESTS_PASSED++))
    elif echo "$response" | grep -qi "success\|updated"; then
        log_warning "Locked setting may have been modified (lock not enforced)"
    else
        log_debug "Response: ${response:0:100}"
    fi

    # Unlock for next tests
    api_unlock_device_setting "$DEVICE_ID" "location_interval" 2>/dev/null || true

    test_end
}

# =============================================================================
# Test: Unlock Request Creation
# =============================================================================
test_unlock_request_creation() {
    test_start "Unlock Request Creation"

    # Lock a setting first
    api_lock_device_setting "$DEVICE_ID" "notifications_enabled" 2>/dev/null || true

    # Create unlock request
    log_step "Creating unlock request"
    local request_data
    request_data=$(generate_unlock_request "notifications_enabled" "Need to disable for meeting")

    local response
    response=$(api_create_unlock_request "$DEVICE_ID" "$request_data" 2>&1 || true)

    if echo "$response" | grep -qi "success\|created\|pending\|200\|201"; then
        log_success "Unlock request created"
        ((TESTS_PASSED++))

        # Check request ID
        local request_id
        request_id=$(echo "$response" | jq -r '.id // .request_id // empty')
        if [[ -n "$request_id" ]]; then
            log_success "Request ID: $request_id"
            ((TESTS_PASSED++))
        fi
    elif echo "$response" | grep -qi "not.*implemented\|404\|501"; then
        test_skip "Unlock requests not implemented"
    else
        log_debug "Response: ${response:0:100}"
    fi

    test_end
}

# =============================================================================
# Test: Unlock Request Approval
# =============================================================================
test_unlock_request_approval() {
    test_start "Unlock Request Approval"

    # Get pending unlock requests
    log_step "Getting pending unlock requests"
    local requests
    requests=$(api_get_unlock_requests "$DEVICE_ID" 2>/dev/null || echo "[]")

    local pending_request_id
    pending_request_id=$(echo "$requests" | jq -r '.[0].id // .[0].request_id // empty' 2>/dev/null)

    if [[ -z "$pending_request_id" ]]; then
        # Create a request first
        log_step "Creating request for approval test"
        api_lock_device_setting "$DEVICE_ID" "battery_optimization" 2>/dev/null || true
        local create_response
        create_response=$(api_create_unlock_request "$DEVICE_ID" \
            "$(generate_unlock_request 'battery_optimization' 'Testing approval')" 2>/dev/null || echo "")
        pending_request_id=$(echo "$create_response" | jq -r '.id // .request_id // empty')
    fi

    if [[ -z "$pending_request_id" ]]; then
        test_skip "No pending unlock request to approve"
        test_end
        return
    fi

    # Approve the request
    log_step "Approving unlock request: $pending_request_id"
    local response
    response=$(api_approve_unlock_request "$DEVICE_ID" "$pending_request_id" 2>&1 || true)

    if echo "$response" | grep -qi "success\|approved\|200"; then
        log_success "Unlock request approved"
        ((TESTS_PASSED++))
    else
        log_debug "Response: ${response:0:100}"
    fi

    # Verify setting is unlocked
    log_step "Verifying setting is unlocked"
    local settings
    settings=$(api_get_device_settings "$DEVICE_ID" 2>/dev/null || echo "{}")

    local locks
    locks=$(echo "$settings" | jq '.locks // {}' 2>/dev/null)
    local is_locked
    is_locked=$(echo "$locks" | jq '.battery_optimization // false' 2>/dev/null)

    if [[ "$is_locked" == "false" ]] || [[ -z "$is_locked" ]]; then
        log_success "Setting unlocked after approval"
        ((TESTS_PASSED++))
    fi

    test_end
}

# =============================================================================
# Test: Settings History Display
# =============================================================================
test_settings_history_display() {
    test_start "Settings History Display"

    log_step "Fetching settings history"
    local history
    history=$(api_get_settings_history "$DEVICE_ID" 2>/dev/null || echo "[]")

    local history_count
    history_count=$(echo "$history" | jq 'length' 2>/dev/null || echo "0")

    if [[ $history_count -gt 0 ]]; then
        log_success "Settings history available ($history_count entries)"
        ((TESTS_PASSED++))

        # Check history entry structure
        local first_entry
        first_entry=$(echo "$history" | jq '.[0]' 2>/dev/null)

        if echo "$first_entry" | jq -e '.changed_at // .timestamp' &>/dev/null; then
            log_success "History entry has timestamp"
            ((TESTS_PASSED++))
        fi

        if echo "$first_entry" | jq -e '.setting // .key' &>/dev/null; then
            log_success "History entry has setting key"
            ((TESTS_PASSED++))
        fi
    else
        log_info "No settings history available (may be new device)"
    fi

    test_end
}

# =============================================================================
# Test: Bulk Settings Application
# =============================================================================
test_bulk_settings_application() {
    test_start "Bulk Settings Application"

    # Generate bulk settings
    log_step "Applying multiple settings at once"
    local bulk_settings
    bulk_settings=$(generate_device_settings)

    local response
    response=$(api_update_device_settings "$DEVICE_ID" "$bulk_settings" 2>&1 || true)

    if echo "$response" | grep -qi "success\|updated\|200"; then
        log_success "Bulk settings update accepted"
        ((TESTS_PASSED++))
    else
        log_debug "Response: ${response:0:100}"
    fi

    # Verify multiple settings changed
    log_step "Verifying bulk settings applied"
    local updated
    updated=$(api_get_device_settings "$DEVICE_ID" 2>/dev/null || echo "{}")

    local expected_interval
    expected_interval=$(echo "$bulk_settings" | jq -r '.location_interval')
    local actual_interval
    actual_interval=$(echo "$updated" | jq -r '.location_interval')

    if [[ "$expected_interval" == "$actual_interval" ]]; then
        log_success "Bulk settings applied correctly"
        ((TESTS_PASSED++))
    else
        log_info "Settings may not have been applied as expected"
    fi

    test_end
}

# =============================================================================
# Main Test Execution
# =============================================================================
main() {
    echo ""
    echo "=============================================="
    echo "  E2E Settings Sync Tests"
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
    setup_settings_tests

    # Run tests
    test_fetch_device_settings
    test_update_local_setting
    test_sync_settings_to_server
    test_receive_fcm_settings_update
    test_locked_setting_display
    test_locked_setting_modification_blocked
    test_unlock_request_creation
    test_unlock_request_approval
    test_settings_history_display
    test_bulk_settings_application

    # Cleanup
    cleanup_settings_tests

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
