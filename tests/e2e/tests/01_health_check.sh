#!/bin/bash
# Test: API Health Check
# Verifies backend is running and healthy

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$SCRIPT_DIR/config.sh"
source "$SCRIPT_DIR/lib/common.sh"
source "$SCRIPT_DIR/lib/api.sh"

test_start "API Health Check"

# =============================================================================
# Test 1: Main health endpoint
# =============================================================================
log_info "Testing /api/health endpoint..."
response=$(api_health)
code=$(get_http_code "$response")
body=$(get_response_body "$response")

assert_http_status "$code" "200" "/api/health"
assert_contains "$body" "healthy" "Health response should indicate healthy status"

# Verify database connection
db_connected=$(json_get "$body" ".database.connected")
assert_equals "true" "$db_connected" "Database should be connected"

# Check map matching status if configured
map_matching_enabled=$(json_get "$body" ".external_services.map_matching.enabled" 2>/dev/null || echo "false")
log_info "Map matching enabled: $map_matching_enabled"

# =============================================================================
# Test 2: Liveness probe
# =============================================================================
log_info "Testing /api/health/live endpoint..."
response=$(api_health_live)
code=$(get_http_code "$response")

assert_http_status "$code" "200" "/api/health/live"

# =============================================================================
# Test 3: Readiness probe
# =============================================================================
log_info "Testing /api/health/ready endpoint..."
response=$(api_health_ready)
code=$(get_http_code "$response")

assert_http_status "$code" "200" "/api/health/ready"

# =============================================================================
# Test 4: Non-existent resource returns 404
# =============================================================================
log_info "Testing non-existent resource (geofence)..."
# Request a non-existent geofence ID to test proper API 404 handling
# Using a valid UUID format that doesn't exist
response=$(api_request "GET" "/api/v1/geofences/00000000-0000-0000-0000-000000000000")
code=$(get_http_code "$response")

assert_http_status "$code" "404" "Non-existent geofence"

test_end
print_summary
