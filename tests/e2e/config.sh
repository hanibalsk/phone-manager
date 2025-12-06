#!/bin/bash
# E2E Test Configuration for Phone Manager
# Source this file before running tests

# =============================================================================
# API Configuration
# =============================================================================
export API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"

# Admin API key for device registration and admin operations
# This is the same key as used by the Android app, marked as admin in the database
export ADMIN_API_KEY="${ADMIN_API_KEY:-pm_85TGIKiMtOF061ZTrk9kgIkRag2ucW8AjNPiIHB0CVc_v}"

# Device API key - defaults to same as admin key (can be overridden by tests)
export API_KEY="${API_KEY:-pm_85TGIKiMtOF061ZTrk9kgIkRag2ucW8AjNPiIHB0CVc_v}"

# =============================================================================
# Android App Configuration
# =============================================================================
export APP_PACKAGE="three.two.bit.phonemanager"
export APP_ACTIVITY=".MainActivity"
export APP_APK_PATH="${APP_APK_PATH:-$(dirname "$0")/../../app/build/outputs/apk/debug/app-debug.apk}"

# =============================================================================
# Test Configuration
# =============================================================================
export TEST_GROUP_ID="${TEST_GROUP_ID:-e2e-test-$(date +%s)}"
export TEST_DEVICE_NAME="${TEST_DEVICE_NAME:-E2E Test Device}"
export TEST_TIMEOUT="${TEST_TIMEOUT:-30}"
export SCREENSHOT_ON_FAILURE="${SCREENSHOT_ON_FAILURE:-true}"

# =============================================================================
# Paths
# =============================================================================
export E2E_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export REPORTS_DIR="${E2E_ROOT}/reports"
export SCREENSHOTS_DIR="${E2E_ROOT}/screenshots"
export ROUTES_DIR="${E2E_ROOT}/routes"

# =============================================================================
# Test Data - San Francisco Bay Area Coordinates
# =============================================================================
# Downtown SF
export SF_DOWNTOWN_LAT="37.7749"
export SF_DOWNTOWN_LON="-122.4194"

# Oakland
export OAKLAND_LAT="37.8044"
export OAKLAND_LON="-122.2712"

# Berkeley
export BERKELEY_LAT="37.8715"
export BERKELEY_LON="-122.2730"

# Palo Alto
export PALO_ALTO_LAT="37.4419"
export PALO_ALTO_LON="-122.1430"

# =============================================================================
# Speed Constants (meters per second)
# =============================================================================
export WALKING_SPEED="1.4"      # ~5 km/h
export RUNNING_SPEED="3.5"      # ~12.6 km/h
export CYCLING_SPEED="5.5"      # ~20 km/h
export DRIVING_SPEED="13.9"     # ~50 km/h (city)
export HIGHWAY_SPEED="27.8"     # ~100 km/h

# =============================================================================
# Update Intervals (milliseconds between location updates)
# =============================================================================
export WALKING_INTERVAL="5000"
export DRIVING_INTERVAL="2000"
export DEFAULT_INTERVAL="3000"

# =============================================================================
# API Limits
# =============================================================================
export MAX_BATCH_LOCATIONS="50"
export MAX_BATCH_EVENTS="100"
export GEOFENCE_MIN_RADIUS="20"
export GEOFENCE_MAX_RADIUS="50000"
export PROXIMITY_MIN_RADIUS="50"
export PROXIMITY_MAX_RADIUS="100000"

# =============================================================================
# Initialization
# =============================================================================
# Create directories if they don't exist
mkdir -p "$REPORTS_DIR" "$SCREENSHOTS_DIR"

# Generate test session ID
export TEST_SESSION_ID="$(date +%Y%m%d_%H%M%S)_$$"

# Log file for this session
export TEST_LOG="${REPORTS_DIR}/test_${TEST_SESSION_ID}.log"

# =============================================================================
# Helper: Check if all required tools are available
# =============================================================================
check_dependencies() {
    local missing=()

    command -v curl &>/dev/null || missing+=("curl")
    command -v jq &>/dev/null || missing+=("jq")
    command -v adb &>/dev/null || missing+=("adb")

    if [[ ${#missing[@]} -gt 0 ]]; then
        echo "ERROR: Missing required dependencies: ${missing[*]}"
        echo "Install with: brew install ${missing[*]} (macOS) or apt-get install ${missing[*]} (Ubuntu)"
        return 1
    fi
    return 0
}

# =============================================================================
# Helper: Check ADB connection
# =============================================================================
check_adb_connection() {
    if ! adb devices 2>/dev/null | grep -q "device$"; then
        echo "ERROR: No Android device/emulator connected"
        echo "Start an emulator or connect a device via USB"
        return 1
    fi
    return 0
}

# =============================================================================
# Helper: Check backend availability
# =============================================================================
check_backend() {
    local response
    response=$(curl -s -o /dev/null -w "%{http_code}" "${API_BASE_URL}/api/health" 2>/dev/null)
    if [[ "$response" != "200" ]]; then
        echo "ERROR: Backend not available at ${API_BASE_URL}"
        echo "Start the backend with: cd phone-manager-backend && source .env.local && CONFIG_DIR=config ./target/debug/phone-manager"
        return 1
    fi
    return 0
}

echo "E2E Config loaded - Session: $TEST_SESSION_ID"
