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

echo "ADB helpers loaded"
