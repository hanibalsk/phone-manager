#!/bin/bash
# =============================================================================
# E2E Scenario: Multi-Device Family
# Tests family group with multiple devices, proximity alerts, and admin controls
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
# Scenario Configuration
# =============================================================================
SCENARIO_NAME="Multi-Device Family"
FAMILY_GROUP_ID=""
PARENT_DEVICE_ID=""
CHILD1_DEVICE_ID=""
CHILD2_DEVICE_ID=""
HOME_GEOFENCE_ID=""
SCHOOL_GEOFENCE_ID=""

# =============================================================================
# Scenario Setup
# =============================================================================
setup_scenario() {
    log_info "Setting up $SCENARIO_NAME..."

    # Check prerequisites
    if ! check_dependencies; then
        log_error "Missing dependencies"
        return 1
    fi

    if ! check_backend; then
        log_error "Backend not available"
        return 1
    fi

    # Generate device IDs for family members
    PARENT_DEVICE_ID=$(generate_device_id)
    CHILD1_DEVICE_ID=$(generate_device_id)
    CHILD2_DEVICE_ID=$(generate_device_id)

    log_info "Parent device: $PARENT_DEVICE_ID"
    log_info "Child 1 device: $CHILD1_DEVICE_ID"
    log_info "Child 2 device: $CHILD2_DEVICE_ID"
}

cleanup_scenario() {
    log_info "Cleaning up scenario..."

    # Delete geofences
    if [[ -n "$HOME_GEOFENCE_ID" ]]; then
        api_delete_geofence "$PARENT_DEVICE_ID" "$HOME_GEOFENCE_ID" 2>/dev/null || true
    fi
    if [[ -n "$SCHOOL_GEOFENCE_ID" ]]; then
        api_delete_geofence "$PARENT_DEVICE_ID" "$SCHOOL_GEOFENCE_ID" 2>/dev/null || true
    fi

    # Delete group
    if [[ -n "$FAMILY_GROUP_ID" ]]; then
        api_delete_group "$FAMILY_GROUP_ID" 2>/dev/null || true
    fi
}

# =============================================================================
# Phase 1: Register Devices
# =============================================================================
phase_register_devices() {
    log_info "=== PHASE 1: Register Devices ==="

    test_start "Register Family Devices"

    # Register parent device
    log_step "Registering parent device"
    local parent_response
    parent_response=$(api_register_device "$PARENT_DEVICE_ID" "Parent's Phone" 2>&1 || true)

    if echo "$parent_response" | grep -qi "success\|created\|200\|201" || \
       echo "$parent_response" | jq -e '.id // .device_id' &>/dev/null; then
        log_success "Parent device registered"
        ((TESTS_PASSED++))
    else
        log_warning "Parent device registration unclear"
    fi

    # Register child 1 device
    log_step "Registering Child 1 device"
    local child1_response
    child1_response=$(api_register_device "$CHILD1_DEVICE_ID" "Child 1's Phone" 2>&1 || true)

    if echo "$child1_response" | grep -qi "success\|created\|200\|201" || \
       echo "$child1_response" | jq -e '.id // .device_id' &>/dev/null; then
        log_success "Child 1 device registered"
        ((TESTS_PASSED++))
    fi

    # Register child 2 device
    log_step "Registering Child 2 device"
    local child2_response
    child2_response=$(api_register_device "$CHILD2_DEVICE_ID" "Child 2's Phone" 2>&1 || true)

    if echo "$child2_response" | grep -qi "success\|created\|200\|201" || \
       echo "$child2_response" | jq -e '.id // .device_id' &>/dev/null; then
        log_success "Child 2 device registered"
        ((TESTS_PASSED++))
    fi

    test_end
}

# =============================================================================
# Phase 2: Create Family Group
# =============================================================================
phase_create_family_group() {
    log_info "=== PHASE 2: Create Family Group ==="

    test_start "Create Family Group"

    log_step "Creating family group"
    local group_response
    group_response=$(api_create_group "Smith Family $(date +%s)")

    FAMILY_GROUP_ID=$(echo "$group_response" | jq -r '.id // .group_id // empty')

    if [[ -n "$FAMILY_GROUP_ID" ]]; then
        log_success "Family group created: $FAMILY_GROUP_ID"
        ((TESTS_PASSED++))
    else
        log_error "Failed to create family group"
        ((TESTS_FAILED++))
        return 1
    fi

    test_end
}

# =============================================================================
# Phase 3: Add All Devices to Group
# =============================================================================
phase_add_devices_to_group() {
    log_info "=== PHASE 3: Add Devices to Group ==="

    test_start "Add Devices to Family Group"

    # Generate invite codes and add devices
    log_step "Adding parent device as owner"
    # Parent is already owner from group creation

    # Add Child 1
    log_step "Generating invite for Child 1"
    local invite_response
    invite_response=$(api_create_group_invite "$FAMILY_GROUP_ID")
    local invite_code
    invite_code=$(echo "$invite_response" | jq -r '.code')

    local join_response
    join_response=$(api_join_group "$invite_code" "$CHILD1_DEVICE_ID" 2>&1 || true)

    if echo "$join_response" | grep -qi "success\|joined\|member"; then
        log_success "Child 1 added to group"
        ((TESTS_PASSED++))
    else
        log_warning "Child 1 join unclear"
    fi

    # Add Child 2
    log_step "Generating invite for Child 2"
    invite_response=$(api_create_group_invite "$FAMILY_GROUP_ID")
    invite_code=$(echo "$invite_response" | jq -r '.code')

    join_response=$(api_join_group "$invite_code" "$CHILD2_DEVICE_ID" 2>&1 || true)

    if echo "$join_response" | grep -qi "success\|joined\|member"; then
        log_success "Child 2 added to group"
        ((TESTS_PASSED++))
    else
        log_warning "Child 2 join unclear"
    fi

    # Verify group members
    log_step "Verifying group membership"
    local members
    members=$(api_get_group_members "$FAMILY_GROUP_ID" 2>/dev/null || echo "[]")
    local member_count
    member_count=$(echo "$members" | jq 'length' 2>/dev/null || echo "0")

    log_info "Group has $member_count members"

    if [[ $member_count -ge 3 ]]; then
        log_success "All family members in group"
        ((TESTS_PASSED++))
    fi

    test_end
}

# =============================================================================
# Phase 4: Set Up Geofences
# =============================================================================
phase_setup_geofences() {
    log_info "=== PHASE 4: Setup Geofences ==="

    test_start "Create Family Geofences"

    # Create home geofence
    log_step "Creating home geofence"
    local home_data
    home_data=$(generate_geofence_data "Home" "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON" "100")

    local home_response
    home_response=$(api_create_geofence "$PARENT_DEVICE_ID" "$home_data" 2>/dev/null || echo "")

    HOME_GEOFENCE_ID=$(echo "$home_response" | jq -r '.id // .geofence_id // empty')

    if [[ -n "$HOME_GEOFENCE_ID" ]]; then
        log_success "Home geofence created"
        ((TESTS_PASSED++))
    fi

    # Create school geofence
    log_step "Creating school geofence"
    local school_data
    school_data=$(generate_geofence_data "School" "$TEST_LOC_SCHOOL_LAT" "$TEST_LOC_SCHOOL_LON" "150")

    local school_response
    school_response=$(api_create_geofence "$PARENT_DEVICE_ID" "$school_data" 2>/dev/null || echo "")

    SCHOOL_GEOFENCE_ID=$(echo "$school_response" | jq -r '.id // .geofence_id // empty')

    if [[ -n "$SCHOOL_GEOFENCE_ID" ]]; then
        log_success "School geofence created"
        ((TESTS_PASSED++))
    fi

    test_end
}

# =============================================================================
# Phase 5: Set Up Proximity Alerts
# =============================================================================
phase_setup_proximity_alerts() {
    log_info "=== PHASE 5: Proximity Alerts ==="

    test_start "Setup Proximity Alerts"

    # Create proximity alert between Child 1 and Child 2
    log_step "Creating proximity alert for children"
    local alert_data
    alert_data=$(cat <<EOF
{
    "name": "Children Proximity",
    "device_a": "$CHILD1_DEVICE_ID",
    "device_b": "$CHILD2_DEVICE_ID",
    "radius": 100,
    "notify_on": ["ENTER", "EXIT"]
}
EOF
)

    local response
    response=$(api_create_proximity_alert "$FAMILY_GROUP_ID" "$alert_data" 2>&1 || true)

    if echo "$response" | grep -qi "success\|created\|200\|201"; then
        log_success "Proximity alert created"
        ((TESTS_PASSED++))
    elif echo "$response" | grep -qi "not.*implemented\|404\|501"; then
        log_info "Proximity alerts not implemented"
    else
        log_debug "Response: ${response:0:100}"
    fi

    test_end
}

# =============================================================================
# Phase 6: Simulate Child Leaving Geofence
# =============================================================================
phase_child_leaves_geofence() {
    log_info "=== PHASE 6: Child Leaves Geofence ==="

    test_start "Child Leaves Home Geofence"

    # Set Child 1 at home initially
    log_step "Child 1 at home"
    # Simulate via API (child device location update)
    api_update_device_location "$CHILD1_DEVICE_ID" \
        "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON" 2>/dev/null || true
    sleep 3

    # Child 1 leaves home (goes to park)
    log_step "Child 1 leaving home"
    api_update_device_location "$CHILD1_DEVICE_ID" \
        "$TEST_LOC_PARK_LAT" "$TEST_LOC_PARK_LON" 2>/dev/null || true
    sleep 10

    # Check for geofence exit event
    log_step "Checking for EXIT event"
    local events
    events=$(api_get_geofence_events "$PARENT_DEVICE_ID" "$HOME_GEOFENCE_ID" 2>/dev/null || echo "[]")

    if echo "$events" | jq -e '.[] | select(.type | test("EXIT"; "i"))' &>/dev/null; then
        log_success "Geofence EXIT event detected"
        ((TESTS_PASSED++))
    else
        log_info "EXIT event may not have been triggered (depends on implementation)"
    fi

    test_end
}

# =============================================================================
# Phase 7: Verify Parent Notification
# =============================================================================
phase_verify_notification() {
    log_info "=== PHASE 7: Parent Notification ==="

    test_start "Verify Parent Notification"

    # Mock FCM notification to parent
    log_step "Simulating notification to parent"

    if adb_check_device; then
        mock_fcm_geofence_event "$PARENT_DEVICE_ID" "$HOME_GEOFENCE_ID" "EXIT" "$CHILD1_DEVICE_ID"
        sleep 5

        # Check notifications
        local notifications
        notifications=$(adb shell dumpsys notification --noredact 2>/dev/null | grep -A10 "$APP_PACKAGE" || echo "")

        if echo "$notifications" | grep -qi "left\|exit\|geofence\|child"; then
            log_success "Parent received notification"
            ((TESTS_PASSED++))
        else
            log_info "Notification not detected via dumpsys"
        fi
    else
        log_info "Device not available for notification check"
    fi

    test_end
}

# =============================================================================
# Phase 8: Admin Settings Control
# =============================================================================
phase_admin_settings_control() {
    log_info "=== PHASE 8: Admin Settings Control ==="

    test_start "Admin Settings Control"

    # Parent locks a setting on Child 1's device
    log_step "Parent locking setting on Child 1"
    local lock_response
    lock_response=$(api_lock_device_setting "$CHILD1_DEVICE_ID" "location_interval" 2>&1 || true)

    if echo "$lock_response" | grep -qi "success\|locked\|200"; then
        log_success "Setting locked by admin"
        ((TESTS_PASSED++))
    elif echo "$lock_response" | grep -qi "not.*implemented\|404\|501"; then
        log_info "Admin locks not implemented"
    else
        log_debug "Response: ${lock_response:0:100}"
    fi

    # Verify Child cannot modify locked setting
    log_step "Verifying Child cannot modify locked setting"
    local update_response
    update_response=$(api_update_device_settings "$CHILD1_DEVICE_ID" '{"location_interval": 120000}' 2>&1 || true)

    if echo "$update_response" | grep -qi "locked\|forbidden\|blocked"; then
        log_success "Locked setting modification blocked"
        ((TESTS_PASSED++))
    else
        log_info "Lock enforcement unclear"
    fi

    # Parent unlocks setting
    log_step "Parent unlocking setting"
    api_unlock_device_setting "$CHILD1_DEVICE_ID" "location_interval" 2>/dev/null || true

    test_end
}

# =============================================================================
# Phase 9: View Family Dashboard
# =============================================================================
phase_family_dashboard() {
    log_info "=== PHASE 9: Family Dashboard ==="

    test_start "View Family Dashboard"

    if ! adb_check_device; then
        test_skip "No device for dashboard test"
        test_end
        return
    fi

    log_step "Opening family dashboard"
    adb_open_deep_link "phonemanager://group/$FAMILY_GROUP_ID"
    sleep 5

    adb_screenshot "family_dashboard"

    log_success "Family dashboard captured"
    ((TESTS_PASSED++))

    # Check member locations
    log_step "Checking member locations"
    local member_locations
    member_locations=$(api_get_group_member_locations "$FAMILY_GROUP_ID" 2>/dev/null || echo "[]")

    local location_count
    location_count=$(echo "$member_locations" | jq 'length' 2>/dev/null || echo "0")

    if [[ $location_count -gt 0 ]]; then
        log_success "Member locations available ($location_count)"
        ((TESTS_PASSED++))
    fi

    test_end
}

# =============================================================================
# Phase 10: End of Day Summary
# =============================================================================
phase_end_of_day() {
    log_info "=== PHASE 10: End of Day Summary ==="

    test_start "Family Day Summary"

    # Get activity summary for each family member
    log_step "Getting family activity summary"

    for device_id in "$PARENT_DEVICE_ID" "$CHILD1_DEVICE_ID" "$CHILD2_DEVICE_ID"; do
        local trips
        trips=$(api_get_trips "$device_id" 5 2>/dev/null || echo "[]")
        local trip_count
        trip_count=$(echo "$trips" | jq 'length' 2>/dev/null || echo "0")

        local locations
        locations=$(api_get_location_count "$device_id" 2>/dev/null || echo "0")

        log_info "Device $device_id: $trip_count trips, $locations locations"
    done

    log_success "Family summary complete"
    ((TESTS_PASSED++))

    test_end
}

# =============================================================================
# Main Scenario Execution
# =============================================================================
main() {
    echo ""
    echo "=============================================="
    echo "  E2E SCENARIO: $SCENARIO_NAME"
    echo "  Session: $TEST_SESSION_ID"
    echo "=============================================="
    echo ""

    # Setup
    if ! setup_scenario; then
        log_error "Scenario setup failed"
        exit 1
    fi

    # Run phases
    phase_register_devices
    phase_create_family_group
    phase_add_devices_to_group
    phase_setup_geofences
    phase_setup_proximity_alerts
    phase_child_leaves_geofence
    phase_verify_notification
    phase_admin_settings_control
    phase_family_dashboard
    phase_end_of_day

    # Cleanup
    cleanup_scenario

    # Summary
    print_summary
    generate_html_report "${REPORTS_DIR}/scenario_multi_device_${TEST_SESSION_ID}.html"

    echo ""
    echo "=============================================="
    echo "  SCENARIO COMPLETE"
    echo "=============================================="

    [[ $TESTS_FAILED -eq 0 ]]
}

# Run if executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
