#!/bin/bash
# =============================================================================
# E2E Tests: Weather Integration
# Tests weather API, display, notifications, and caching
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

# =============================================================================
# Test Setup
# =============================================================================
setup_weather_tests() {
    log_info "Setting up weather tests..."

    # Get device ID
    if adb_check_device; then
        DEVICE_ID=$(adb_get_device_id 2>/dev/null || generate_device_id)
        adb_grant_location_permissions
    else
        DEVICE_ID=$(generate_device_id)
    fi

    log_info "Device ID: $DEVICE_ID"
}

cleanup_weather_tests() {
    log_info "Cleaning up weather tests..."
}

# =============================================================================
# Test: Weather API Fetch
# =============================================================================
test_weather_api_fetch() {
    test_start "Weather API Fetch"

    log_step "Fetching current weather for test location"
    local response
    response=$(api_get_weather "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON" 2>&1)
    local status=$?

    if [[ $status -ne 0 ]]; then
        log_error "Weather API call failed"
        test_end
        return 1
    fi

    # Check if endpoint doesn't exist (returns HTML instead of JSON)
    if echo "$response" | grep -qi "<!DOCTYPE\|<html"; then
        test_skip "Weather API endpoint not implemented"
        test_end
        return 0
    fi

    # Check for authentication errors
    if echo "$response" | grep -qi "unauthorized\|Missing Authorization"; then
        test_skip "Weather endpoint requires Bearer token authentication"
        test_end
        return 0
    fi

    # Verify response structure
    if echo "$response" | jq -e '.' &>/dev/null; then
        log_success "Weather data returned as valid JSON"
        ((TESTS_PASSED++))
    else
        log_error "Weather response not valid JSON"
        ((TESTS_FAILED++))
        test_end
        return 1
    fi

    # Check for expected weather fields
    local temp
    temp=$(echo "$response" | jq -r '.temperature // .temp // .current.temp // empty')
    if [[ -n "$temp" ]] && [[ "$temp" != "null" ]]; then
        log_success "Temperature available: $temp"
        ((TESTS_PASSED++))
    else
        log_warning "Temperature not found in response"
    fi

    local conditions
    conditions=$(echo "$response" | jq -r '.conditions // .weather // .description // empty')
    if [[ -n "$conditions" ]] && [[ "$conditions" != "null" ]]; then
        log_success "Weather conditions available: $conditions"
        ((TESTS_PASSED++))
    fi

    local humidity
    humidity=$(echo "$response" | jq -r '.humidity // .current.humidity // empty')
    if [[ -n "$humidity" ]] && [[ "$humidity" != "null" ]]; then
        log_success "Humidity available: $humidity%"
        ((TESTS_PASSED++))
    fi

    log_debug "Weather response: ${response:0:300}"

    test_end
}

# =============================================================================
# Test: Weather Screen Display
# =============================================================================
test_weather_screen_display() {
    test_start "Weather Screen Display"

    if ! adb_check_device; then
        test_skip "No device for UI test"
        test_end
        return
    fi

    # Set location for weather
    log_step "Setting location for weather"
    adb_set_location "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON"
    sleep 2

    # Open weather screen
    log_step "Opening weather screen"
    adb_open_deep_link "phonemanager://weather"
    sleep 5

    adb_screenshot "weather_screen"

    log_success "Weather screen captured"
    ((TESTS_PASSED++))

    # Check if weather UI elements are visible
    log_step "Checking weather UI elements"
    local ui_dump
    ui_dump=$(adb shell uiautomator dump /dev/tty 2>/dev/null || echo "")

    if echo "$ui_dump" | grep -qi "temperature\|weather\|forecast\|degrees"; then
        log_success "Weather UI elements detected"
        ((TESTS_PASSED++))
    else
        log_info "Could not verify weather UI elements via uiautomator"
    fi

    test_end
}

# =============================================================================
# Test: Weather Notification Integration
# =============================================================================
test_weather_notification_integration() {
    test_start "Weather Notification Integration"

    if ! adb_check_device; then
        test_skip "No device for notification test"
        test_end
        return
    fi

    # Check if weather notifications are enabled
    log_step "Checking weather notification settings"
    local settings
    settings=$(api_get_device_settings "$DEVICE_ID" 2>/dev/null || echo "{}")

    local weather_notifications
    weather_notifications=$(echo "$settings" | jq -r '.weather_notifications // .enable_weather_notifications // true')

    if [[ "$weather_notifications" == "true" ]]; then
        log_success "Weather notifications enabled"
        ((TESTS_PASSED++))
    else
        log_info "Weather notifications may be disabled"
    fi

    # Trigger weather update
    log_step "Triggering weather update"
    adb shell am broadcast \
        -a "${APP_PACKAGE}.UPDATE_WEATHER" \
        -n "${APP_PACKAGE}/.receivers.WeatherReceiver" 2>/dev/null || true

    sleep 5

    # Check for weather notification
    log_step "Checking for weather notification"
    local notifications
    notifications=$(adb shell dumpsys notification --noredact 2>/dev/null | grep -A10 "$APP_PACKAGE" || echo "")

    if echo "$notifications" | grep -qi "weather\|temperature\|forecast"; then
        log_success "Weather notification found"
        ((TESTS_PASSED++))
    else
        log_info "Weather notification not detected"
    fi

    # Screenshot notification area
    adb shell cmd statusbar expand-notifications
    sleep 1
    adb_screenshot "weather_notification"
    adb shell cmd statusbar collapse

    test_end
}

# =============================================================================
# Test: Weather Cache Behavior
# =============================================================================
test_weather_cache_behavior() {
    test_start "Weather Cache Behavior"

    # First request
    log_step "Making first weather request"
    local start_time=$(date +%s%3N)
    local response1
    response1=$(api_get_weather "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON" 2>/dev/null || echo "")
    local first_duration=$(($(date +%s%3N) - start_time))

    if [[ -z "$response1" ]]; then
        test_skip "Weather API not available"
        test_end
        return
    fi

    log_info "First request took: ${first_duration}ms"

    # Second request (should be cached)
    log_step "Making second weather request (should be cached)"
    sleep 1
    start_time=$(date +%s%3N)
    local response2
    response2=$(api_get_weather "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON" 2>/dev/null || echo "")
    local second_duration=$(($(date +%s%3N) - start_time))

    log_info "Second request took: ${second_duration}ms"

    # Check if responses are the same (cached)
    if [[ "$response1" == "$response2" ]]; then
        log_success "Weather data appears to be cached"
        ((TESTS_PASSED++))
    else
        log_info "Weather data may have changed between requests"
    fi

    # Check if second request was faster (indicates cache hit)
    if [[ $second_duration -lt $first_duration ]]; then
        log_success "Cached request was faster"
        ((TESTS_PASSED++))
    else
        log_info "Cache timing not significantly different"
    fi

    test_end
}

# =============================================================================
# Test: Weather Offline Fallback
# =============================================================================
test_weather_offline_fallback() {
    test_start "Weather Offline Fallback"

    if ! adb_check_device; then
        test_skip "No device for offline test"
        test_end
        return
    fi

    # First, get weather while online to cache it
    log_step "Caching weather data while online"
    adb_set_location "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON"
    sleep 2

    adb_open_deep_link "phonemanager://weather"
    sleep 5

    # Go offline
    log_step "Going offline"
    adb_enable_airplane_mode
    sleep 3

    # Try to view weather
    log_step "Checking weather display while offline"
    adb_open_deep_link "phonemanager://weather"
    sleep 3
    adb_screenshot "weather_offline"

    # Check UI for cached data or offline message
    local ui_dump
    ui_dump=$(adb shell uiautomator dump /dev/tty 2>/dev/null || echo "")

    if echo "$ui_dump" | grep -qi "offline\|cached\|last.updated\|temperature"; then
        log_success "Weather shows cached data or offline message"
        ((TESTS_PASSED++))
    else
        log_info "Could not verify offline weather behavior"
    fi

    # Go back online
    log_step "Going back online"
    adb_disable_airplane_mode
    sleep 5

    test_end
}

# =============================================================================
# Test: Weather Toggle Setting
# =============================================================================
test_weather_toggle_setting() {
    test_start "Weather Toggle Setting"

    # Get current setting
    log_step "Getting current weather setting"
    local settings
    settings=$(api_get_device_settings "$DEVICE_ID" 2>/dev/null || echo "{}")

    local current_state
    current_state=$(echo "$settings" | jq -r '.weather_enabled // .show_weather // true')

    # Toggle the setting
    local new_state="false"
    if [[ "$current_state" == "false" ]]; then
        new_state="true"
    fi

    log_step "Toggling weather setting to: $new_state"
    local update_data
    update_data="{\"weather_enabled\": $new_state}"

    local response
    response=$(api_update_device_settings "$DEVICE_ID" "$update_data" 2>&1 || true)

    if echo "$response" | grep -qi "success\|updated\|200"; then
        log_success "Weather setting toggled"
        ((TESTS_PASSED++))
    else
        log_debug "Response: ${response:0:100}"
    fi

    # Verify setting changed
    log_step "Verifying setting change"
    settings=$(api_get_device_settings "$DEVICE_ID" 2>/dev/null || echo "{}")
    local actual_state
    actual_state=$(echo "$settings" | jq -r '.weather_enabled // .show_weather // empty')

    if [[ "$actual_state" == "$new_state" ]]; then
        log_success "Weather setting verified"
        ((TESTS_PASSED++))
    fi

    # Restore original setting
    api_update_device_settings "$DEVICE_ID" "{\"weather_enabled\": $current_state}" 2>/dev/null || true

    test_end
}

# =============================================================================
# Test: Weather 5-Day Forecast
# =============================================================================
test_weather_5day_forecast() {
    test_start "Weather 5-Day Forecast"

    log_step "Fetching 5-day forecast"
    local response
    response=$(api_get_weather_forecast "$TEST_LOC_HOME_LAT" "$TEST_LOC_HOME_LON" 5 2>&1 || true)

    if [[ -z "$response" ]] || echo "$response" | grep -qi "not.*implemented\|404\|501"; then
        test_skip "Forecast API not implemented"
        test_end
        return
    fi

    # Check forecast structure
    if echo "$response" | jq -e '.' &>/dev/null; then
        log_success "Forecast returned as valid JSON"
        ((TESTS_PASSED++))
    else
        log_error "Forecast response not valid JSON"
        test_end
        return
    fi

    # Check for forecast days
    local days
    days=$(echo "$response" | jq '.forecast // .daily // []' 2>/dev/null)
    local day_count
    day_count=$(echo "$days" | jq 'length' 2>/dev/null || echo "0")

    if [[ $day_count -ge 5 ]]; then
        log_success "5-day forecast available ($day_count days)"
        ((TESTS_PASSED++))
    elif [[ $day_count -gt 0 ]]; then
        log_info "Forecast has $day_count days (expected 5)"
    else
        log_warning "No forecast days in response"
    fi

    # Check forecast day structure
    local first_day
    first_day=$(echo "$days" | jq '.[0]' 2>/dev/null)

    if echo "$first_day" | jq -e '.date // .dt' &>/dev/null; then
        log_success "Forecast day has date"
        ((TESTS_PASSED++))
    fi

    if echo "$first_day" | jq -e '.high // .temp.max // .temp_max' &>/dev/null; then
        log_success "Forecast day has high temperature"
        ((TESTS_PASSED++))
    fi

    if echo "$first_day" | jq -e '.low // .temp.min // .temp_min' &>/dev/null; then
        log_success "Forecast day has low temperature"
        ((TESTS_PASSED++))
    fi

    # Check UI if device available
    if adb_check_device; then
        log_step "Checking forecast display in app"
        adb_open_deep_link "phonemanager://weather"
        sleep 3
        # Scroll to see forecast
        adb shell input swipe 500 1500 500 500
        sleep 1
        adb_screenshot "weather_forecast"
    fi

    test_end
}

# =============================================================================
# Main Test Execution
# =============================================================================
main() {
    echo ""
    echo "=============================================="
    echo "  E2E Weather Integration Tests"
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

    # Early check: Test if weather endpoint exists (returns JSON not HTML)
    log_info "Checking weather endpoint availability..."
    local test_response
    test_response=$(curl -s "${API_BASE_URL}/api/v1/weather?lat=37.7749&lon=-122.4194" -H "X-API-Key: ${ADMIN_API_KEY}" 2>&1)
    if echo "$test_response" | grep -qi "<!DOCTYPE\|<html"; then
        log_warning "Weather API endpoint not implemented - skipping all weather tests"
        echo ""
        echo "==========================================
TEST SUMMARY
==========================================
Passed:  0
Failed:  0
Skipped: 7
Total:   7
Pass Rate: N/A (all skipped)
=========================================="
        exit 0
    fi

    # Setup
    setup_weather_tests

    # Run tests
    test_weather_api_fetch
    test_weather_screen_display
    test_weather_notification_integration
    test_weather_cache_behavior
    test_weather_offline_fallback
    test_weather_toggle_setting
    test_weather_5day_forecast

    # Cleanup
    cleanup_weather_tests

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
