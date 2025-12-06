#!/bin/bash
# Common utility functions for E2E tests
# Provides logging, assertions, and test lifecycle management

# =============================================================================
# Colors for output
# =============================================================================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# =============================================================================
# Test Counters
# =============================================================================
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_SKIPPED=0
CURRENT_TEST=""
TEST_START_TIME=0

# =============================================================================
# Logging Functions
# =============================================================================
log_info() {
    local msg="$1"
    echo -e "${BLUE}[INFO]${NC} $msg"
    [[ -n "$TEST_LOG" ]] && echo "[INFO] $(date '+%H:%M:%S') $msg" >> "$TEST_LOG"
}

log_success() {
    local msg="$1"
    echo -e "${GREEN}[PASS]${NC} $msg"
    [[ -n "$TEST_LOG" ]] && echo "[PASS] $(date '+%H:%M:%S') $msg" >> "$TEST_LOG"
}

log_error() {
    local msg="$1"
    echo -e "${RED}[FAIL]${NC} $msg"
    [[ -n "$TEST_LOG" ]] && echo "[FAIL] $(date '+%H:%M:%S') $msg" >> "$TEST_LOG"
}

log_warning() {
    local msg="$1"
    echo -e "${YELLOW}[WARN]${NC} $msg"
    [[ -n "$TEST_LOG" ]] && echo "[WARN] $(date '+%H:%M:%S') $msg" >> "$TEST_LOG"
}

log_debug() {
    local msg="$1"
    if [[ "${DEBUG:-false}" == "true" ]]; then
        echo -e "${CYAN}[DEBUG]${NC} $msg"
    fi
    [[ -n "$TEST_LOG" ]] && echo "[DEBUG] $(date '+%H:%M:%S') $msg" >> "$TEST_LOG"
}

log_step() {
    local step="$1"
    echo -e "${BOLD}  -> ${NC}$step"
    [[ -n "$TEST_LOG" ]] && echo "  -> $(date '+%H:%M:%S') $step" >> "$TEST_LOG"
}

# =============================================================================
# Test Lifecycle Functions
# =============================================================================
test_start() {
    local test_name="$1"
    CURRENT_TEST="$test_name"
    TEST_START_TIME=$(date +%s)

    echo ""
    echo -e "${BOLD}=========================================="
    echo -e "TEST: $test_name"
    echo -e "==========================================${NC}"
    [[ -n "$TEST_LOG" ]] && echo "" >> "$TEST_LOG"
    [[ -n "$TEST_LOG" ]] && echo "=== TEST: $test_name ===" >> "$TEST_LOG"
}

test_end() {
    local test_name="${1:-$CURRENT_TEST}"
    local end_time=$(date +%s)
    local duration=$((end_time - TEST_START_TIME))

    echo "------------------------------------------"
    echo -e "Duration: ${duration}s"
    [[ -n "$TEST_LOG" ]] && echo "Duration: ${duration}s" >> "$TEST_LOG"
}

test_skip() {
    local reason="$1"
    log_warning "SKIPPED: $reason"
    ((TESTS_SKIPPED++))
}

# =============================================================================
# Assertion Functions
# =============================================================================
assert_equals() {
    local expected="$1"
    local actual="$2"
    local message="${3:-Values should be equal}"

    if [[ "$expected" == "$actual" ]]; then
        log_success "$message"
        ((TESTS_PASSED++))
        return 0
    else
        log_error "$message"
        log_error "  Expected: '$expected'"
        log_error "  Actual:   '$actual'"
        ((TESTS_FAILED++))
        capture_failure_screenshot
        return 1
    fi
}

assert_not_equals() {
    local not_expected="$1"
    local actual="$2"
    local message="${3:-Values should not be equal}"

    if [[ "$not_expected" != "$actual" ]]; then
        log_success "$message"
        ((TESTS_PASSED++))
        return 0
    else
        log_error "$message"
        log_error "  Should not be: '$not_expected'"
        log_error "  But got:       '$actual'"
        ((TESTS_FAILED++))
        capture_failure_screenshot
        return 1
    fi
}

assert_contains() {
    local haystack="$1"
    local needle="$2"
    local message="${3:-String should contain substring}"

    if [[ "$haystack" == *"$needle"* ]]; then
        log_success "$message"
        ((TESTS_PASSED++))
        return 0
    else
        log_error "$message"
        log_error "  String: '${haystack:0:100}...'"
        log_error "  Should contain: '$needle'"
        ((TESTS_FAILED++))
        capture_failure_screenshot
        return 1
    fi
}

assert_not_contains() {
    local haystack="$1"
    local needle="$2"
    local message="${3:-String should not contain substring}"

    if [[ "$haystack" != *"$needle"* ]]; then
        log_success "$message"
        ((TESTS_PASSED++))
        return 0
    else
        log_error "$message"
        log_error "  String should NOT contain: '$needle'"
        ((TESTS_FAILED++))
        capture_failure_screenshot
        return 1
    fi
}

assert_http_status() {
    local actual="$1"
    local expected="$2"
    local endpoint="$3"

    assert_equals "$expected" "$actual" "HTTP $expected for $endpoint"
}

assert_http_success() {
    local actual="$1"
    local endpoint="$2"

    if [[ "$actual" =~ ^2[0-9][0-9]$ ]]; then
        log_success "HTTP success ($actual) for $endpoint"
        ((TESTS_PASSED++))
        return 0
    else
        log_error "Expected HTTP 2xx for $endpoint, got $actual"
        ((TESTS_FAILED++))
        capture_failure_screenshot
        return 1
    fi
}

assert_true() {
    local condition="$1"
    local message="${2:-Condition should be true}"

    if [[ "$condition" == "true" || "$condition" == "1" || -n "$condition" ]]; then
        log_success "$message"
        ((TESTS_PASSED++))
        return 0
    else
        log_error "$message"
        ((TESTS_FAILED++))
        capture_failure_screenshot
        return 1
    fi
}

assert_false() {
    local condition="$1"
    local message="${2:-Condition should be false}"

    if [[ "$condition" == "false" || "$condition" == "0" || -z "$condition" ]]; then
        log_success "$message"
        ((TESTS_PASSED++))
        return 0
    else
        log_error "$message"
        ((TESTS_FAILED++))
        capture_failure_screenshot
        return 1
    fi
}

assert_file_exists() {
    local filepath="$1"
    local message="${2:-File should exist: $filepath}"

    if [[ -f "$filepath" ]]; then
        log_success "$message"
        ((TESTS_PASSED++))
        return 0
    else
        log_error "$message"
        ((TESTS_FAILED++))
        return 1
    fi
}

assert_json_field() {
    local json="$1"
    local field="$2"
    local expected="$3"
    local message="${4:-JSON field $field should equal $expected}"

    local actual
    actual=$(echo "$json" | jq -r "$field" 2>/dev/null)

    if [[ "$actual" == "$expected" ]]; then
        log_success "$message"
        ((TESTS_PASSED++))
        return 0
    else
        log_error "$message"
        log_error "  Expected: '$expected'"
        log_error "  Actual:   '$actual'"
        ((TESTS_FAILED++))
        return 1
    fi
}

assert_json_field_exists() {
    local json="$1"
    local field="$2"
    local message="${3:-JSON field $field should exist}"

    local value
    value=$(echo "$json" | jq -r "$field" 2>/dev/null)

    if [[ "$value" != "null" && -n "$value" ]]; then
        log_success "$message"
        ((TESTS_PASSED++))
        return 0
    else
        log_error "$message"
        ((TESTS_FAILED++))
        return 1
    fi
}

# =============================================================================
# Failure Handling
# =============================================================================
capture_failure_screenshot() {
    if [[ "${SCREENSHOT_ON_FAILURE:-true}" == "true" ]]; then
        if command -v adb &>/dev/null && adb devices 2>/dev/null | grep -q "device$"; then
            local screenshot_name="failure_${CURRENT_TEST//[^a-zA-Z0-9]/_}_$(date +%H%M%S).png"
            local screenshot_path="${SCREENSHOTS_DIR:-/tmp}/${screenshot_name}"
            adb exec-out screencap -p > "$screenshot_path" 2>/dev/null
            if [[ -s "$screenshot_path" ]]; then
                log_debug "Failure screenshot saved: $screenshot_path"
            fi
        fi
    fi
}

# =============================================================================
# Report Generation
# =============================================================================
print_summary() {
    local total=$((TESTS_PASSED + TESTS_FAILED + TESTS_SKIPPED))
    local pass_rate=0
    if [[ $total -gt 0 ]]; then
        pass_rate=$((TESTS_PASSED * 100 / total))
    fi

    echo ""
    echo -e "${BOLD}=========================================="
    echo -e "TEST SUMMARY"
    echo -e "==========================================${NC}"
    echo -e "${GREEN}Passed:${NC}  $TESTS_PASSED"
    echo -e "${RED}Failed:${NC}  $TESTS_FAILED"
    echo -e "${YELLOW}Skipped:${NC} $TESTS_SKIPPED"
    echo -e "Total:   $total"
    echo -e "Pass Rate: ${pass_rate}%"
    echo "=========================================="

    if [[ -n "$TEST_LOG" ]]; then
        echo "" >> "$TEST_LOG"
        echo "=== SUMMARY ===" >> "$TEST_LOG"
        echo "Passed: $TESTS_PASSED" >> "$TEST_LOG"
        echo "Failed: $TESTS_FAILED" >> "$TEST_LOG"
        echo "Skipped: $TESTS_SKIPPED" >> "$TEST_LOG"
        echo "Pass Rate: ${pass_rate}%" >> "$TEST_LOG"
    fi

    if [[ $TESTS_FAILED -gt 0 ]]; then
        return 1
    fi
    return 0
}

generate_html_report() {
    local output_file="${1:-${REPORTS_DIR}/report_${TEST_SESSION_ID}.html}"
    local total=$((TESTS_PASSED + TESTS_FAILED + TESTS_SKIPPED))

    cat > "$output_file" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>E2E Test Report - $TEST_SESSION_ID</title>
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 20px; }
        .header { background: #333; color: white; padding: 20px; border-radius: 8px; }
        .summary { display: flex; gap: 20px; margin: 20px 0; }
        .stat { padding: 15px 25px; border-radius: 8px; text-align: center; }
        .passed { background: #d4edda; color: #155724; }
        .failed { background: #f8d7da; color: #721c24; }
        .skipped { background: #fff3cd; color: #856404; }
        .log { background: #f5f5f5; padding: 15px; border-radius: 8px; font-family: monospace; white-space: pre-wrap; max-height: 500px; overflow-y: auto; }
    </style>
</head>
<body>
    <div class="header">
        <h1>E2E Test Report</h1>
        <p>Session: $TEST_SESSION_ID | Generated: $(date)</p>
    </div>
    <div class="summary">
        <div class="stat passed"><h2>$TESTS_PASSED</h2><p>Passed</p></div>
        <div class="stat failed"><h2>$TESTS_FAILED</h2><p>Failed</p></div>
        <div class="stat skipped"><h2>$TESTS_SKIPPED</h2><p>Skipped</p></div>
    </div>
    <h2>Test Log</h2>
    <div class="log">$(cat "$TEST_LOG" 2>/dev/null || echo "No log available")</div>
</body>
</html>
EOF

    log_info "HTML report generated: $output_file"
}

# =============================================================================
# Utility Functions
# =============================================================================
wait_for() {
    local seconds="$1"
    local message="${2:-Waiting ${seconds}s...}"
    log_debug "$message"
    sleep "$seconds"
}

retry() {
    local max_attempts="${1:-3}"
    local delay="${2:-1}"
    shift 2
    local cmd="$@"

    local attempt=1
    while [[ $attempt -le $max_attempts ]]; do
        if eval "$cmd"; then
            return 0
        fi
        log_debug "Attempt $attempt failed, retrying in ${delay}s..."
        sleep "$delay"
        ((attempt++))
    done

    log_error "Command failed after $max_attempts attempts: $cmd"
    return 1
}

# Cleanup handler
cleanup() {
    log_info "Cleaning up test session..."
    # Add cleanup logic here if needed
}

trap cleanup EXIT

echo "Common utilities loaded"
