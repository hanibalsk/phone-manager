#!/usr/bin/env bash
# Emulator management library for multi-device E2E testing
# Provides parallel emulator launch, coordination, and cleanup

# =============================================================================
# Configuration
# =============================================================================

# Default emulator ports (even numbers, consecutive pairs)
EMULATOR_PORTS=(5554 5556 5558)

# AVD names for test devices
EMULATOR_AVDS=("phone_manager_e2e_1" "phone_manager_e2e_2" "phone_manager_e2e_3")

# Device role to serial mapping (using indexed arrays for bash 3.x compatibility)
# Index 0=PARENT, 1=CHILD1, 2=CHILD2
DEVICE_SERIALS_PARENT=""
DEVICE_SERIALS_CHILD1=""
DEVICE_SERIALS_CHILD2=""

# Helper functions for device serial access (replaces associative array)
get_device_serial() {
    local role="$1"
    case "$role" in
        PARENT) echo "$DEVICE_SERIALS_PARENT" ;;
        CHILD1) echo "$DEVICE_SERIALS_CHILD1" ;;
        CHILD2) echo "$DEVICE_SERIALS_CHILD2" ;;
        *) echo "" ;;
    esac
}

set_device_serial() {
    local role="$1"
    local serial="$2"
    case "$role" in
        PARENT) DEVICE_SERIALS_PARENT="$serial" ;;
        CHILD1) DEVICE_SERIALS_CHILD1="$serial" ;;
        CHILD2) DEVICE_SERIALS_CHILD2="$serial" ;;
    esac
}

# Android SDK paths
ANDROID_SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
EMULATOR_CMD="$ANDROID_SDK/emulator/emulator"
AVD_MANAGER="$ANDROID_SDK/cmdline-tools/latest/bin/avdmanager"
SDK_MANAGER="$ANDROID_SDK/cmdline-tools/latest/bin/sdkmanager"

# Default AVD configuration
DEFAULT_SYSTEM_IMAGE="system-images;android-34;google_apis;arm64-v8a"
DEFAULT_DEVICE="pixel_6"
EMULATOR_RAM_MB=2048
EMULATOR_HEAP_MB=512

# =============================================================================
# AVD Management
# =============================================================================

# Check if AVD exists
avd_exists() {
    local avd_name="$1"
    "$EMULATOR_CMD" -list-avds 2>/dev/null | grep -q "^${avd_name}$"
}

# Create AVD if it doesn't exist
create_avd_if_needed() {
    local avd_name="$1"
    local device="${2:-$DEFAULT_DEVICE}"
    local system_image="${3:-$DEFAULT_SYSTEM_IMAGE}"

    if avd_exists "$avd_name"; then
        log_debug "AVD $avd_name already exists"
        return 0
    fi

    log_info "Creating AVD: $avd_name"

    # Check if system image is installed
    if ! "$SDK_MANAGER" --list_installed 2>/dev/null | grep -q "$system_image"; then
        log_info "Installing system image: $system_image"
        yes | "$SDK_MANAGER" "$system_image" 2>/dev/null || {
            log_error "Failed to install system image: $system_image"
            return 1
        }
    fi

    # Create AVD
    echo "no" | "$AVD_MANAGER" create avd \
        --force \
        --name "$avd_name" \
        --device "$device" \
        --package "$system_image" \
        2>/dev/null || {
        log_error "Failed to create AVD: $avd_name"
        return 1
    }

    # Configure AVD for testing
    local config_ini="$HOME/.android/avd/${avd_name}.avd/config.ini"
    if [[ -f "$config_ini" ]]; then
        echo "hw.ramSize=${EMULATOR_RAM_MB}" >> "$config_ini"
        echo "vm.heapSize=${EMULATOR_HEAP_MB}" >> "$config_ini"
        echo "hw.gpu.enabled=yes" >> "$config_ini"
        echo "hw.gpu.mode=swiftshader_indirect" >> "$config_ini"
    fi

    log_success "AVD $avd_name created successfully"
    return 0
}

# Delete AVD
delete_avd() {
    local avd_name="$1"

    if avd_exists "$avd_name"; then
        log_info "Deleting AVD: $avd_name"
        "$AVD_MANAGER" delete avd --name "$avd_name" 2>/dev/null
    fi
}

# =============================================================================
# Emulator Launch & Shutdown
# =============================================================================

# Launch a single emulator
launch_emulator() {
    local avd_name="$1"
    local port="$2"
    local headless="${3:-true}"

    # Kill any existing instance on this port
    local existing_pid
    existing_pid=$(lsof -ti:$port 2>/dev/null)
    if [[ -n "$existing_pid" ]]; then
        log_debug "Killing existing process on port $port (PID: $existing_pid)"
        kill "$existing_pid" 2>/dev/null || true
        sleep 2
    fi

    # Build emulator command
    local emu_args=(
        -avd "$avd_name"
        -port "$port"
        -no-audio
        -no-boot-anim
        -gpu swiftshader_indirect
        -memory "$EMULATOR_RAM_MB"
        -partition-size 4096
        -no-snapshot-save
    )

    if [[ "$headless" == "true" ]]; then
        emu_args+=(-no-window)
    fi

    log_info "Starting emulator $avd_name on port $port..."

    # Start emulator in background
    "$EMULATOR_CMD" "${emu_args[@]}" &>/dev/null &
    local emu_pid=$!

    # Save PID for cleanup
    echo "$emu_pid" > "/tmp/emulator_${port}.pid"

    log_debug "Emulator started with PID: $emu_pid"
    return 0
}

# Wait for a single emulator to be ready
wait_for_emulator() {
    local serial="$1"
    local timeout="${2:-180}"
    local counter=0

    log_debug "Waiting for $serial to be ready..."

    # Wait for device to appear
    while ! adb -s "$serial" get-state 2>/dev/null | grep -q "device"; do
        sleep 2
        ((counter+=2))
        if [[ $counter -ge $timeout ]]; then
            log_error "Timeout waiting for $serial to appear"
            return 1
        fi
    done

    # Wait for boot completion
    while [[ -z $(adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r') ]]; do
        sleep 2
        ((counter+=2))
        if [[ $counter -ge $timeout ]]; then
            log_error "Timeout waiting for $serial to boot"
            return 1
        fi
    done

    # Wait for package manager
    while ! adb -s "$serial" shell pm path android 2>/dev/null | grep -q "package:"; do
        sleep 1
        ((counter+=1))
        if [[ $counter -ge $timeout ]]; then
            log_error "Timeout waiting for package manager on $serial"
            return 1
        fi
    done

    log_success "$serial ready (${counter}s)"
    return 0
}

# Launch multiple emulators in parallel
launch_emulators() {
    local count="${1:-2}"
    local headless="${2:-true}"

    if [[ $count -gt 3 ]]; then
        log_error "Maximum 3 emulators supported"
        return 1
    fi

    log_info "Launching $count emulator(s)..."

    # Create AVDs and start emulators
    for i in $(seq 0 $((count - 1))); do
        local avd="${EMULATOR_AVDS[$i]}"
        local port="${EMULATOR_PORTS[$i]}"

        create_avd_if_needed "$avd" || return 1
        launch_emulator "$avd" "$port" "$headless" || return 1

        # Assign device role
        case $i in
            0) set_device_serial "PARENT" "emulator-$port" ;;
            1) set_device_serial "CHILD1" "emulator-$port" ;;
            2) set_device_serial "CHILD2" "emulator-$port" ;;
        esac
    done

    # Wait for all emulators to be ready
    log_info "Waiting for emulators to boot..."
    for i in $(seq 0 $((count - 1))); do
        local port="${EMULATOR_PORTS[$i]}"
        local serial="emulator-$port"
        wait_for_emulator "$serial" || return 1
    done

    # Export device serials for child scripts
    export DEVICE_SERIALS_PARENT DEVICE_SERIALS_CHILD1 DEVICE_SERIALS_CHILD2

    log_success "All $count emulators ready"
    return 0
}

# Stop a single emulator
stop_emulator() {
    local port="$1"
    local serial="emulator-$port"

    log_debug "Stopping emulator on port $port"

    # Try graceful shutdown first
    adb -s "$serial" emu kill 2>/dev/null || true

    # Kill by PID if still running
    if [[ -f "/tmp/emulator_${port}.pid" ]]; then
        local pid
        pid=$(cat "/tmp/emulator_${port}.pid")
        if kill -0 "$pid" 2>/dev/null; then
            kill "$pid" 2>/dev/null || true
            sleep 2
            kill -9 "$pid" 2>/dev/null || true
        fi
        rm -f "/tmp/emulator_${port}.pid"
    fi
}

# Stop all running emulators
shutdown_all_emulators() {
    log_info "Shutting down all emulators..."

    for port in "${EMULATOR_PORTS[@]}"; do
        stop_emulator "$port"
    done

    # Clear device serials
    DEVICE_SERIALS=()

    sleep 3
    log_success "All emulators stopped"
}

# =============================================================================
# Health Checks
# =============================================================================

# Check if all expected emulators are running
check_emulators_health() {
    local expected_count="${1:-2}"
    local running=0

    for i in $(seq 0 $((expected_count - 1))); do
        local port="${EMULATOR_PORTS[$i]}"
        local serial="emulator-$port"

        if adb -s "$serial" get-state 2>/dev/null | grep -q "device"; then
            ((running++))
        else
            log_warning "Emulator $serial not responding"
        fi
    done

    if [[ $running -eq $expected_count ]]; then
        return 0
    else
        log_error "$running/$expected_count emulators running"
        return 1
    fi
}

# Get list of running emulator serials
get_running_emulators() {
    adb devices 2>/dev/null | grep "emulator-" | grep "device$" | cut -f1
}

# Get emulator count
get_emulator_count() {
    get_running_emulators | wc -l | tr -d ' '
}

# =============================================================================
# App Installation
# =============================================================================

# Install APK on all running emulators
install_apk_on_all() {
    local apk_path="${1:-$APP_APK_PATH}"

    if [[ ! -f "$apk_path" ]]; then
        log_error "APK not found: $apk_path"
        return 1
    fi

    log_info "Installing APK on all emulators..."

    for serial in $(get_running_emulators); do
        log_debug "Installing on $serial..."
        adb -s "$serial" install -r -g "$apk_path" 2>/dev/null || {
            log_error "Failed to install on $serial"
            return 1
        }
    done

    log_success "APK installed on all emulators"
}

# Grant all permissions on all emulators
grant_permissions_on_all() {
    local package="${1:-$APP_PACKAGE}"

    log_info "Granting permissions on all emulators..."

    for serial in $(get_running_emulators); do
        log_debug "Granting permissions on $serial..."

        adb -s "$serial" shell pm grant "$package" android.permission.ACCESS_FINE_LOCATION 2>/dev/null
        adb -s "$serial" shell pm grant "$package" android.permission.ACCESS_COARSE_LOCATION 2>/dev/null
        adb -s "$serial" shell pm grant "$package" android.permission.ACCESS_BACKGROUND_LOCATION 2>/dev/null
        adb -s "$serial" shell pm grant "$package" android.permission.POST_NOTIFICATIONS 2>/dev/null
        adb -s "$serial" shell pm grant "$package" android.permission.ACTIVITY_RECOGNITION 2>/dev/null
        adb -s "$serial" shell pm grant "$package" android.permission.CAMERA 2>/dev/null
    done

    log_success "Permissions granted on all emulators"
}

# =============================================================================
# Utility Functions
# =============================================================================

# Get serial for device role
get_serial_for_role() {
    local role="$1"
    get_device_serial "$role"
}

# List device roles and serials
list_device_roles() {
    log_info "Device role mappings:"
    echo "  PARENT -> $(get_device_serial PARENT)"
    echo "  CHILD1 -> $(get_device_serial CHILD1)"
    echo "  CHILD2 -> $(get_device_serial CHILD2)"
}

# Print emulator status
print_emulator_status() {
    log_info "Emulator Status:"
    for port in "${EMULATOR_PORTS[@]}"; do
        local serial="emulator-$port"
        local status
        if adb -s "$serial" get-state 2>/dev/null | grep -q "device"; then
            status="RUNNING"
        else
            status="STOPPED"
        fi
        echo "  $serial: $status"
    done
}

# =============================================================================
# CLI Interface
# =============================================================================

emulator_manager_usage() {
    echo "Usage: emulator_manager.sh <command> [options]"
    echo ""
    echo "Commands:"
    echo "  launch [count] [--headed]   Launch emulators (default: 2, headless)"
    echo "  stop [port]                 Stop emulator on specific port"
    echo "  stop-all                    Stop all emulators"
    echo "  status                      Show emulator status"
    echo "  install <apk>               Install APK on all emulators"
    echo "  permissions [package]       Grant permissions on all emulators"
    echo "  create-avd <name>           Create AVD for testing"
    echo "  delete-avd <name>           Delete AVD"
    echo "  health [count]              Check emulator health"
    echo ""
    echo "Examples:"
    echo "  emulator_manager.sh launch 3"
    echo "  emulator_manager.sh launch 2 --headed"
    echo "  emulator_manager.sh install app/build/outputs/apk/debug/app-debug.apk"
    echo "  emulator_manager.sh stop-all"
}

# Main CLI handler (only when run directly)
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    # Source dependencies
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    source "$SCRIPT_DIR/common.sh" 2>/dev/null || {
        # Minimal logging if common.sh not available
        log_info() { echo "[INFO] $1"; }
        log_success() { echo "[OK] $1"; }
        log_warning() { echo "[WARN] $1"; }
        log_error() { echo "[ERROR] $1"; }
        log_debug() { [[ "${DEBUG:-}" == "true" ]] && echo "[DEBUG] $1"; }
    }
    source "$SCRIPT_DIR/../config.sh" 2>/dev/null || true

    case "${1:-}" in
        launch)
            count="${2:-2}"
            headless="true"
            [[ "${3:-}" == "--headed" ]] && headless="false"
            launch_emulators "$count" "$headless"
            ;;
        stop)
            stop_emulator "${2:-5554}"
            ;;
        stop-all)
            shutdown_all_emulators
            ;;
        status)
            print_emulator_status
            ;;
        install)
            install_apk_on_all "${2:-}"
            ;;
        permissions)
            grant_permissions_on_all "${2:-}"
            ;;
        create-avd)
            create_avd_if_needed "${2:-phone_manager_e2e_test}"
            ;;
        delete-avd)
            delete_avd "${2:-}"
            ;;
        health)
            check_emulators_health "${2:-2}"
            ;;
        *)
            emulator_manager_usage
            exit 1
            ;;
    esac
else
    # Sourced as library - define fallback logging if not already defined
    if ! type log_info &>/dev/null; then
        log_info() { echo "[INFO] $1"; }
        log_success() { echo "[OK] $1"; }
        log_warning() { echo "[WARN] $1"; }
        log_error() { echo "[ERROR] $1"; }
        log_debug() { [[ "${DEBUG:-}" == "true" ]] && echo "[DEBUG] $1" || true; }
    fi
    echo "Emulator manager library loaded"
fi
