#!/bin/bash
# Phone Manager E2E Test Suite - Master Runner
# Runs all E2E tests and generates reports

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# =============================================================================
# Parse Arguments
# =============================================================================
VERBOSE=false
CATEGORY=""
SKIP_ADB=false
GENERATE_REPORT=true

while [[ $# -gt 0 ]]; do
    case $1 in
        -v|--verbose)
            VERBOSE=true
            export DEBUG=true
            shift
            ;;
        -c|--category)
            CATEGORY="$2"
            shift 2
            ;;
        --skip-adb)
            SKIP_ADB=true
            shift
            ;;
        --no-report)
            GENERATE_REPORT=false
            shift
            ;;
        -h|--help)
            echo "Usage: $0 [options]"
            echo ""
            echo "Options:"
            echo "  -v, --verbose     Enable verbose output"
            echo "  -c, --category    Run specific category (api, adb, screens)"
            echo "  --skip-adb        Skip ADB-dependent tests"
            echo "  --no-report       Skip HTML report generation"
            echo "  -h, --help        Show this help"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# =============================================================================
# Load Configuration
# =============================================================================
source "$SCRIPT_DIR/config.sh"
source "$SCRIPT_DIR/lib/common.sh"
source "$SCRIPT_DIR/lib/api.sh"

# =============================================================================
# Banner
# =============================================================================
echo ""
echo "=============================================="
echo "   Phone Manager E2E Test Suite"
echo "=============================================="
echo "Date:      $(date)"
echo "Session:   $TEST_SESSION_ID"
echo "API URL:   $API_BASE_URL"
echo "App:       $APP_PACKAGE"
echo "=============================================="
echo ""

# =============================================================================
# Prerequisites Check
# =============================================================================
log_info "Checking prerequisites..."

# Check dependencies
if ! check_dependencies; then
    log_error "Missing dependencies. Please install and retry."
    exit 1
fi
log_success "Dependencies OK"

# Check backend
if ! check_backend; then
    log_error "Backend not available. Please start it and retry."
    log_info "Start with: cd phone-manager-backend && source .env.local && CONFIG_DIR=config ./target/debug/phone-manager"
    exit 1
fi
log_success "Backend OK"

# Check ADB connection (optional for API-only tests)
ADB_AVAILABLE=false
if [[ "$SKIP_ADB" != "true" ]]; then
    if check_adb_connection; then
        ADB_AVAILABLE=true
        log_success "ADB connection OK"
    else
        log_warning "No ADB connection - ADB tests will be skipped"
    fi
fi

echo ""

# =============================================================================
# Test Execution
# =============================================================================
TOTAL_TESTS=0
FAILED_TESTS=0
PASSED_TESTS=0
SKIPPED_TESTS=0

run_test() {
    local test_file="$1"
    local test_name=$(basename "$test_file" .sh)

    echo ""
    echo ">>> Running: $test_name"
    echo "-------------------------------------------"

    if bash "$test_file" 2>&1 | tee -a "$TEST_LOG"; then
        ((PASSED_TESTS++))
    else
        ((FAILED_TESTS++))
    fi
    ((TOTAL_TESTS++))
}

# API Tests (always run)
log_info "=== API TESTS ==="
if [[ -z "$CATEGORY" || "$CATEGORY" == "api" ]]; then
    for test_file in "$SCRIPT_DIR/tests/01_health_check.sh" \
                     "$SCRIPT_DIR/tests/02_device_registration.sh" \
                     "$SCRIPT_DIR/tests/03_location_tracking.sh" \
                     "$SCRIPT_DIR/tests/04_trip_lifecycle.sh"; do
        if [[ -f "$test_file" ]]; then
            run_test "$test_file"
        fi
    done
fi

# ADB Tests (require device connection)
if [[ "$ADB_AVAILABLE" == "true" ]] && [[ -z "$CATEGORY" || "$CATEGORY" == "adb" || "$CATEGORY" == "screens" ]]; then
    log_info "=== ADB TESTS ==="

    # Movement simulation
    if [[ -f "$SCRIPT_DIR/tests/05_movement_simulation.sh" ]]; then
        run_test "$SCRIPT_DIR/tests/05_movement_simulation.sh"
    fi

    # Screen navigation
    if [[ -f "$SCRIPT_DIR/tests/06_screen_navigation.sh" ]]; then
        run_test "$SCRIPT_DIR/tests/06_screen_navigation.sh"
    fi

    # Geofence tests
    if [[ -f "$SCRIPT_DIR/tests/07_geofence_tests.sh" ]]; then
        run_test "$SCRIPT_DIR/tests/07_geofence_tests.sh"
    fi
fi

# Scenario Tests
if [[ -z "$CATEGORY" || "$CATEGORY" == "scenarios" ]]; then
    log_info "=== SCENARIO TESTS ==="
    for scenario_file in "$SCRIPT_DIR/scenarios/"*.sh; do
        if [[ -f "$scenario_file" ]]; then
            run_test "$scenario_file"
        fi
    done
fi

# Full Integration Test
if [[ -f "$SCRIPT_DIR/tests/08_full_integration.sh" ]] && [[ -z "$CATEGORY" ]]; then
    log_info "=== INTEGRATION TEST ==="
    run_test "$SCRIPT_DIR/tests/08_full_integration.sh"
fi

# =============================================================================
# Summary
# =============================================================================
echo ""
echo "=============================================="
echo "FINAL SUMMARY"
echo "=============================================="
echo "Total Test Files: $TOTAL_TESTS"
echo "Passed:           $PASSED_TESTS"
echo "Failed:           $FAILED_TESTS"
echo "=============================================="

# Generate HTML report
if [[ "$GENERATE_REPORT" == "true" ]]; then
    source "$SCRIPT_DIR/lib/common.sh"
    TESTS_PASSED=$PASSED_TESTS
    TESTS_FAILED=$FAILED_TESTS
    generate_html_report
fi

echo ""
echo "Log file: $TEST_LOG"
echo "Screenshots: $SCREENSHOTS_DIR"

if [[ "$GENERATE_REPORT" == "true" ]]; then
    echo "HTML Report: ${REPORTS_DIR}/report_${TEST_SESSION_ID}.html"
fi

echo ""

# Exit with failure if any tests failed
if [[ $FAILED_TESTS -gt 0 ]]; then
    echo "RESULT: FAILED"
    exit 1
else
    echo "RESULT: PASSED"
    exit 0
fi
