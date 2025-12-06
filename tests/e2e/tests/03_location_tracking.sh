#!/bin/bash
# Test: Location Tracking
# Tests location upload (single and batch) and history retrieval

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$SCRIPT_DIR/config.sh"
source "$SCRIPT_DIR/lib/common.sh"
source "$SCRIPT_DIR/lib/api.sh"
source "$SCRIPT_DIR/lib/generators.sh"

test_start "Location Tracking"

# =============================================================================
# Setup: Register test device
# =============================================================================
log_info "Setting up test device..."
device_info=$(generate_device)
DEVICE_ID=$(echo "$device_info" | cut -d'|' -f1)
DEVICE_NAME=$(echo "$device_info" | cut -d'|' -f2)
GROUP_ID="location-test-$(generate_short_id)"

response=$(api_register_device "$DEVICE_ID" "$DEVICE_NAME" "$GROUP_ID")
code=$(get_http_code "$response")
body=$(get_response_body "$response")

if [[ "$code" != "200" && "$code" != "201" ]]; then
    log_error "Failed to register test device"
    exit 1
fi

# Note: API_KEY is already set in config.sh (same key is used for all operations)
log_info "Device registered: $DEVICE_NAME"

# =============================================================================
# Test 1: Upload single location
# =============================================================================
log_step "Uploading single location..."
response=$(api_upload_location "$DEVICE_ID" "$SF_DOWNTOWN_LAT" "$SF_DOWNTOWN_LON" "15.0")
code=$(get_http_code "$response")

assert_http_success "$code" "POST /api/v1/locations"

# =============================================================================
# Test 2: Upload batch of locations
# =============================================================================
log_step "Uploading batch of 25 locations..."
locations=$(generate_location_path \
    "$SF_DOWNTOWN_LAT" "$SF_DOWNTOWN_LON" \
    "$OAKLAND_LAT" "$OAKLAND_LON" \
    25)

response=$(api_upload_locations_batch "$DEVICE_ID" "$locations")
code=$(get_http_code "$response")
body=$(get_response_body "$response")

assert_http_success "$code" "POST /api/v1/locations/batch"

# Verify count in response
count=$(json_get "$body" ".count" 2>/dev/null || json_get "$body" ".processed" 2>/dev/null || echo "25")
log_info "Batch uploaded: $count locations"

# =============================================================================
# Test 3: Get location history
# =============================================================================
log_step "Retrieving location history..."
response=$(api_get_location_history "$DEVICE_ID" 50)
code=$(get_http_code "$response")
body=$(get_response_body "$response")

assert_http_success "$code" "GET /api/v1/devices/$DEVICE_ID/locations"

# Verify we got locations back
location_count=$(json_get "$body" ".locations | length" 2>/dev/null || echo "0")
if [[ "$location_count" -gt 0 ]]; then
    log_success "Retrieved $location_count locations"
    ((TESTS_PASSED++))
else
    log_error "No locations returned"
    ((TESTS_FAILED++))
fi

# =============================================================================
# Test 4: Location history with time filter
# =============================================================================
log_step "Testing location history with time filter..."
# Get locations from last hour
from_time=$(($(date +%s) - 3600))000
response=$(api_get_location_history "$DEVICE_ID" 50 "$from_time")
code=$(get_http_code "$response")

assert_http_success "$code" "Location history with time filter"

# =============================================================================
# Test 5: Location history with simplification
# =============================================================================
log_step "Testing trajectory simplification..."
response=$(api_get_location_history "$DEVICE_ID" 100 "" "" "100")  # 100m tolerance
code=$(get_http_code "$response")
body=$(get_response_body "$response")

assert_http_success "$code" "Location history with simplification"

# Check for simplification metadata
original_count=$(json_get "$body" ".original_count" 2>/dev/null || echo "unknown")
simplified_count=$(json_get "$body" ".simplified_count" 2>/dev/null || echo "unknown")
log_info "Simplification: $original_count -> $simplified_count points"

# =============================================================================
# Test 6: Maximum batch size (50 locations)
# =============================================================================
log_step "Testing max batch size (50 locations)..."
max_locations=$(generate_location_path \
    "$SF_DOWNTOWN_LAT" "$SF_DOWNTOWN_LON" \
    "$PALO_ALTO_LAT" "$PALO_ALTO_LON" \
    50)

response=$(api_upload_locations_batch "$DEVICE_ID" "$max_locations")
code=$(get_http_code "$response")

assert_http_success "$code" "Max batch size (50 locations)"

# =============================================================================
# Test 7: Coordinate validation - valid boundaries
# =============================================================================
log_step "Testing coordinate boundary validation..."

# Valid: North Pole area
response=$(api_upload_location "$DEVICE_ID" "89.9" "0" "10.0")
code=$(get_http_code "$response")
assert_http_success "$code" "Valid coordinate (89.9, 0)"

# Valid: South Pole area
response=$(api_upload_location "$DEVICE_ID" "-89.9" "0" "10.0")
code=$(get_http_code "$response")
assert_http_success "$code" "Valid coordinate (-89.9, 0)"

# Valid: Date line
response=$(api_upload_location "$DEVICE_ID" "0" "179.9" "10.0")
code=$(get_http_code "$response")
assert_http_success "$code" "Valid coordinate (0, 179.9)"

# =============================================================================
# Test 8: Coordinate validation - invalid values
# =============================================================================
log_step "Testing invalid coordinate rejection..."

# Invalid latitude > 90
invalid_location='{"timestamp": '$(date +%s000)', "latitude": 91.0, "longitude": 0, "accuracy": 10}'
payload='{"device_id": "'$DEVICE_ID'", "locations": ['"$invalid_location"']}'
response=$(api_request "POST" "/api/v1/locations/batch" "$payload")
code=$(get_http_code "$response")

if [[ "$code" == "400" || "$code" == "422" ]]; then
    log_success "Invalid latitude (91) rejected"
    ((TESTS_PASSED++))
else
    log_error "Invalid latitude should be rejected, got HTTP $code"
    ((TESTS_FAILED++))
fi

# =============================================================================
# Test 9: Empty batch handling
# =============================================================================
log_step "Testing empty batch handling..."
response=$(api_upload_locations_batch "$DEVICE_ID" "[]")
code=$(get_http_code "$response")

# Should either succeed with 0 count or reject as invalid
if [[ "$code" =~ ^[24][0-9][0-9]$ ]]; then
    log_success "Empty batch handled (HTTP $code)"
    ((TESTS_PASSED++))
else
    log_warning "Unexpected response for empty batch: HTTP $code"
fi

test_end
print_summary
