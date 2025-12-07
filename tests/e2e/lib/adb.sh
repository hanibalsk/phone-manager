#!/bin/bash
# ADB helper functions for Android E2E testing
# Provides GPS simulation, screenshots, UI interaction, and app control

# =============================================================================
# Connection Management
# =============================================================================

# Check if ADB is connected to a device/emulator
adb_check_connection() {
    adb devices 2>/dev/null | grep -q "device$"
}

# Wait for device connection
adb_wait_for_device() {
    local timeout="${1:-30}"
    local counter=0

    while ! adb_check_connection && [[ $counter -lt $timeout ]]; do
        sleep 1
        ((counter++))
    done

    adb_check_connection
}

# Check if running on emulator (vs physical device)
adb_is_emulator() {
    local device_name
    device_name=$(adb shell getprop ro.product.model 2>/dev/null)
    [[ "$device_name" == *"sdk"* || "$device_name" == *"Emulator"* || "$device_name" == *"Android SDK"* ]]
}

# =============================================================================
# GPS Location Simulation (Emulator)
# =============================================================================

# Set GPS location on emulator
# Note: adb emu geo fix uses longitude,latitude order!
adb_set_location() {
    local latitude="$1"
    local longitude="$2"
    local altitude="${3:-0}"

    if ! adb_is_emulator; then
        log_warning "GPS simulation only works on emulator"
        return 1
    fi

    # geo fix uses: longitude latitude [altitude]
    adb emu geo fix "$longitude" "$latitude" "$altitude" 2>/dev/null
    log_debug "Set location: lat=$latitude, lon=$longitude"
}

# Simulate movement along a path (array of "lat,lon" points)
adb_simulate_movement() {
    local interval="${1:-2}"  # seconds between updates
    shift
    local points=("$@")

    log_info "Simulating movement with ${#points[@]} waypoints"

    for point in "${points[@]}"; do
        local lat=$(echo "$point" | cut -d',' -f1)
        local lon=$(echo "$point" | cut -d',' -f2)
        adb_set_location "$lat" "$lon"
        sleep "$interval"
    done
}

# Generate linear path between two points
adb_generate_path() {
    local start_lat="$1"
    local start_lon="$2"
    local end_lat="$3"
    local end_lon="$4"
    local num_points="${5:-10}"

    local points=()
    for i in $(seq 0 $((num_points - 1))); do
        local t=$(echo "scale=6; $i / ($num_points - 1)" | bc)
        local lat=$(echo "scale=6; $start_lat + ($end_lat - $start_lat) * $t" | bc)
        local lon=$(echo "scale=6; $start_lon + ($end_lon - $start_lon) * $t" | bc)
        points+=("$lat,$lon")
    done

    echo "${points[@]}"
}

# =============================================================================
# Screenshot Capture
# =============================================================================

# Take screenshot and save to file
adb_screenshot() {
    local filename="${1:-screenshot_$(date +%Y%m%d_%H%M%S).png}"
    local filepath="${SCREENSHOTS_DIR:-/tmp}/${filename}"

    adb exec-out screencap -p > "$filepath" 2>/dev/null

    if [[ -s "$filepath" ]]; then
        log_debug "Screenshot saved: $filepath"
        echo "$filepath"
        return 0
    else
        log_error "Failed to capture screenshot"
        return 1
    fi
}

# Take screenshot with descriptive name
adb_screenshot_named() {
    local test_name="$1"
    local step_name="$2"
    local filename="${test_name}_${step_name}_$(date +%H%M%S).png"
    adb_screenshot "$filename"
}

# =============================================================================
# UI Interaction - Input
# =============================================================================

# Tap at coordinates
adb_tap() {
    local x="$1"
    local y="$2"
    adb shell input tap "$x" "$y"
    log_debug "Tap at ($x, $y)"
}

# Long press at coordinates
adb_long_press() {
    local x="$1"
    local y="$2"
    local duration="${3:-1000}"
    adb shell input swipe "$x" "$y" "$x" "$y" "$duration"
}

# Swipe gesture
adb_swipe() {
    local x1="$1"
    local y1="$2"
    local x2="$3"
    local y2="$4"
    local duration="${5:-300}"
    adb shell input swipe "$x1" "$y1" "$x2" "$y2" "$duration"
    log_debug "Swipe from ($x1,$y1) to ($x2,$y2)"
}

# Scroll gestures (assuming 1080p-ish screen)
adb_scroll_up() {
    adb_swipe 540 1500 540 500 500
}

adb_scroll_down() {
    adb_swipe 540 500 540 1500 500
}

adb_scroll_left() {
    adb_swipe 900 1000 200 1000 300
}

adb_scroll_right() {
    adb_swipe 200 1000 900 1000 300
}

# Input text (escapes spaces)
adb_input_text() {
    local text="$1"
    # Escape spaces and special characters
    local escaped=$(echo "$text" | sed 's/ /%s/g; s/&/\\&/g; s/</\\</g; s/>/\\>/g')
    adb shell input text "$escaped"
    log_debug "Input text: $text"
}

# Clear text field (select all + delete)
adb_clear_text() {
    adb shell input keyevent KEYCODE_CTRL_A
    adb shell input keyevent KEYCODE_DEL
}

# =============================================================================
# UI Interaction - Key Events
# =============================================================================

adb_back() {
    adb shell input keyevent KEYCODE_BACK
    log_debug "Press: Back"
}

adb_home() {
    adb shell input keyevent KEYCODE_HOME
    log_debug "Press: Home"
}

adb_enter() {
    adb shell input keyevent KEYCODE_ENTER
}

adb_tab() {
    adb shell input keyevent KEYCODE_TAB
}

adb_menu() {
    adb shell input keyevent KEYCODE_MENU
}

adb_recent_apps() {
    adb shell input keyevent KEYCODE_APP_SWITCH
}

# =============================================================================
# UI Element Discovery
# =============================================================================

# Get UI hierarchy dump as XML
adb_get_ui_dump() {
    adb exec-out uiautomator dump /dev/tty 2>/dev/null | sed 's/UI hierchary dumped to.*$//'
}

# Check if element with text exists
adb_element_exists() {
    local text="$1"
    local ui_dump=$(adb_get_ui_dump)
    echo "$ui_dump" | grep -q "text=\"$text\""
}

# Check if element with content-description exists
adb_element_exists_by_desc() {
    local desc="$1"
    local ui_dump=$(adb_get_ui_dump)
    echo "$ui_dump" | grep -q "content-desc=\"$desc\""
}

# Check if element with resource-id exists
adb_element_exists_by_id() {
    local id="$1"
    local ui_dump=$(adb_get_ui_dump)
    echo "$ui_dump" | grep -q "resource-id=\"$id\""
}

# Get bounds of element by text
adb_get_element_bounds() {
    local text="$1"
    local ui_dump=$(adb_get_ui_dump)
    # Use sed to extract bounds pattern - macOS compatible
    echo "$ui_dump" | grep "text=\"$text\"" | sed -n 's/.*bounds="\(\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]\)".*/\1/p' | head -1
}

# Calculate center of bounds "[x1,y1][x2,y2]"
_bounds_to_center() {
    local bounds="$1"
    # Parse bounds format [x1,y1][x2,y2] using sed - macOS compatible
    local x1=$(echo "$bounds" | sed 's/\[\([0-9]*\),.*/\1/')
    local y1=$(echo "$bounds" | sed 's/\[[0-9]*,\([0-9]*\)\].*/\1/')
    local x2=$(echo "$bounds" | sed 's/.*\]\[\([0-9]*\),.*/\1/')
    local y2=$(echo "$bounds" | sed 's/.*\]\[[0-9]*,\([0-9]*\)\]/\1/')

    local center_x=$(( (x1 + x2) / 2 ))
    local center_y=$(( (y1 + y2) / 2 ))
    echo "$center_x $center_y"
}

# Find and tap element by text
adb_tap_by_text() {
    local text="$1"
    local bounds=$(adb_get_element_bounds "$text")

    if [[ -n "$bounds" ]]; then
        local center=$(_bounds_to_center "$bounds")
        local x=$(echo "$center" | cut -d' ' -f1)
        local y=$(echo "$center" | cut -d' ' -f2)
        adb_tap "$x" "$y"
        log_debug "Tapped element with text: $text"
        return 0
    else
        log_error "Element not found: $text"
        return 1
    fi
}

# Wait for element to appear
adb_wait_for_element() {
    local text="$1"
    local timeout="${2:-10}"
    local counter=0

    while ! adb_element_exists "$text" && [[ $counter -lt $timeout ]]; do
        sleep 1
        ((counter++))
    done

    if adb_element_exists "$text"; then
        log_debug "Element found: $text (${counter}s)"
        return 0
    else
        log_error "Element not found after ${timeout}s: $text"
        return 1
    fi
}

# Wait for element to disappear
adb_wait_for_element_gone() {
    local text="$1"
    local timeout="${2:-10}"
    local counter=0

    while adb_element_exists "$text" && [[ $counter -lt $timeout ]]; do
        sleep 1
        ((counter++))
    done

    ! adb_element_exists "$text"
}

# Get current activity name
adb_get_current_activity() {
    # Extract activity name from mResumedActivity line - macOS compatible
    adb shell dumpsys activity activities 2>/dev/null | grep 'mResumedActivity' | sed -n 's/.*\([a-zA-Z0-9_.]*\/\.[a-zA-Z0-9_]*\).*/\1/p' | head -1
}

# =============================================================================
# App Management
# =============================================================================

# Install APK
adb_install_app() {
    local apk_path="$1"
    local flags="${2:--r -g}"  # -r=replace, -g=grant permissions

    if [[ ! -f "$apk_path" ]]; then
        log_error "APK not found: $apk_path"
        return 1
    fi

    log_info "Installing APK: $apk_path"
    adb install $flags "$apk_path"
}

# Launch app
adb_launch_app() {
    local package="${1:-$APP_PACKAGE}"
    local activity="${2:-$APP_ACTIVITY}"

    adb shell am start -n "${package}/${activity}" 2>/dev/null
    log_debug "Launched: ${package}/${activity}"
    sleep 2
}

# Launch app with deep link
adb_launch_deep_link() {
    local uri="$1"
    local package="${2:-$APP_PACKAGE}"

    adb shell am start -a android.intent.action.VIEW -d "$uri" "$package"
    log_debug "Deep link: $uri"
    sleep 2
}

# Stop app
adb_stop_app() {
    local package="${1:-$APP_PACKAGE}"
    adb shell am force-stop "$package"
    log_debug "Stopped: $package"
}

# Clear app data
adb_clear_app_data() {
    local package="${1:-$APP_PACKAGE}"
    adb shell pm clear "$package"
    log_info "Cleared app data: $package"
}

# Check if app is installed
adb_app_installed() {
    local package="${1:-$APP_PACKAGE}"
    adb shell pm list packages 2>/dev/null | grep -q "package:$package"
}

# Check if app is running
adb_app_running() {
    local package="${1:-$APP_PACKAGE}"
    adb shell pidof "$package" &>/dev/null
}

# =============================================================================
# Permissions
# =============================================================================

# Grant runtime permission
adb_grant_permission() {
    local package="${1:-$APP_PACKAGE}"
    local permission="$2"
    adb shell pm grant "$package" "$permission" 2>/dev/null
}

# Grant all location permissions
adb_grant_location_permissions() {
    local package="${1:-$APP_PACKAGE}"

    log_info "Granting location permissions to $package"
    adb_grant_permission "$package" "android.permission.ACCESS_FINE_LOCATION"
    adb_grant_permission "$package" "android.permission.ACCESS_COARSE_LOCATION"
    adb_grant_permission "$package" "android.permission.ACCESS_BACKGROUND_LOCATION"
}

# Grant all required permissions for Phone Manager
adb_grant_all_permissions() {
    local package="${1:-$APP_PACKAGE}"

    log_info "Granting all permissions to $package"
    adb_grant_location_permissions "$package"
    adb_grant_permission "$package" "android.permission.POST_NOTIFICATIONS"
    adb_grant_permission "$package" "android.permission.ACTIVITY_RECOGNITION"
    adb_grant_permission "$package" "android.permission.CAMERA"
}

# =============================================================================
# Device Information
# =============================================================================

# Get device screen resolution
adb_get_screen_size() {
    adb shell wm size 2>/dev/null | sed -n 's/.*: *\([0-9]*x[0-9]*\).*/\1/p' | head -1
}

# Get device density
adb_get_density() {
    adb shell wm density 2>/dev/null | sed -n 's/.*: *\([0-9]*\).*/\1/p' | head -1
}

# Get Android version
adb_get_android_version() {
    adb shell getprop ro.build.version.release 2>/dev/null
}

# Get device model
adb_get_device_model() {
    adb shell getprop ro.product.model 2>/dev/null
}

# =============================================================================
# Trip Simulation Helpers
# =============================================================================

# Simulate a walking trip with realistic movement
adb_simulate_walking_trip() {
    local start_lat="$1"
    local start_lon="$2"
    local end_lat="$3"
    local end_lon="$4"
    local duration_seconds="${5:-300}"  # 5 minutes default

    if ! adb_is_emulator; then
        log_warning "Trip simulation only works on emulator"
        return 1
    fi

    log_info "Simulating walking trip: ($start_lat,$start_lon) -> ($end_lat,$end_lon)"

    local num_points=$((duration_seconds / 5))  # Update every 5 seconds
    local path=$(adb_generate_path "$start_lat" "$start_lon" "$end_lat" "$end_lon" "$num_points")

    adb_simulate_movement 5 $path
}

# Simulate a driving trip with realistic speed
adb_simulate_driving_trip() {
    local start_lat="$1"
    local start_lon="$2"
    local end_lat="$3"
    local end_lon="$4"
    local duration_seconds="${5:-180}"  # 3 minutes default

    if ! adb_is_emulator; then
        log_warning "Trip simulation only works on emulator"
        return 1
    fi

    log_info "Simulating driving trip: ($start_lat,$start_lon) -> ($end_lat,$end_lon)"

    local num_points=$((duration_seconds / 2))  # Update every 2 seconds
    local path=$(adb_generate_path "$start_lat" "$start_lon" "$end_lat" "$end_lon" "$num_points")

    adb_simulate_movement 2 $path
}

# Simulate stationary period (for trip end detection)
adb_simulate_stationary() {
    local lat="$1"
    local lon="$2"
    local duration_seconds="${3:-60}"

    log_info "Simulating stationary at ($lat,$lon) for ${duration_seconds}s"

    adb_set_location "$lat" "$lon"
    sleep "$duration_seconds"
}

# Simulate multi-modal trip (walk -> drive -> walk)
adb_simulate_multimodal_trip() {
    local home_lat="$1"
    local home_lon="$2"
    local pickup_lat="$3"
    local pickup_lon="$4"
    local dest_lat="$5"
    local dest_lon="$6"
    local final_lat="$7"
    local final_lon="$8"

    log_info "Simulating multi-modal trip"

    # Walking to pickup
    log_step "Walking to pickup..."
    adb_simulate_walking_trip "$home_lat" "$home_lon" "$pickup_lat" "$pickup_lon" 120

    # Brief stop at pickup
    adb_simulate_stationary "$pickup_lat" "$pickup_lon" 30

    # Driving
    log_step "Driving to destination..."
    adb_simulate_driving_trip "$pickup_lat" "$pickup_lon" "$dest_lat" "$dest_lon" 180

    # Brief stop
    adb_simulate_stationary "$dest_lat" "$dest_lon" 30

    # Walking to final
    log_step "Walking to final destination..."
    adb_simulate_walking_trip "$dest_lat" "$dest_lon" "$final_lat" "$final_lon" 120
}

# =============================================================================
# FCM Mocking (Settings Sync)
# =============================================================================

# Simulate FCM settings update message
mock_fcm_settings_update() {
    local device_id="$1"
    local setting_key="$2"
    local setting_value="$3"

    log_info "Mocking FCM settings update: $setting_key=$setting_value"

    adb shell am broadcast \
        -a com.google.android.c2dm.intent.RECEIVE \
        -n "${APP_PACKAGE}/.services.SettingsMessagingService" \
        --es "type" "settings_update" \
        --es "device_id" "$device_id" \
        --es "setting_key" "$setting_key" \
        --es "setting_value" "$setting_value" \
        2>/dev/null
}

# Simulate FCM settings lock notification
mock_fcm_settings_lock() {
    local device_id="$1"
    local setting_key="$2"
    local locked_by="$3"
    local reason="${4:-Admin locked}"

    log_info "Mocking FCM settings lock: $setting_key by $locked_by"

    adb shell am broadcast \
        -a com.google.android.c2dm.intent.RECEIVE \
        -n "${APP_PACKAGE}/.services.SettingsMessagingService" \
        --es "type" "settings_lock" \
        --es "device_id" "$device_id" \
        --es "setting_key" "$setting_key" \
        --es "locked_by" "$locked_by" \
        --es "reason" "$reason" \
        2>/dev/null
}

# Simulate FCM enrollment complete notification
mock_fcm_enrollment_complete() {
    local enrollment_id="$1"
    local organization_name="$2"

    log_info "Mocking FCM enrollment complete: $enrollment_id"

    adb shell am broadcast \
        -a com.google.android.c2dm.intent.RECEIVE \
        -n "${APP_PACKAGE}/.services.SettingsMessagingService" \
        --es "type" "enrollment_complete" \
        --es "enrollment_id" "$enrollment_id" \
        --es "organization_name" "$organization_name" \
        2>/dev/null
}

# Simulate FCM group invite notification
mock_fcm_group_invite() {
    local group_id="$1"
    local group_name="$2"
    local invited_by="$3"

    log_info "Mocking FCM group invite: $group_name"

    adb shell am broadcast \
        -a com.google.android.c2dm.intent.RECEIVE \
        -n "${APP_PACKAGE}/.services.SettingsMessagingService" \
        --es "type" "group_invite" \
        --es "group_id" "$group_id" \
        --es "group_name" "$group_name" \
        --es "invited_by" "$invited_by" \
        2>/dev/null
}

# =============================================================================
# Service Management
# =============================================================================

# Check if location tracking service is running
adb_is_location_service_running() {
    local package="${1:-$APP_PACKAGE}"
    adb shell dumpsys activity services "$package" 2>/dev/null | grep -q "LocationTrackingService"
}

# Wait for location service to start
adb_wait_for_location_service() {
    local timeout="${1:-30}"
    local counter=0

    while ! adb_is_location_service_running && [[ $counter -lt $timeout ]]; do
        sleep 1
        ((counter++))
    done

    adb_is_location_service_running
}

# Get foreground service notification
adb_get_foreground_notification() {
    local package="${1:-$APP_PACKAGE}"
    adb shell dumpsys notification --noredact 2>/dev/null | grep -A20 "pkg=$package" | head -25
}

# Force stop and restart app (to test service recovery)
adb_restart_app() {
    local package="${1:-$APP_PACKAGE}"
    adb_stop_app "$package"
    sleep 2
    adb_launch_app "$package"
}

# Simulate app kill (swipe from recents)
adb_kill_from_recents() {
    local package="${1:-$APP_PACKAGE}"

    # Open recent apps
    adb_recent_apps
    sleep 1

    # Get screen size for swipe calculation
    local size=$(adb_get_screen_size)
    local width=$(echo "$size" | cut -d'x' -f1)
    local height=$(echo "$size" | cut -d'x' -f2)

    # Swipe to dismiss (swipe up in recents)
    local center_x=$((width / 2))
    local start_y=$((height / 2))
    local end_y=$((height / 4))

    adb_swipe "$center_x" "$start_y" "$center_x" "$end_y" 200
    sleep 1

    # Go home
    adb_home
}

# =============================================================================
# Extended Permissions
# =============================================================================

# Grant Bluetooth permissions
adb_grant_bluetooth_permissions() {
    local package="${1:-$APP_PACKAGE}"
    adb_grant_permission "$package" "android.permission.BLUETOOTH"
    adb_grant_permission "$package" "android.permission.BLUETOOTH_CONNECT"
    adb_grant_permission "$package" "android.permission.BLUETOOTH_SCAN"
}

# Revoke permission (for testing permission denied scenarios)
adb_revoke_permission() {
    local package="${1:-$APP_PACKAGE}"
    local permission="$2"
    adb shell pm revoke "$package" "$permission" 2>/dev/null
}

# Revoke all location permissions
adb_revoke_location_permissions() {
    local package="${1:-$APP_PACKAGE}"
    adb_revoke_permission "$package" "android.permission.ACCESS_FINE_LOCATION"
    adb_revoke_permission "$package" "android.permission.ACCESS_COARSE_LOCATION"
    adb_revoke_permission "$package" "android.permission.ACCESS_BACKGROUND_LOCATION"
}

# Check if permission is granted
adb_has_permission() {
    local package="${1:-$APP_PACKAGE}"
    local permission="$2"
    adb shell dumpsys package "$package" 2>/dev/null | grep -q "$permission: granted=true"
}

# =============================================================================
# Deep Link Testing
# =============================================================================

# Launch group invite deep link
adb_open_group_invite() {
    local invite_code="$1"
    adb_launch_deep_link "phonemanager://join/$invite_code"
}

# Launch enrollment deep link
adb_open_enrollment() {
    local token="$1"
    adb_launch_deep_link "phonemanager://enroll/$token"
}

# Launch weather screen deep link
adb_open_weather_screen() {
    adb_launch_deep_link "phonemanager://weather"
}

# Launch trip history deep link
adb_open_trip_history() {
    adb_launch_deep_link "phonemanager://trips"
}

# Launch settings screen
adb_open_settings() {
    adb_launch_deep_link "phonemanager://settings"
}

# =============================================================================
# Extended UI Helpers
# =============================================================================

# Wait for element to appear (by content description)
adb_wait_for_element_by_desc() {
    local desc="$1"
    local timeout="${2:-10}"
    local counter=0

    while ! adb_element_exists_by_desc "$desc" && [[ $counter -lt $timeout ]]; do
        sleep 1
        ((counter++))
    done

    adb_element_exists_by_desc "$desc"
}

# Wait for element to appear (by resource ID)
adb_wait_for_element_by_id() {
    local id="$1"
    local timeout="${2:-10}"
    local counter=0

    while ! adb_element_exists_by_id "$id" && [[ $counter -lt $timeout ]]; do
        sleep 1
        ((counter++))
    done

    adb_element_exists_by_id "$id"
}

# Tap element by content description
adb_tap_by_desc() {
    local desc="$1"
    local ui_dump=$(adb_get_ui_dump)
    local bounds=$(echo "$ui_dump" | grep "content-desc=\"$desc\"" | sed -n 's/.*bounds="\(\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]\)".*/\1/p' | head -1)

    if [[ -n "$bounds" ]]; then
        local center=$(_bounds_to_center "$bounds")
        local x=$(echo "$center" | cut -d' ' -f1)
        local y=$(echo "$center" | cut -d' ' -f2)
        adb_tap "$x" "$y"
        log_debug "Tapped element with desc: $desc"
        return 0
    else
        log_error "Element not found by desc: $desc"
        return 1
    fi
}

# Get text from element
adb_get_element_text() {
    local search_text="$1"
    local ui_dump=$(adb_get_ui_dump)
    # Get the text attribute from element containing the search text
    echo "$ui_dump" | grep -o 'text="[^"]*"' | sed 's/text="//;s/"$//' | grep "$search_text" | head -1
}

# Check if loading indicator is present
adb_is_loading() {
    local ui_dump=$(adb_get_ui_dump)
    echo "$ui_dump" | grep -qE "Loading|ProgressIndicator|CircularProgress"
}

# Wait for loading to complete
adb_wait_for_loading_complete() {
    local timeout="${1:-30}"
    local counter=0

    while adb_is_loading && [[ $counter -lt $timeout ]]; do
        sleep 1
        ((counter++))
    done

    ! adb_is_loading
}

# =============================================================================
# Secret Mode Testing
# =============================================================================

# Perform secret mode activation gesture (long press on logo)
adb_activate_secret_mode_long_press() {
    local x="${1:-540}"  # Center of screen by default
    local y="${2:-300}"  # Approximate logo position

    log_info "Activating secret mode with long press at ($x,$y)"
    adb_long_press "$x" "$y" 3500  # 3.5 second long press
}

# Perform secret mode activation gesture (5x tap on version)
adb_activate_secret_mode_taps() {
    local x="${1:-540}"
    local y="${2:-1800}"  # Approximate version text position

    log_info "Activating secret mode with 5x tap at ($x,$y)"
    for i in {1..5}; do
        adb_tap "$x" "$y"
        sleep 0.3
    done
}

# =============================================================================
# Battery and Power Management
# =============================================================================

# Disable battery optimization for app
adb_disable_battery_optimization() {
    local package="${1:-$APP_PACKAGE}"
    adb shell dumpsys deviceidle whitelist +$package 2>/dev/null
    log_debug "Disabled battery optimization for $package"
}

# Enable battery optimization for app
adb_enable_battery_optimization() {
    local package="${1:-$APP_PACKAGE}"
    adb shell dumpsys deviceidle whitelist -$package 2>/dev/null
    log_debug "Enabled battery optimization for $package"
}

# Simulate device entering Doze mode
adb_simulate_doze() {
    log_info "Simulating Doze mode"
    adb shell dumpsys deviceidle force-idle 2>/dev/null
}

# Exit Doze mode
adb_exit_doze() {
    log_info "Exiting Doze mode"
    adb shell dumpsys deviceidle unforce 2>/dev/null
}

echo "ADB helpers loaded"

# =============================================================================
# Aliases for backward compatibility
# =============================================================================
adb_check_device() {
    adb_check_connection
}

adb_get_device_id() {
    # Get device ID from the app settings or generate one
    adb shell cat /data/data/three.two.bit.phonemanager/shared_prefs/*.xml 2>/dev/null | grep -oP "(?<=device_id\">)[^<]+" || generate_uuid
}

# =============================================================================
# App Management
# =============================================================================

# Check if an app is installed
adb_is_app_installed() {
    local package="$1"
    adb shell pm list packages 2>/dev/null | grep -q "package:${package}$"
}

