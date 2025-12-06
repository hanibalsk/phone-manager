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
# Generate test session ID early (used by other variables)
export TEST_SESSION_ID="${TEST_SESSION_ID:-$(date +%Y%m%d_%H%M%S)_$$}"

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
# Test Users Configuration (Hybrid approach)
# =============================================================================
# Pre-configured test users for stable tests
export TEST_USER_PARENT_EMAIL="test_parent@e2e.phonemanager.local"
export TEST_USER_PARENT_PASSWORD="Parent123!"
export TEST_USER_PARENT_NAME="E2E Parent User"

export TEST_USER_CHILD1_EMAIL="test_child1@e2e.phonemanager.local"
export TEST_USER_CHILD1_PASSWORD="Child123!"
export TEST_USER_CHILD1_NAME="E2E Child One"

export TEST_USER_CHILD2_EMAIL="test_child2@e2e.phonemanager.local"
export TEST_USER_CHILD2_PASSWORD="Child123!"
export TEST_USER_CHILD2_NAME="E2E Child Two"

export TEST_USER_ADMIN_EMAIL="test_admin@e2e.phonemanager.local"
export TEST_USER_ADMIN_PASSWORD="Admin123!"
export TEST_USER_ADMIN_NAME="E2E Admin User"

# Dynamic test user prefix (for isolation)
export TEST_USER_PREFIX="e2e_${TEST_SESSION_ID}_"

# =============================================================================
# Test Locations (Named locations for scenarios)
# =============================================================================
export TEST_LOC_HOME_LAT="37.7749"
export TEST_LOC_HOME_LON="-122.4194"
export TEST_LOC_HOME_NAME="Home (SF Downtown)"

export TEST_LOC_WORK_LAT="37.8044"
export TEST_LOC_WORK_LON="-122.2712"
export TEST_LOC_WORK_NAME="Work (Oakland)"

export TEST_LOC_SCHOOL_LAT="37.8715"
export TEST_LOC_SCHOOL_LON="-122.2730"
export TEST_LOC_SCHOOL_NAME="School (Berkeley)"

export TEST_LOC_PARK_LAT="37.7600"
export TEST_LOC_PARK_LON="-122.4100"
export TEST_LOC_PARK_NAME="Park (SF Mission)"

export TEST_LOC_MALL_LAT="37.7855"
export TEST_LOC_MALL_LON="-122.4061"
export TEST_LOC_MALL_NAME="Mall (SF Union Square)"

# =============================================================================
# Test Routes (Pre-defined routes for trip simulation)
# =============================================================================
# Route format: "name:start_lat,start_lon,end_lat,end_lon,duration_min,mode"
export TEST_ROUTE_COMMUTE="commute:${TEST_LOC_HOME_LAT},${TEST_LOC_HOME_LON},${TEST_LOC_WORK_LAT},${TEST_LOC_WORK_LON},30,driving"
export TEST_ROUTE_SCHOOL_RUN="school_run:${TEST_LOC_HOME_LAT},${TEST_LOC_HOME_LON},${TEST_LOC_SCHOOL_LAT},${TEST_LOC_SCHOOL_LON},15,driving"
export TEST_ROUTE_MORNING_JOG="morning_jog:${TEST_LOC_HOME_LAT},${TEST_LOC_HOME_LON},${TEST_LOC_PARK_LAT},${TEST_LOC_PARK_LON},20,walking"
export TEST_ROUTE_SHOPPING="shopping:${TEST_LOC_HOME_LAT},${TEST_LOC_HOME_LON},${TEST_LOC_MALL_LAT},${TEST_LOC_MALL_LON},10,driving"

# =============================================================================
# Test Geofence Presets
# =============================================================================
export TEST_GEOFENCE_HOME_RADIUS="100"
export TEST_GEOFENCE_WORK_RADIUS="200"
export TEST_GEOFENCE_SCHOOL_RADIUS="150"

# =============================================================================
# Trip Detection Settings
# =============================================================================
export TRIP_STATIONARY_THRESHOLD_SECONDS="300"   # 5 minutes
export TRIP_GRACE_PERIOD_SECONDS="120"           # 2 minutes
export TRIP_MIN_DISTANCE_METERS="50"             # Minimum distance for trip

# =============================================================================
# Test Timeouts
# =============================================================================
export TEST_TIMEOUT_SHORT="10"     # Quick operations
export TEST_TIMEOUT_MEDIUM="30"    # Standard operations
export TEST_TIMEOUT_LONG="60"      # Long operations (trip detection, etc.)
export TEST_TIMEOUT_EXTENDED="120" # Extended operations (full scenarios)

# =============================================================================
# Test Group Names
# =============================================================================
export TEST_GROUP_FAMILY="E2E Test Family ${TEST_SESSION_ID}"
export TEST_GROUP_WORK="E2E Test Work ${TEST_SESSION_ID}"

# =============================================================================
# Cleanup Configuration
# =============================================================================
export CLEANUP_AFTER_TEST="${CLEANUP_AFTER_TEST:-true}"
export PRESERVE_FAILED_DATA="${PRESERVE_FAILED_DATA:-true}"

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
