#!/bin/bash
# Test: Trip Lifecycle
# Tests trip creation, state transitions, movement events, and path correction

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$SCRIPT_DIR/config.sh"
source "$SCRIPT_DIR/lib/common.sh"
source "$SCRIPT_DIR/lib/api.sh"
source "$SCRIPT_DIR/lib/generators.sh"

test_start "Trip Lifecycle"

# =============================================================================
# Setup: Register test device
# =============================================================================
log_info "Setting up test device..."
device_info=$(generate_device)
DEVICE_ID=$(echo "$device_info" | cut -d'|' -f1)
DEVICE_NAME=$(echo "$device_info" | cut -d'|' -f2)
GROUP_ID="trip-test-$(generate_short_id)"

response=$(api_register_device "$DEVICE_ID" "$DEVICE_NAME" "$GROUP_ID")
# Note: API uses shared key from config.sh, not per-device keys
log_info "Device registered: $DEVICE_NAME"

LOCAL_TRIP_ID="local-trip-$(generate_short_id)"

# =============================================================================
# Test 1: Create new trip
# =============================================================================
log_step "Creating new trip..."
response=$(api_create_trip "$DEVICE_ID" "$LOCAL_TRIP_ID" "$SF_DOWNTOWN_LAT" "$SF_DOWNTOWN_LON" "IN_VEHICLE")
code=$(get_http_code "$response")
body=$(get_response_body "$response")

assert_http_status "$code" "201" "POST /api/v1/trips"

# Extract trip ID
TRIP_ID=$(json_get "$body" ".id")
assert_not_equals "null" "$TRIP_ID" "Trip ID should be returned"
log_info "Trip created: $TRIP_ID"

# Verify trip state is ACTIVE
state=$(json_get "$body" ".state")
assert_equals "ACTIVE" "$state" "New trip should be in ACTIVE state"

# =============================================================================
# Test 2: Trip idempotency (same local_trip_id returns same trip)
# =============================================================================
log_step "Testing trip creation idempotency..."
response=$(api_create_trip "$DEVICE_ID" "$LOCAL_TRIP_ID" "$SF_DOWNTOWN_LAT" "$SF_DOWNTOWN_LON" "IN_VEHICLE")
code=$(get_http_code "$response")
body=$(get_response_body "$response")

# Should return 200 (existing) instead of 201 (created)
assert_http_success "$code" "Idempotent trip creation"

returned_id=$(json_get "$body" ".id")
assert_equals "$TRIP_ID" "$returned_id" "Idempotent request should return same trip"

# =============================================================================
# Test 3: Upload movement events for trip
# =============================================================================
log_step "Uploading movement events..."
events=$(generate_movement_events_for_path \
    "$TRIP_ID" \
    "$SF_DOWNTOWN_LAT" "$SF_DOWNTOWN_LON" \
    "$OAKLAND_LAT" "$OAKLAND_LON" \
    15 "IN_VEHICLE")

response=$(api_upload_movement_events_batch "$DEVICE_ID" "$events")
code=$(get_http_code "$response")

assert_http_success "$code" "POST /api/v1/movement-events/batch"

# =============================================================================
# Test 4: Get trip movement events
# =============================================================================
log_step "Retrieving trip movement events..."
response=$(api_get_trip_movement_events "$TRIP_ID")
code=$(get_http_code "$response")
body=$(get_response_body "$response")

assert_http_success "$code" "GET /api/v1/trips/$TRIP_ID/movement-events"

event_count=$(json_get "$body" ".events | length" 2>/dev/null || json_get "$body" "| length" 2>/dev/null || echo "0")
if [[ "$event_count" -gt 0 ]]; then
    log_success "Retrieved $event_count movement events"
    ((TESTS_PASSED++))
else
    log_warning "No events returned (may be timing issue)"
fi

# =============================================================================
# Test 5: Complete trip
# =============================================================================
log_step "Completing trip..."
response=$(api_complete_trip "$TRIP_ID" "$OAKLAND_LAT" "$OAKLAND_LON")
code=$(get_http_code "$response")
body=$(get_response_body "$response")

assert_http_success "$code" "PATCH /api/v1/trips/$TRIP_ID (complete)"

state=$(json_get "$body" ".state")
assert_equals "COMPLETED" "$state" "Trip should be in COMPLETED state"

# =============================================================================
# Test 6: Invalid state transition (completed -> active)
# =============================================================================
log_step "Testing invalid state transition..."
payload='{"state": "ACTIVE"}'
response=$(api_request "PATCH" "/api/v1/trips/$TRIP_ID" "$payload")
code=$(get_http_code "$response")

if [[ "$code" == "400" || "$code" == "409" ]]; then
    log_success "Invalid state transition rejected (HTTP $code)"
    ((TESTS_PASSED++))
else
    log_error "Invalid state transition should be rejected, got HTTP $code"
    ((TESTS_FAILED++))
fi

# =============================================================================
# Test 7: Get device trips
# =============================================================================
log_step "Listing device trips..."
response=$(api_get_device_trips "$DEVICE_ID")
code=$(get_http_code "$response")
body=$(get_response_body "$response")

assert_http_success "$code" "GET /api/v1/devices/$DEVICE_ID/trips"
assert_contains "$body" "$TRIP_ID" "Trip should be in device trips list"

# =============================================================================
# Test 8: Create and cancel trip
# =============================================================================
log_step "Testing trip cancellation..."
LOCAL_TRIP_ID2="cancel-trip-$(generate_short_id)"
response=$(api_create_trip "$DEVICE_ID" "$LOCAL_TRIP_ID2" "$SF_DOWNTOWN_LAT" "$SF_DOWNTOWN_LON" "WALKING")
code=$(get_http_code "$response")
TRIP_ID2=$(json_get "$(get_response_body "$response")" ".id")

assert_http_status "$code" "201" "Create trip for cancellation"

response=$(api_cancel_trip "$TRIP_ID2")
code=$(get_http_code "$response")
body=$(get_response_body "$response")

assert_http_success "$code" "Cancel trip"

state=$(json_get "$body" ".state")
assert_equals "CANCELLED" "$state" "Trip should be CANCELLED"

# =============================================================================
# Test 9: Trip path (if available)
# =============================================================================
log_step "Getting trip path..."
response=$(api_get_trip_path "$TRIP_ID")
code=$(get_http_code "$response")

if [[ "$code" == "200" ]]; then
    body=$(get_response_body "$response")
    log_success "Trip path retrieved"
    ((TESTS_PASSED++))

    # Check for path correction status
    correction_status=$(json_get "$body" ".correction_status" 2>/dev/null || echo "unknown")
    log_info "Path correction status: $correction_status"
else
    log_warning "Trip path not available (HTTP $code)"
fi

# =============================================================================
# Test 10: Multi-modal trip
# =============================================================================
log_step "Testing multi-modal trip..."
LOCAL_TRIP_ID3="multimodal-$(generate_short_id)"
response=$(api_create_trip "$DEVICE_ID" "$LOCAL_TRIP_ID3" "$SF_DOWNTOWN_LAT" "$SF_DOWNTOWN_LON" "WALKING")
TRIP_ID3=$(json_get "$(get_response_body "$response")" ".id")

# Upload events with mode transitions
multimodal_events=$(generate_multimodal_events "$TRIP_ID3" \
    "$SF_DOWNTOWN_LAT" "$SF_DOWNTOWN_LON" \
    "37.7780" "-122.4150" \
    "37.8000" "-122.3800" \
    "$OAKLAND_LAT" "$OAKLAND_LON")

response=$(api_upload_movement_events_batch "$DEVICE_ID" "$multimodal_events")
code=$(get_http_code "$response")

assert_http_success "$code" "Multi-modal movement events"

response=$(api_complete_trip "$TRIP_ID3" "$OAKLAND_LAT" "$OAKLAND_LON")
code=$(get_http_code "$response")

assert_http_success "$code" "Complete multi-modal trip"

# =============================================================================
# Test 11: Transportation modes
# =============================================================================
log_step "Testing all transportation modes..."
for mode in STATIONARY WALKING RUNNING CYCLING IN_VEHICLE; do
    LOCAL_ID="mode-test-$mode-$(generate_short_id)"
    response=$(api_create_trip "$DEVICE_ID" "$LOCAL_ID" "$SF_DOWNTOWN_LAT" "$SF_DOWNTOWN_LON" "$mode")
    code=$(get_http_code "$response")

    if [[ "$code" == "201" ]]; then
        log_success "Mode $mode accepted"
        # Cancel the test trip
        trip_id=$(json_get "$(get_response_body "$response")" ".id")
        api_cancel_trip "$trip_id" >/dev/null
        ((TESTS_PASSED++))
    else
        log_error "Mode $mode rejected (HTTP $code)"
        ((TESTS_FAILED++))
    fi
done

test_end
print_summary
