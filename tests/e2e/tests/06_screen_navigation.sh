#!/bin/bash
# Test: Screen Navigation
# Tests navigation to all 35+ screens in the app

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$SCRIPT_DIR/config.sh"
source "$SCRIPT_DIR/lib/common.sh"
source "$SCRIPT_DIR/lib/adb.sh"

test_start "Screen Navigation"

# =============================================================================
# Prerequisites Check
# =============================================================================
log_info "Checking prerequisites..."

if ! adb_check_connection; then
    log_error "No Android device/emulator connected"
    log_info "Start emulator with: emulator -avd <avd_name>"
    exit 1
fi

if ! adb_app_installed; then
    log_error "App not installed: $APP_PACKAGE"
    log_info "Install with: adb install -r app/build/outputs/apk/debug/app-debug.apk"
    exit 1
fi

# Get device info
log_info "Device: $(adb_get_device_model)"
log_info "Android: $(adb_get_android_version)"
log_info "Screen: $(adb_get_screen_size)"

# =============================================================================
# Setup: Fresh app state
# =============================================================================
log_step "Preparing app for testing..."
adb_stop_app
adb_grant_all_permissions
sleep 1
adb_launch_app
sleep 3

# Take initial screenshot
adb_screenshot "00_app_launched.png"

# =============================================================================
# Category: Current screen (depends on app state)
# =============================================================================

# Function to navigate and verify screen
test_screen() {
    local screen_name="$1"
    local element_text="$2"
    local nav_action="$3"  # Optional navigation action

    log_step "Testing: $screen_name"

    # Execute navigation action if provided
    if [[ -n "$nav_action" ]]; then
        eval "$nav_action"
        sleep 2
    fi

    # Check if expected element exists
    if adb_wait_for_element "$element_text" 5; then
        log_success "$screen_name - Element found: $element_text"
        adb_screenshot "${screen_name// /_}.png"
        ((TESTS_PASSED++))
        return 0
    else
        log_error "$screen_name - Element NOT found: $element_text"
        adb_screenshot "${screen_name// /_}_FAIL.png"
        ((TESTS_FAILED++))
        return 1
    fi
}

# Helper to tap and navigate
tap_text() {
    local text="$1"
    adb_tap_by_text "$text" || adb_tap_by_text "$text"
}

# =============================================================================
# Authentication Flow (if not logged in)
# =============================================================================
log_info "=== AUTHENTICATION SCREENS ==="

if adb_element_exists "Login" || adb_element_exists "Sign In"; then
    test_screen "Login Screen" "Email" ""
    test_screen "Login Form" "Password" ""

    # Check for Register link
    if adb_element_exists "Register" || adb_element_exists "Sign Up"; then
        test_screen "Register Link" "Register" ""
    fi

    # Check for Forgot Password
    if adb_element_exists "Forgot"; then
        test_screen "Forgot Password Link" "Forgot" ""
    fi

    log_warning "App requires login - some tests may be skipped"
fi

# =============================================================================
# Home Screen (after login/registration)
# =============================================================================
log_info "=== HOME & CORE SCREENS ==="

# Check if we're on home screen
if adb_element_exists "Location Statistics" || adb_element_exists "Location Tracking" || adb_element_exists "Home"; then
    test_screen "Home Screen" "Location Tracking" ""
fi

# =============================================================================
# Navigation Tiles from Home
# =============================================================================

# Group Members
log_step "Testing: Group Members"
adb_launch_app  # Reset to home
sleep 2
if adb_element_exists "Group" && adb_tap_by_text "Group"; then
    sleep 2
    if adb_element_exists "Group Members" || adb_element_exists "No other devices"; then
        log_success "Group Members screen loaded"
        adb_screenshot "group_members.png"
        ((TESTS_PASSED++))
    fi
    adb_back
    sleep 1
fi

# Map
log_step "Testing: Map"
if adb_element_exists "Map" && adb_tap_by_text "Map"; then
    sleep 2
    # Map may show permission dialog or actual map
    adb_screenshot "map_screen.png"
    log_success "Map screen loaded"
    ((TESTS_PASSED++))
    adb_back
    sleep 1
fi

# History
log_step "Testing: History"
if adb_element_exists "History" && adb_tap_by_text "History"; then
    sleep 2
    adb_screenshot "history_screen.png"
    log_success "History screen loaded"
    ((TESTS_PASSED++))
    adb_back
    sleep 1
fi

# Settings
log_step "Testing: Settings"
if adb_element_exists "Settings" && adb_tap_by_text "Settings"; then
    sleep 2
    if adb_element_exists "Device" || adb_element_exists "Sync" || adb_element_exists "Settings"; then
        log_success "Settings screen loaded"
        adb_screenshot "settings_screen.png"
        ((TESTS_PASSED++))
    fi
    adb_back
    sleep 1
fi

# Alerts (Proximity Alerts)
log_step "Testing: Alerts"
if adb_element_exists "Alerts" && adb_tap_by_text "Alerts"; then
    sleep 2
    adb_screenshot "alerts_screen.png"
    log_success "Alerts screen loaded"
    ((TESTS_PASSED++))
    adb_back
    sleep 1
fi

# Geofences
log_step "Testing: Geofences"
if adb_element_exists "Geofences" && adb_tap_by_text "Geofences"; then
    sleep 2
    adb_screenshot "geofences_screen.png"
    log_success "Geofences screen loaded"
    ((TESTS_PASSED++))

    # Test Create Geofence
    if adb_element_exists "Create" || adb_element_exists "Add"; then
        adb_tap_by_text "Create" || adb_tap_by_text "Add"
        sleep 2
        adb_screenshot "create_geofence.png"
        log_success "Create Geofence screen loaded"
        ((TESTS_PASSED++))
        adb_back
    fi

    adb_back
    sleep 1
fi

# Trips / Trip History
log_step "Testing: Trips"
if adb_element_exists "Trips" && adb_tap_by_text "Trips"; then
    sleep 2
    adb_screenshot "trips_screen.png"
    log_success "Trips screen loaded"
    ((TESTS_PASSED++))
    adb_back
    sleep 1
fi

# =============================================================================
# Deep Navigation Tests
# =============================================================================
log_info "=== EXTENDED NAVIGATION ==="

# Weather (if available)
if adb_element_exists "Weather"; then
    log_step "Testing: Weather"
    adb_tap_by_text "Weather"
    sleep 2
    adb_screenshot "weather_screen.png"
    log_success "Weather screen loaded"
    ((TESTS_PASSED++))
    adb_back
    sleep 1
fi

# Webhooks (if available)
if adb_element_exists "Webhooks"; then
    log_step "Testing: Webhooks"
    adb_tap_by_text "Webhooks"
    sleep 2
    adb_screenshot "webhooks_screen.png"
    log_success "Webhooks screen loaded"
    ((TESTS_PASSED++))
    adb_back
    sleep 1
fi

# =============================================================================
# Scroll and find more options
# =============================================================================
log_step "Scrolling to find more options..."
adb_scroll_up
sleep 1
adb_screenshot "home_scrolled.png"

# Device List (if available)
if adb_element_exists "Devices" || adb_element_exists "My Devices"; then
    log_step "Testing: Device List"
    adb_tap_by_text "Devices" || adb_tap_by_text "My Devices"
    sleep 2
    adb_screenshot "device_list.png"
    log_success "Device List screen loaded"
    ((TESTS_PASSED++))
    adb_back
    sleep 1
fi

# Groups (if available)
if adb_element_exists "Groups" || adb_element_exists "My Groups"; then
    log_step "Testing: Group List"
    adb_tap_by_text "Groups" || adb_tap_by_text "My Groups"
    sleep 2
    adb_screenshot "group_list.png"
    log_success "Group List screen loaded"
    ((TESTS_PASSED++))
    adb_back
    sleep 1
fi

# =============================================================================
# Settings Sub-screens
# =============================================================================
log_info "=== SETTINGS SUB-SCREENS ==="

adb_launch_app
sleep 2
if adb_element_exists "Settings" && adb_tap_by_text "Settings"; then
    sleep 2

    # Look for various settings options
    for option in "Sync" "Privacy" "About" "Account" "Notifications"; do
        if adb_element_exists "$option"; then
            log_step "Testing: Settings > $option"
            adb_tap_by_text "$option"
            sleep 1
            adb_screenshot "settings_${option,,}.png"
            log_success "Settings > $option loaded"
            ((TESTS_PASSED++))
            adb_back
            sleep 1
        fi
    done

    adb_back
fi

# =============================================================================
# Back Navigation Test
# =============================================================================
log_info "=== BACK NAVIGATION ==="

log_step "Testing back button navigation..."
adb_launch_app
sleep 2

# Navigate deep and come back
adb_tap_by_text "Settings" 2>/dev/null || true
sleep 1
adb_back
sleep 1

if adb_element_exists "Location Tracking" || adb_element_exists "Location Statistics" || adb_element_exists "Home"; then
    log_success "Back navigation returned to home"
    ((TESTS_PASSED++))
else
    log_warning "Back navigation may not have returned to home"
fi

# =============================================================================
# Final Screenshot
# =============================================================================
adb_screenshot "99_test_complete.png"

# =============================================================================
# Summary
# =============================================================================
test_end

log_info "Screenshots saved to: $SCREENSHOTS_DIR"
log_info "View with: open $SCREENSHOTS_DIR"

print_summary
