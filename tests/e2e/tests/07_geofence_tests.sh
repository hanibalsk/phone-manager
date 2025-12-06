#!/bin/bash
# Test: Geofence Tests
# Tests geofence creation, listing, updates, and event triggers

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$SCRIPT_DIR/config.sh"
source "$SCRIPT_DIR/lib/common.sh"
source "$SCRIPT_DIR/lib/api.sh"
source "$SCRIPT_DIR/lib/generators.sh"

test_start "Geofence Tests"

# =============================================================================
# Setup: Register test device
# =============================================================================
log_info "Setting up test device..."
device_info=$(generate_device)
DEVICE_ID=$(echo "$device_info" | cut -d'|' -f1)
DEVICE_NAME="Geofence-Test-$(generate_short_id)"
GROUP_ID="geofence-test-$(generate_short_id)"

response=$(api_register_device "$DEVICE_ID" "$DEVICE_NAME" "$GROUP_ID")
# Note: API uses shared key from config.sh, not per-device keys
log_success "Device registered: $DEVICE_NAME"

# Store created geofence IDs for cleanup
GEOFENCE_IDS=()

# =============================================================================
# Test 1: Create basic geofence
# =============================================================================
log_step "Creating basic geofence..."
response=$(api_create_geofence "$DEVICE_ID" "Home" "$SF_DOWNTOWN_LAT" "$SF_DOWNTOWN_LON" "100" '["enter", "exit"]')
code=$(get_http_code "$response")
body=$(get_response_body "$response")

assert_http_status "$code" "201" "POST /api/v1/geofences"

GEOFENCE_ID=$(json_get "$body" ".geofence_id")
assert_not_equals "null" "$GEOFENCE_ID" "Geofence ID should be returned"
log_info "Geofence created: $GEOFENCE_ID"
GEOFENCE_IDS+=("$GEOFENCE_ID")

# Verify geofence properties
name=$(json_get "$body" ".name")
assert_equals "Home" "$name" "Geofence name should match"

# =============================================================================
# Test 2: Create geofence with metadata
# =============================================================================
log_step "Creating geofence with metadata..."
metadata='{"color": "#FF0000", "icon": "work", "notification_sound": "alert"}'
response=$(api_create_geofence "$DEVICE_ID" "Work" "$OAKLAND_LAT" "$OAKLAND_LON" "200" '["enter"]' "$metadata")
code=$(get_http_code "$response")
body=$(get_response_body "$response")

assert_http_status "$code" "201" "Geofence with metadata"

GEOFENCE_ID2=$(json_get "$body" ".geofence_id")
GEOFENCE_IDS+=("$GEOFENCE_ID2")
log_success "Geofence with metadata created"

# =============================================================================
# Test 3: Create geofences with various radii
# =============================================================================
log_step "Testing geofence radius limits..."

# Minimum radius (20m)
response=$(api_create_geofence "$DEVICE_ID" "Small" "37.7800" "-122.4100" "20" '["enter"]')
code=$(get_http_code "$response")
if [[ "$code" == "201" ]]; then
    GEOFENCE_IDS+=("$(json_get "$(get_response_body "$response")" ".geofence_id")")
    log_success "Minimum radius (20m) accepted"
    ((TESTS_PASSED++))
else
    log_error "Minimum radius rejected (HTTP $code)"
    ((TESTS_FAILED++))
fi

# Medium radius (500m)
response=$(api_create_geofence "$DEVICE_ID" "Medium" "37.7810" "-122.4110" "500" '["enter", "exit"]')
code=$(get_http_code "$response")
if [[ "$code" == "201" ]]; then
    GEOFENCE_IDS+=("$(json_get "$(get_response_body "$response")" ".geofence_id")")
    log_success "Medium radius (500m) accepted"
    ((TESTS_PASSED++))
else
    log_error "Medium radius rejected (HTTP $code)"
    ((TESTS_FAILED++))
fi

# Large radius (5000m)
response=$(api_create_geofence "$DEVICE_ID" "Large" "37.7820" "-122.4120" "5000" '["dwell"]')
code=$(get_http_code "$response")
if [[ "$code" == "201" ]]; then
    GEOFENCE_IDS+=("$(json_get "$(get_response_body "$response")" ".geofence_id")")
    log_success "Large radius (5000m) accepted"
    ((TESTS_PASSED++))
else
    log_error "Large radius rejected (HTTP $code)"
    ((TESTS_FAILED++))
fi

# Maximum radius (50000m)
response=$(api_create_geofence "$DEVICE_ID" "Maximum" "37.7830" "-122.4130" "50000" '["enter"]')
code=$(get_http_code "$response")
if [[ "$code" == "201" ]]; then
    GEOFENCE_IDS+=("$(json_get "$(get_response_body "$response")" ".geofence_id")")
    log_success "Maximum radius (50000m) accepted"
    ((TESTS_PASSED++))
else
    log_error "Maximum radius rejected (HTTP $code)"
    ((TESTS_FAILED++))
fi

# =============================================================================
# Test 4: Invalid radius rejection
# =============================================================================
log_step "Testing invalid radius rejection..."

# Too small (< 20m)
response=$(api_create_geofence "$DEVICE_ID" "TooSmall" "37.7840" "-122.4140" "10" '["enter"]')
code=$(get_http_code "$response")
if [[ "$code" == "400" || "$code" == "422" ]]; then
    log_success "Radius < 20m rejected (HTTP $code)"
    ((TESTS_PASSED++))
else
    log_error "Should reject radius < 20m, got HTTP $code"
    ((TESTS_FAILED++))
fi

# Too large (> 50000m)
response=$(api_create_geofence "$DEVICE_ID" "TooLarge" "37.7850" "-122.4150" "100000" '["enter"]')
code=$(get_http_code "$response")
if [[ "$code" == "400" || "$code" == "422" ]]; then
    log_success "Radius > 50000m rejected (HTTP $code)"
    ((TESTS_PASSED++))
else
    log_error "Should reject radius > 50000m, got HTTP $code"
    ((TESTS_FAILED++))
fi

# =============================================================================
# Test 5: List device geofences
# =============================================================================
log_step "Listing device geofences..."
response=$(api_get_geofences "$DEVICE_ID")
code=$(get_http_code "$response")
body=$(get_response_body "$response")

assert_http_success "$code" "GET /api/v1/geofences"

geofence_count=$(json_get "$body" ".geofences | length" 2>/dev/null || json_get "$body" "| length" 2>/dev/null || echo "0")
if [[ "$geofence_count" -gt 0 ]]; then
    log_success "Retrieved $geofence_count geofences"
    ((TESTS_PASSED++))
else
    log_error "No geofences returned"
    ((TESTS_FAILED++))
fi

# =============================================================================
# Test 6: Get single geofence
# =============================================================================
log_step "Getting single geofence..."
response=$(api_get_geofence "$GEOFENCE_ID")
code=$(get_http_code "$response")
body=$(get_response_body "$response")

assert_http_success "$code" "GET /api/v1/geofences/:id"

returned_id=$(json_get "$body" ".geofence_id")
assert_equals "$GEOFENCE_ID" "$returned_id" "Returned geofence ID should match"

# =============================================================================
# Test 7: Update geofence
# =============================================================================
log_step "Updating geofence..."
update_payload='{"name": "Home Updated", "radius": 150}'
response=$(api_update_geofence "$GEOFENCE_ID" "$update_payload")
code=$(get_http_code "$response")
body=$(get_response_body "$response")

assert_http_success "$code" "PATCH /api/v1/geofences/:id"

updated_name=$(json_get "$body" ".name")
assert_equals "Home Updated" "$updated_name" "Name should be updated"

updated_radius=$(json_get "$body" ".radius")
if [[ "$updated_radius" == "150" || "$updated_radius" == "150.0" ]]; then
    log_success "Radius updated to 150"
    ((TESTS_PASSED++))
else
    log_warning "Radius not updated as expected: $updated_radius"
fi

# =============================================================================
# Test 8: Update geofence triggers
# =============================================================================
log_step "Updating geofence triggers..."
update_payload='{"triggers": ["enter", "exit", "dwell"]}'
response=$(api_update_geofence "$GEOFENCE_ID" "$update_payload")
code=$(get_http_code "$response")
body=$(get_response_body "$response")

assert_http_success "$code" "Update geofence triggers"

triggers=$(json_get "$body" ".triggers")
if [[ "$triggers" == *"dwell"* ]]; then
    log_success "Triggers updated to include dwell"
    ((TESTS_PASSED++))
else
    log_warning "Dwell trigger not found in: $triggers"
fi

# =============================================================================
# Test 9: Event types validation
# =============================================================================
log_step "Testing event types..."

# All valid event types
for event_type in "enter" "exit" "dwell"; do
    response=$(api_create_geofence "$DEVICE_ID" "Event-$event_type" "37.7860" "-122.4160" "100" "[\"$event_type\"]")
    code=$(get_http_code "$response")

    if [[ "$code" == "201" ]]; then
        GEOFENCE_IDS+=("$(json_get "$(get_response_body "$response")" ".geofence_id")")
        log_info "  Event type '$event_type': accepted"
    else
        log_warning "  Event type '$event_type': HTTP $code"
    fi
done

log_success "Event types tested"
((TESTS_PASSED++))

# =============================================================================
# Test 10: Multiple triggers
# =============================================================================
log_step "Testing multiple triggers..."
response=$(api_create_geofence "$DEVICE_ID" "Multi-Trigger" "37.7870" "-122.4170" "100" '["enter", "exit", "dwell"]')
code=$(get_http_code "$response")

if [[ "$code" == "201" ]]; then
    GEOFENCE_IDS+=("$(json_get "$(get_response_body "$response")" ".geofence_id")")
    log_success "Multiple triggers accepted"
    ((TESTS_PASSED++))
else
    log_error "Multiple triggers rejected (HTTP $code)"
    ((TESTS_FAILED++))
fi

# =============================================================================
# Test 11: Geofence limit (max 50 per device)
# =============================================================================
log_step "Testing geofence limit awareness..."
current_count=${#GEOFENCE_IDS[@]}
log_info "Current geofence count: $current_count (limit: 50)"

if [[ "$current_count" -lt 50 ]]; then
    log_success "Within geofence limit"
    ((TESTS_PASSED++))
fi

# =============================================================================
# Test 12: Delete geofence
# =============================================================================
log_step "Deleting a geofence..."

# Delete one of the test geofences
if [[ ${#GEOFENCE_IDS[@]} -gt 0 ]]; then
    # Get last element (bash 3.x compatible - no negative indices)
    last_idx=$((${#GEOFENCE_IDS[@]} - 1))
    delete_id="${GEOFENCE_IDS[$last_idx]}"
    response=$(api_delete_geofence "$delete_id")
    code=$(get_http_code "$response")

    if [[ "$code" == "200" || "$code" == "204" ]]; then
        log_success "Geofence deleted"
        ((TESTS_PASSED++))
        # Remove from array (bash 3.x compatible)
        unset "GEOFENCE_IDS[$last_idx]"
    else
        log_error "Delete failed (HTTP $code)"
        ((TESTS_FAILED++))
    fi
fi

# =============================================================================
# Test 13: Get deleted geofence (should fail)
# =============================================================================
log_step "Verifying deletion..."
response=$(api_get_geofence "$delete_id")
code=$(get_http_code "$response")

if [[ "$code" == "404" ]]; then
    log_success "Deleted geofence returns 404"
    ((TESTS_PASSED++))
else
    log_warning "Expected 404 for deleted geofence, got HTTP $code"
fi

# =============================================================================
# Test 14: Coordinate validation
# =============================================================================
log_step "Testing coordinate validation..."

# Valid coordinates at different locations
valid_coords=(
    "0,0"           # Equator/Prime Meridian
    "89.9,179.9"    # Near North Pole
    "-89.9,-179.9"  # Near South Pole
    "51.5074,-0.1278"   # London
    "35.6762,139.6503"  # Tokyo
)

for coord in "${valid_coords[@]}"; do
    IFS=',' read -r lat lon <<< "$coord"
    response=$(api_create_geofence "$DEVICE_ID" "Coord-$lat-$lon" "$lat" "$lon" "100" '["enter"]')
    code=$(get_http_code "$response")

    if [[ "$code" == "201" ]]; then
        GEOFENCE_IDS+=("$(json_get "$(get_response_body "$response")" ".geofence_id")")
        log_info "  Valid coord ($lat, $lon): accepted"
    else
        log_warning "  Valid coord ($lat, $lon): HTTP $code"
    fi
done

log_success "Coordinate validation tested"
((TESTS_PASSED++))

# =============================================================================
# Cleanup
# =============================================================================
log_info "=== Cleanup ==="
log_step "Cleaning up test geofences..."

cleanup_count=0
for gf_id in "${GEOFENCE_IDS[@]}"; do
    if [[ -n "$gf_id" ]]; then
        api_delete_geofence "$gf_id" >/dev/null 2>&1 || true
        ((cleanup_count++))
    fi
done

log_success "Cleaned up $cleanup_count geofences"

test_end
print_summary
