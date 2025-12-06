#!/bin/bash
# Multi-device coordination helpers for E2E testing
# Provides device-specific commands, parallel operations, and location coordination

# =============================================================================
# Dependencies
# =============================================================================

# Source emulator manager if not already loaded
if [[ -z "${DEVICE_SERIALS+x}" ]]; then
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    source "$SCRIPT_DIR/emulator_manager.sh" 2>/dev/null || true
fi

# =============================================================================
# Device-Specific ADB Commands
# =============================================================================

# Execute ADB command on specific device by role
adb_for_device() {
    local device_role="$1"
    shift
    local serial="${DEVICE_SERIALS[$device_role]}"

    if [[ -z "$serial" ]]; then
        log_error "Unknown device role: $device_role"
        log_error "Available roles: ${!DEVICE_SERIALS[*]}"
        return 1
    fi

    adb -s "$serial" "$@"
}

# Execute shell command on specific device
shell_for_device() {
    local device_role="$1"
    shift
    adb_for_device "$device_role" shell "$@"
}

# =============================================================================
# Location Management
# =============================================================================

# Set GPS location on specific device
# Note: geo fix uses longitude,latitude order!
adb_set_location_for_device() {
    local device_role="$1"
    local latitude="$2"
    local longitude="$3"
    local altitude="${4:-0}"
    local serial="${DEVICE_SERIALS[$device_role]}"

    if [[ -z "$serial" ]]; then
        log_error "Unknown device role: $device_role"
        return 1
    fi

    # Use geo fix command (longitude, latitude order)
    adb -s "$serial" emu geo fix "$longitude" "$latitude" "$altitude" 2>/dev/null || {
        log_error "Failed to set location on $device_role ($serial)"
        return 1
    }

    log_debug "[$device_role] Location set: ($latitude, $longitude)"
    return 0
}

# Set locations on multiple devices in parallel
# Usage: adb_set_locations_parallel "PARENT:37.77,-122.41" "CHILD1:37.80,-122.27"
adb_set_locations_parallel() {
    local entries=("$@")
    local pids=()

    for entry in "${entries[@]}"; do
        local role="${entry%%:*}"
        local coords="${entry#*:}"
        local lat="${coords%%,*}"
        local lon="${coords#*,}"

        adb_set_location_for_device "$role" "$lat" "$lon" &
        pids+=($!)
    done

    # Wait for all to complete
    local failed=0
    for pid in "${pids[@]}"; do
        wait "$pid" || ((failed++))
    done

    return $failed
}

# Simulate gradual movement on device
# Usage: simulate_movement_for_device "CHILD1" "start_lat,start_lon" "end_lat,end_lon" [steps] [interval_sec]
simulate_movement_for_device() {
    local device_role="$1"
    local start_coords="$2"
    local end_coords="$3"
    local steps="${4:-10}"
    local interval="${5:-2}"

    local start_lat="${start_coords%%,*}"
    local start_lon="${start_coords#*,}"
    local end_lat="${end_coords%%,*}"
    local end_lon="${end_coords#*,}"

    log_info "[$device_role] Simulating movement: ($start_lat,$start_lon) -> ($end_lat,$end_lon)"

    for i in $(seq 0 $((steps - 1))); do
        local t=$(echo "scale=6; $i / ($steps - 1)" | bc)
        local lat=$(echo "scale=6; $start_lat + ($end_lat - $start_lat) * $t" | bc)
        local lon=$(echo "scale=6; $start_lon + ($end_lon - $start_lon) * $t" | bc)

        adb_set_location_for_device "$device_role" "$lat" "$lon"
        sleep "$interval"
    done

    log_debug "[$device_role] Movement simulation complete"
}

# Simulate geofence boundary crossing
# Usage: simulate_geofence_crossing "CHILD1" "center_lat" "center_lon" "radius_m" "enter|exit"
simulate_geofence_crossing() {
    local device_role="$1"
    local center_lat="$2"
    local center_lon="$3"
    local radius_meters="$4"
    local direction="${5:-enter}"

    # Convert radius to lat/lon offset (approximate: 1 degree ~ 111km)
    local offset=$(echo "scale=6; $radius_meters / 111000" | bc)

    local outside_lat=$(echo "scale=6; $center_lat + ($offset * 1.5)" | bc)
    local inside_lat=$(echo "scale=6; $center_lat + ($offset * 0.3)" | bc)

    log_info "[$device_role] Simulating geofence $direction..."

    if [[ "$direction" == "enter" ]]; then
        simulate_movement_for_device "$device_role" "$outside_lat,$center_lon" "$inside_lat,$center_lon" 5 2
    else
        simulate_movement_for_device "$device_role" "$inside_lat,$center_lon" "$outside_lat,$center_lon" 5 2
    fi
}

# Simulate proximity approach/departure between two devices
# Usage: simulate_proximity "CHILD1" "CHILD2" "center_lat,center_lon" "radius_m" "approach|depart"
simulate_proximity() {
    local moving_device="$1"
    local stationary_device="$2"
    local center_coords="$3"
    local radius_meters="$4"
    local direction="${5:-approach}"

    local center_lat="${center_coords%%,*}"
    local center_lon="${center_coords#*,}"
    local offset=$(echo "scale=6; $radius_meters * 2 / 111000" | bc)

    # Position stationary device at center
    adb_set_location_for_device "$stationary_device" "$center_lat" "$center_lon"

    local outside_lat=$(echo "scale=6; $center_lat + $offset" | bc)
    local inside_lat=$(echo "scale=6; $center_lat + ($offset * 0.2)" | bc)

    log_info "Simulating proximity $direction: $moving_device -> $stationary_device"

    if [[ "$direction" == "approach" ]]; then
        simulate_movement_for_device "$moving_device" "$outside_lat,$center_lon" "$inside_lat,$center_lon" 5 2
    else
        simulate_movement_for_device "$moving_device" "$inside_lat,$center_lon" "$outside_lat,$center_lon" 5 2
    fi
}

# =============================================================================
# Screenshot Capture
# =============================================================================

# Take screenshot from specific device
adb_screenshot_for_device() {
    local device_role="$1"
    local filename="${2:-${device_role}_screenshot_$(date +%H%M%S).png}"
    local serial="${DEVICE_SERIALS[$device_role]}"

    if [[ -z "$serial" ]]; then
        log_error "Unknown device role: $device_role"
        return 1
    fi

    local filepath="${SCREENSHOTS_DIR:-/tmp}/${filename}"

    adb -s "$serial" exec-out screencap -p > "$filepath" 2>/dev/null

    if [[ -s "$filepath" ]]; then
        log_debug "[$device_role] Screenshot: $filepath"
        echo "$filepath"
        return 0
    else
        log_error "[$device_role] Failed to capture screenshot"
        return 1
    fi
}

# Take screenshots from all devices
take_screenshots_all() {
    local prefix="${1:-all_devices}"
    local timestamp=$(date +%H%M%S)

    for role in "${!DEVICE_SERIALS[@]}"; do
        adb_screenshot_for_device "$role" "${prefix}_${role}_${timestamp}.png"
    done
}

# =============================================================================
# App Management
# =============================================================================

# Launch app on specific device
launch_app_on_device() {
    local device_role="$1"
    local package="${2:-$APP_PACKAGE}"
    local activity="${3:-$APP_ACTIVITY}"

    adb_for_device "$device_role" shell am start -n "${package}/${activity}" 2>/dev/null
    log_debug "[$device_role] Launched app"
    sleep 2
}

# Launch app on all devices
launch_app_on_all_devices() {
    local package="${1:-$APP_PACKAGE}"
    local activity="${2:-$APP_ACTIVITY}"

    log_info "Launching app on all devices..."

    for role in "${!DEVICE_SERIALS[@]}"; do
        launch_app_on_device "$role" "$package" "$activity" &
    done
    wait

    sleep 3
    log_success "App launched on all devices"
}

# Stop app on specific device
stop_app_on_device() {
    local device_role="$1"
    local package="${2:-$APP_PACKAGE}"

    adb_for_device "$device_role" shell am force-stop "$package"
    log_debug "[$device_role] Stopped app"
}

# Stop app on all devices
stop_app_on_all_devices() {
    local package="${1:-$APP_PACKAGE}"

    for role in "${!DEVICE_SERIALS[@]}"; do
        stop_app_on_device "$role" "$package"
    done
}

# Clear app data on specific device
clear_app_data_on_device() {
    local device_role="$1"
    local package="${2:-$APP_PACKAGE}"

    adb_for_device "$device_role" shell pm clear "$package"
    log_debug "[$device_role] Cleared app data"
}

# Clear app data on all devices
clear_app_data_on_all_devices() {
    local package="${1:-$APP_PACKAGE}"

    for role in "${!DEVICE_SERIALS[@]}"; do
        clear_app_data_on_device "$role" "$package"
    done
}

# =============================================================================
# Permissions
# =============================================================================

# Grant all permissions on specific device
grant_permissions_on_device() {
    local device_role="$1"
    local package="${2:-$APP_PACKAGE}"

    log_debug "[$device_role] Granting permissions..."

    adb_for_device "$device_role" shell pm grant "$package" android.permission.ACCESS_FINE_LOCATION 2>/dev/null
    adb_for_device "$device_role" shell pm grant "$package" android.permission.ACCESS_COARSE_LOCATION 2>/dev/null
    adb_for_device "$device_role" shell pm grant "$package" android.permission.ACCESS_BACKGROUND_LOCATION 2>/dev/null
    adb_for_device "$device_role" shell pm grant "$package" android.permission.POST_NOTIFICATIONS 2>/dev/null
    adb_for_device "$device_role" shell pm grant "$package" android.permission.ACTIVITY_RECOGNITION 2>/dev/null
    adb_for_device "$device_role" shell pm grant "$package" android.permission.CAMERA 2>/dev/null
}

# Grant all permissions on all devices
grant_permissions_on_all_devices() {
    local package="${1:-$APP_PACKAGE}"

    log_info "Granting permissions on all devices..."

    for role in "${!DEVICE_SERIALS[@]}"; do
        grant_permissions_on_device "$role" "$package"
    done

    log_success "Permissions granted on all devices"
}

# Revoke location permissions on specific device
revoke_location_permissions_on_device() {
    local device_role="$1"
    local package="${2:-$APP_PACKAGE}"

    adb_for_device "$device_role" shell pm revoke "$package" android.permission.ACCESS_FINE_LOCATION 2>/dev/null
    adb_for_device "$device_role" shell pm revoke "$package" android.permission.ACCESS_COARSE_LOCATION 2>/dev/null
    adb_for_device "$device_role" shell pm revoke "$package" android.permission.ACCESS_BACKGROUND_LOCATION 2>/dev/null

    log_debug "[$device_role] Location permissions revoked"
}

# =============================================================================
# Network Simulation
# =============================================================================

# Set network condition on device (requires root or emulator)
set_network_on_device() {
    local device_role="$1"
    local condition="$2"  # "offline", "slow", "normal"

    log_info "[$device_role] Setting network: $condition"

    case "$condition" in
        "offline")
            adb_for_device "$device_role" shell svc wifi disable 2>/dev/null
            adb_for_device "$device_role" shell svc data disable 2>/dev/null
            ;;
        "slow")
            # Note: This requires emulator console access
            local serial="${DEVICE_SERIALS[$device_role]}"
            local port="${serial#emulator-}"
            echo "network delay 200" | nc localhost "$port" 2>/dev/null
            echo "network speed gsm" | nc localhost "$port" 2>/dev/null
            ;;
        "normal")
            adb_for_device "$device_role" shell svc wifi enable 2>/dev/null
            adb_for_device "$device_role" shell svc data enable 2>/dev/null
            ;;
    esac
}

# Set all devices offline
set_all_devices_offline() {
    for role in "${!DEVICE_SERIALS[@]}"; do
        set_network_on_device "$role" "offline"
    done
}

# Set all devices online
set_all_devices_online() {
    for role in "${!DEVICE_SERIALS[@]}"; do
        set_network_on_device "$role" "normal"
    done
}

# =============================================================================
# Service Checks
# =============================================================================

# Check if location service is running on device
is_location_service_running_on_device() {
    local device_role="$1"
    local package="${2:-$APP_PACKAGE}"

    adb_for_device "$device_role" shell dumpsys activity services "$package" 2>/dev/null | grep -q "LocationTrackingService"
}

# Wait for location service to start on device
wait_for_location_service_on_device() {
    local device_role="$1"
    local timeout="${2:-30}"
    local counter=0

    while ! is_location_service_running_on_device "$device_role"; do
        sleep 1
        ((counter++))
        if [[ $counter -ge $timeout ]]; then
            log_error "[$device_role] Location service not started after ${timeout}s"
            return 1
        fi
    done

    log_debug "[$device_role] Location service running (${counter}s)"
    return 0
}

# Wait for location service on all devices
wait_for_location_service_on_all_devices() {
    local timeout="${1:-30}"

    for role in "${!DEVICE_SERIALS[@]}"; do
        wait_for_location_service_on_device "$role" "$timeout" || return 1
    done

    log_success "Location service running on all devices"
}

# =============================================================================
# UI Interaction
# =============================================================================

# Tap at coordinates on device
tap_on_device() {
    local device_role="$1"
    local x="$2"
    local y="$3"

    adb_for_device "$device_role" shell input tap "$x" "$y"
    log_debug "[$device_role] Tap at ($x, $y)"
}

# Input text on device
input_text_on_device() {
    local device_role="$1"
    local text="$2"

    # Escape spaces and special characters
    local escaped=$(echo "$text" | sed 's/ /%s/g; s/&/\\&/g')
    adb_for_device "$device_role" shell input text "$escaped"
    log_debug "[$device_role] Input text: $text"
}

# Press back on device
back_on_device() {
    local device_role="$1"
    adb_for_device "$device_role" shell input keyevent KEYCODE_BACK
}

# Press home on device
home_on_device() {
    local device_role="$1"
    adb_for_device "$device_role" shell input keyevent KEYCODE_HOME
}

# Check if element with text exists on device
element_exists_on_device() {
    local device_role="$1"
    local text="$2"

    local ui_dump
    ui_dump=$(adb_for_device "$device_role" exec-out uiautomator dump /dev/tty 2>/dev/null | sed 's/UI hierchary dumped to.*$//')
    echo "$ui_dump" | grep -q "text=\"$text\""
}

# Wait for element on device
wait_for_element_on_device() {
    local device_role="$1"
    local text="$2"
    local timeout="${3:-10}"
    local counter=0

    while ! element_exists_on_device "$device_role" "$text"; do
        sleep 1
        ((counter++))
        if [[ $counter -ge $timeout ]]; then
            log_error "[$device_role] Element not found: $text"
            return 1
        fi
    done

    log_debug "[$device_role] Element found: $text (${counter}s)"
    return 0
}

# =============================================================================
# Deep Link Testing
# =============================================================================

# Open deep link on device
open_deep_link_on_device() {
    local device_role="$1"
    local uri="$2"
    local package="${3:-$APP_PACKAGE}"

    adb_for_device "$device_role" shell am start -a android.intent.action.VIEW -d "$uri" "$package"
    log_debug "[$device_role] Deep link: $uri"
    sleep 2
}

# Open group invite on device
open_group_invite_on_device() {
    local device_role="$1"
    local invite_code="$2"

    open_deep_link_on_device "$device_role" "phonemanager://join/$invite_code"
}

# Open enrollment on device
open_enrollment_on_device() {
    local device_role="$1"
    local token="$2"

    open_deep_link_on_device "$device_role" "phonemanager://enroll/$token"
}

# =============================================================================
# Utility Functions
# =============================================================================

# Get device info for role
get_device_info_for_role() {
    local device_role="$1"

    echo "Device Role: $device_role"
    echo "Serial: ${DEVICE_SERIALS[$device_role]}"
    echo "Model: $(adb_for_device "$device_role" shell getprop ro.product.model 2>/dev/null)"
    echo "Android: $(adb_for_device "$device_role" shell getprop ro.build.version.release 2>/dev/null)"
}

# Print all device info
print_all_device_info() {
    log_info "Multi-Device Configuration:"
    echo ""
    for role in "${!DEVICE_SERIALS[@]}"; do
        get_device_info_for_role "$role"
        echo ""
    done
}

# Verify all devices are ready for testing
verify_all_devices_ready() {
    local package="${1:-$APP_PACKAGE}"

    log_info "Verifying all devices..."

    for role in "${!DEVICE_SERIALS[@]}"; do
        # Check ADB connection
        if ! adb_for_device "$role" get-state 2>/dev/null | grep -q "device"; then
            log_error "[$role] ADB connection failed"
            return 1
        fi

        # Check app installed
        if ! adb_for_device "$role" shell pm path "$package" 2>/dev/null | grep -q "package:"; then
            log_error "[$role] App not installed"
            return 1
        fi

        log_debug "[$role] Ready"
    done

    log_success "All devices ready for testing"
    return 0
}

# =============================================================================
# Module Export
# =============================================================================

echo "Multi-device helpers loaded"
