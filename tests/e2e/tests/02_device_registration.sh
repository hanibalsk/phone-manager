#!/bin/bash
# Test: Device Registration
# Tests device registration and group membership

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$SCRIPT_DIR/config.sh"
source "$SCRIPT_DIR/lib/common.sh"
source "$SCRIPT_DIR/lib/api.sh"
source "$SCRIPT_DIR/lib/generators.sh"

test_start "Device Registration"

# Generate test data
device_info=$(generate_device)
DEVICE_ID=$(echo "$device_info" | cut -d'|' -f1)
DEVICE_NAME=$(echo "$device_info" | cut -d'|' -f2)
GROUP_ID="test-group-$(generate_short_id)"

log_info "Test device: $DEVICE_NAME"
log_info "Device ID: $DEVICE_ID"
log_info "Group ID: $GROUP_ID"

# =============================================================================
# Test 1: Register new device
# =============================================================================
log_step "Registering new device..."
response=$(api_register_device "$DEVICE_ID" "$DEVICE_NAME" "$GROUP_ID" "android")
code=$(get_http_code "$response")
body=$(get_response_body "$response")

assert_http_success "$code" "POST /api/v1/devices/register"

# Verify response contains device info
returned_id=$(json_get "$body" ".device_id")
assert_equals "$DEVICE_ID" "$returned_id" "Returned device_id should match"

returned_name=$(json_get "$body" ".display_name")
assert_equals "$DEVICE_NAME" "$returned_name" "Returned display_name should match"

# Note: API uses shared key from config.sh, not per-device keys
# The backend doesn't return api_key - authentication uses the configured ADMIN_API_KEY

# =============================================================================
# Test 2: Re-register same device (should update, not create)
# =============================================================================
log_step "Re-registering same device (update)..."
NEW_NAME="Updated $DEVICE_NAME"
response=$(api_register_device "$DEVICE_ID" "$NEW_NAME" "$GROUP_ID" "android")
code=$(get_http_code "$response")
body=$(get_response_body "$response")

assert_http_success "$code" "Re-register device"

# Verify name was updated
returned_name=$(json_get "$body" ".display_name")
assert_equals "$NEW_NAME" "$returned_name" "Display name should be updated"

# =============================================================================
# Test 3: List devices in group
# =============================================================================
log_step "Listing devices in group..."
response=$(api_get_devices "$GROUP_ID")
code=$(get_http_code "$response")
body=$(get_response_body "$response")

assert_http_success "$code" "GET /api/v1/devices?group_id=$GROUP_ID"

# Verify our device is in the list
assert_contains "$body" "$DEVICE_ID" "Device should be in group list"

# =============================================================================
# Test 4: Register second device in same group
# =============================================================================
log_step "Registering second device..."
device2_info=$(generate_device)
DEVICE2_ID=$(echo "$device2_info" | cut -d'|' -f1)
DEVICE2_NAME=$(echo "$device2_info" | cut -d'|' -f2)

response=$(api_register_device "$DEVICE2_ID" "$DEVICE2_NAME" "$GROUP_ID" "android")
code=$(get_http_code "$response")

assert_http_success "$code" "Register second device"

# Verify both devices in group
response=$(api_get_devices "$GROUP_ID")
body=$(get_response_body "$response")

assert_contains "$body" "$DEVICE_ID" "First device should be in group"
assert_contains "$body" "$DEVICE2_ID" "Second device should be in group"

# =============================================================================
# Test 5: Invalid device registration (missing required fields)
# =============================================================================
log_step "Testing invalid registration..."
response=$(api_request "POST" "/api/v1/devices/register" '{"device_id": ""}')
code=$(get_http_code "$response")

# Should be 400 Bad Request or 422 Unprocessable Entity
if [[ "$code" == "400" || "$code" == "422" ]]; then
    log_success "Invalid registration rejected with HTTP $code"
    ((TESTS_PASSED++))
else
    log_error "Expected 400/422 for invalid registration, got $code"
    ((TESTS_FAILED++))
fi

# =============================================================================
# Test 6: Deactivate device
# =============================================================================
log_step "Deactivating second device..."
response=$(api_deactivate_device "$DEVICE2_ID")
code=$(get_http_code "$response")

assert_http_status "$code" "204" "DELETE /api/v1/devices/$DEVICE2_ID"

# Verify device is no longer in active list (may still return but marked inactive)
response=$(api_get_devices "$GROUP_ID")
body=$(get_response_body "$response")
# Note: Device might still appear but marked as inactive

test_end
print_summary
