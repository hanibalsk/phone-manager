#!/bin/bash
# API helper functions for Phone Manager backend
# Provides curl wrappers for all API endpoints

# =============================================================================
# Core HTTP Functions
# =============================================================================

# Make an API request and return "http_code|body"
# Optional 5th parameter: use_admin_key - if "true", uses ADMIN_API_KEY instead of API_KEY
api_request() {
    local method="$1"
    local endpoint="$2"
    local data="${3:-}"
    local extra_headers="${4:-}"
    local use_admin_key="${5:-false}"

    local url="${API_BASE_URL}${endpoint}"
    local headers=(-H "Content-Type: application/json")

    # Add API key - use admin key if specified, otherwise use device key
    if [[ "$use_admin_key" == "true" && -n "$ADMIN_API_KEY" ]]; then
        headers+=(-H "X-API-Key: $ADMIN_API_KEY")
    elif [[ -n "$API_KEY" ]]; then
        headers+=(-H "X-API-Key: $API_KEY")
    fi

    # Add extra headers if provided
    if [[ -n "$extra_headers" ]]; then
        headers+=(-H "$extra_headers")
    fi

    local response
    local curl_opts=(-s -w "\n%{http_code}")

    if [[ "$method" == "GET" ]]; then
        response=$(curl "${curl_opts[@]}" "${headers[@]}" "$url" 2>/dev/null)
    elif [[ "$method" == "DELETE" ]]; then
        response=$(curl "${curl_opts[@]}" -X DELETE "${headers[@]}" "$url" 2>/dev/null)
    else
        response=$(curl "${curl_opts[@]}" -X "$method" "${headers[@]}" -d "$data" "$url" 2>/dev/null)
    fi

    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')

    echo "$http_code|$body"
}

# Make an admin API request (uses ADMIN_API_KEY)
api_admin_request() {
    local method="$1"
    local endpoint="$2"
    local data="${3:-}"
    local extra_headers="${4:-}"
    api_request "$method" "$endpoint" "$data" "$extra_headers" "true"
}

# Extract HTTP code from response
get_http_code() {
    echo "$1" | cut -d'|' -f1
}

# Extract body from response
get_response_body() {
    echo "$1" | cut -d'|' -f2-
}

# Extract JSON field using jq
json_get() {
    local json="$1"
    local path="$2"
    echo "$json" | jq -r "$path" 2>/dev/null
}

# =============================================================================
# Health Check Endpoints
# =============================================================================

api_health() {
    api_request "GET" "/api/health"
}

api_health_live() {
    api_request "GET" "/api/health/live"
}

api_health_ready() {
    api_request "GET" "/api/health/ready"
}

# Quick health check - returns true/false
api_is_healthy() {
    local response=$(api_health)
    local code=$(get_http_code "$response")
    [[ "$code" == "200" ]]
}

# =============================================================================
# Device Endpoints
# =============================================================================

api_register_device() {
    local device_id="$1"
    local display_name="$2"
    local group_id="$3"
    local platform="${4:-android}"
    local fcm_token="${5:-}"

    local payload=$(cat <<EOF
{
    "device_id": "$device_id",
    "display_name": "$display_name",
    "group_id": "$group_id",
    "platform": "$platform"$(if [[ -n "$fcm_token" ]]; then echo ", \"fcm_token\": \"$fcm_token\""; fi)
}
EOF
)
    # Device registration requires admin API key
    api_admin_request "POST" "/api/v1/devices/register" "$payload"
}

api_get_devices() {
    local group_id="$1"
    api_request "GET" "/api/v1/devices?group_id=$group_id"
}

api_deactivate_device() {
    local device_id="$1"
    api_request "DELETE" "/api/v1/devices/$device_id"
}

# =============================================================================
# Location Endpoints
# =============================================================================

api_upload_location() {
    local device_id="$1"
    local latitude="$2"
    local longitude="$3"
    local accuracy="${4:-10.0}"
    local timestamp="${5:-$(date +%s000)}"

    local payload=$(cat <<EOF
{
    "device_id": "$device_id",
    "timestamp": $timestamp,
    "latitude": $latitude,
    "longitude": $longitude,
    "accuracy": $accuracy
}
EOF
)
    api_request "POST" "/api/v1/locations" "$payload"
}

api_upload_locations_batch() {
    local device_id="$1"
    local locations_json="$2"

    local payload=$(cat <<EOF
{
    "device_id": "$device_id",
    "locations": $locations_json
}
EOF
)
    api_request "POST" "/api/v1/locations/batch" "$payload"
}

api_get_location_history() {
    local device_id="$1"
    local limit="${2:-50}"
    local from="${3:-}"
    local to="${4:-}"
    local tolerance="${5:-}"

    local params="limit=$limit"
    [[ -n "$from" ]] && params+="&from=$from"
    [[ -n "$to" ]] && params+="&to=$to"
    [[ -n "$tolerance" ]] && params+="&tolerance=$tolerance"

    api_request "GET" "/api/v1/devices/$device_id/locations?$params"
}

# =============================================================================
# Trip Endpoints
# =============================================================================

api_create_trip() {
    local device_id="$1"
    local local_trip_id="$2"
    local start_lat="$3"
    local start_lon="$4"
    local transport_mode="${5:-IN_VEHICLE}"
    local detection_source="${6:-ACTIVITY_RECOGNITION}"
    local timestamp="${7:-$(date +%s000)}"

    local payload=$(cat <<EOF
{
    "device_id": "$device_id",
    "local_trip_id": "$local_trip_id",
    "start_timestamp": $timestamp,
    "start_latitude": $start_lat,
    "start_longitude": $start_lon,
    "transportation_mode": "$transport_mode",
    "detection_source": "$detection_source"
}
EOF
)
    api_request "POST" "/api/v1/trips" "$payload"
}

api_complete_trip() {
    local trip_id="$1"
    local end_lat="$2"
    local end_lon="$3"
    local timestamp="${4:-$(date +%s000)}"

    local payload=$(cat <<EOF
{
    "state": "COMPLETED",
    "end_timestamp": $timestamp,
    "end_latitude": $end_lat,
    "end_longitude": $end_lon
}
EOF
)
    api_request "PATCH" "/api/v1/trips/$trip_id" "$payload"
}

api_cancel_trip() {
    local trip_id="$1"
    local timestamp="${2:-$(date +%s000)}"

    local payload=$(cat <<EOF
{
    "state": "CANCELLED",
    "end_timestamp": $timestamp
}
EOF
)
    api_request "PATCH" "/api/v1/trips/$trip_id" "$payload"
}

api_get_device_trips() {
    local device_id="$1"
    local limit="${2:-20}"
    local from="${3:-}"
    local to="${4:-}"

    local params="limit=$limit"
    [[ -n "$from" ]] && params+="&from=$from"
    [[ -n "$to" ]] && params+="&to=$to"

    api_request "GET" "/api/v1/devices/$device_id/trips?$params"
}

api_get_trip_path() {
    local trip_id="$1"
    api_request "GET" "/api/v1/trips/$trip_id/path"
}

api_correct_trip_path() {
    local trip_id="$1"
    api_request "POST" "/api/v1/trips/$trip_id/correct-path"
}

# =============================================================================
# Movement Event Endpoints
# =============================================================================

api_upload_movement_event() {
    local device_id="$1"
    local trip_id="$2"
    local latitude="$3"
    local longitude="$4"
    local transport_mode="${5:-IN_VEHICLE}"
    local confidence="${6:-0.9}"
    local timestamp="${7:-$(date +%s000)}"

    local payload=$(cat <<EOF
{
    "device_id": "$device_id",
    "trip_id": "$trip_id",
    "timestamp": $timestamp,
    "latitude": $latitude,
    "longitude": $longitude,
    "accuracy": 10.0,
    "transportation_mode": "$transport_mode",
    "confidence": $confidence,
    "detection_source": "ACTIVITY_RECOGNITION"
}
EOF
)
    api_request "POST" "/api/v1/movement-events" "$payload"
}

api_upload_movement_events_batch() {
    local device_id="$1"
    local events_json="$2"

    local payload=$(cat <<EOF
{
    "device_id": "$device_id",
    "events": $events_json
}
EOF
)
    api_request "POST" "/api/v1/movement-events/batch" "$payload"
}

api_get_device_movement_events() {
    local device_id="$1"
    local limit="${2:-50}"
    api_request "GET" "/api/v1/devices/$device_id/movement-events?limit=$limit"
}

api_get_trip_movement_events() {
    local trip_id="$1"
    api_request "GET" "/api/v1/trips/$trip_id/movement-events"
}

# =============================================================================
# Geofence Endpoints
# =============================================================================

api_create_geofence() {
    local device_id="$1"
    local name="$2"
    local latitude="$3"
    local longitude="$4"
    local radius="${5:-100}"
    local event_types="${6:-[\"enter\", \"exit\"]}"

    local payload=$(cat <<EOF
{
    "device_id": "$device_id",
    "name": "$name",
    "latitude": $latitude,
    "longitude": $longitude,
    "radius_meters": $radius,
    "event_types": $event_types,
    "active": true
}
EOF
)
    api_request "POST" "/api/v1/geofences" "$payload"
}

api_get_geofences() {
    local device_id="$1"
    api_request "GET" "/api/v1/geofences?device_id=$device_id"
}

api_get_geofence() {
    local geofence_id="$1"
    api_request "GET" "/api/v1/geofences/$geofence_id"
}

api_update_geofence() {
    local geofence_id="$1"
    local updates_json="$2"
    api_request "PATCH" "/api/v1/geofences/$geofence_id" "$updates_json"
}

api_delete_geofence() {
    local geofence_id="$1"
    api_request "DELETE" "/api/v1/geofences/$geofence_id"
}

# =============================================================================
# Proximity Alert Endpoints
# =============================================================================

api_create_proximity_alert() {
    local source_device_id="$1"
    local target_device_id="$2"
    local name="$3"
    local radius="${4:-5000}"

    local payload=$(cat <<EOF
{
    "source_device_id": "$source_device_id",
    "target_device_id": "$target_device_id",
    "name": "$name",
    "radius_meters": $radius,
    "is_active": true
}
EOF
)
    api_request "POST" "/api/v1/proximity-alerts" "$payload"
}

api_get_proximity_alerts() {
    local source_device_id="$1"
    api_request "GET" "/api/v1/proximity-alerts?source_device_id=$source_device_id"
}

api_delete_proximity_alert() {
    local alert_id="$1"
    api_request "DELETE" "/api/v1/proximity-alerts/$alert_id"
}

# =============================================================================
# Privacy/GDPR Endpoints
# =============================================================================

api_export_device_data() {
    local device_id="$1"
    api_request "GET" "/api/v1/devices/$device_id/data-export"
}

api_delete_device_data() {
    local device_id="$1"
    api_request "DELETE" "/api/v1/devices/$device_id/data"
}

# =============================================================================
# Admin Endpoints
# =============================================================================

api_admin_stats() {
    api_request "GET" "/api/v1/admin/stats"
}

api_admin_delete_inactive_devices() {
    api_request "DELETE" "/api/v1/admin/devices/inactive"
}

api_admin_reactivate_device() {
    local device_id="$1"
    api_request "POST" "/api/v1/admin/devices/$device_id/reactivate"
}

# =============================================================================
# Authentication Endpoints
# =============================================================================

# Register a new user with email/password
api_auth_register() {
    local email="$1"
    local password="$2"
    local display_name="${3:-Test User}"

    local payload=$(cat <<EOF
{
    "email": "$email",
    "password": "$password",
    "display_name": "$display_name"
}
EOF
)
    api_request "POST" "/api/v1/auth/register" "$payload"
}

# Login with email/password
api_auth_login() {
    local email="$1"
    local password="$2"

    local payload=$(cat <<EOF
{
    "email": "$email",
    "password": "$password"
}
EOF
)
    api_request "POST" "/api/v1/auth/login" "$payload"
}

# OAuth sign-in (Google, Apple)
api_auth_oauth() {
    local provider="$1"  # google, apple
    local id_token="$2"

    local payload=$(cat <<EOF
{
    "provider": "$provider",
    "id_token": "$id_token"
}
EOF
)
    api_request "POST" "/api/v1/auth/oauth" "$payload"
}

# Refresh access token
api_auth_refresh_token() {
    local refresh_token="$1"

    local payload=$(cat <<EOF
{
    "refresh_token": "$refresh_token"
}
EOF
)
    api_request "POST" "/api/v1/auth/refresh" "$payload"
}

# Logout (single device or all devices)
api_auth_logout() {
    local access_token="$1"
    local all_devices="${2:-false}"

    local payload=$(cat <<EOF
{
    "all_devices": $all_devices
}
EOF
)
    api_request "POST" "/api/v1/auth/logout" "$payload" "Authorization: Bearer $access_token"
}

# Request password reset
api_auth_forgot_password() {
    local email="$1"

    local payload=$(cat <<EOF
{
    "email": "$email"
}
EOF
)
    api_request "POST" "/api/v1/auth/forgot-password" "$payload"
}

# Reset password with token
api_auth_reset_password() {
    local token="$1"
    local new_password="$2"

    local payload=$(cat <<EOF
{
    "token": "$token",
    "new_password": "$new_password"
}
EOF
)
    api_request "POST" "/api/v1/auth/reset-password" "$payload"
}

# Verify email
api_auth_verify_email() {
    local token="$1"
    api_request "GET" "/api/v1/auth/verify-email?token=$token"
}

# Request email verification
api_auth_request_verification() {
    local access_token="$1"
    api_request "POST" "/api/v1/auth/request-verification" "" "Authorization: Bearer $access_token"
}

# Get current user profile
api_auth_get_profile() {
    local access_token="$1"
    api_request "GET" "/api/v1/auth/profile" "" "Authorization: Bearer $access_token"
}

# Update user profile
api_auth_update_profile() {
    local access_token="$1"
    local display_name="$2"

    local payload=$(cat <<EOF
{
    "display_name": "$display_name"
}
EOF
)
    api_request "PATCH" "/api/v1/auth/profile" "$payload" "Authorization: Bearer $access_token"
}

# =============================================================================
# Group Management Endpoints
# =============================================================================

# Create a new group
api_create_group() {
    local name="${1:-}"
    local description="${2:-}"

    local payload=$(cat <<EOF
{
    "name": "$name",
    "description": "$description"
}
EOF
)
    api_request "POST" "/api/v1/groups" "$payload"
}

# Get user's groups
api_get_user_groups() {
    api_request "GET" "/api/v1/groups"
}

# Get group details
api_get_group() {
    local group_id="$1"
    api_request "GET" "/api/v1/groups/$group_id"
}

# Update group
api_update_group() {
    local group_id="$1"
    local updates_json="$2"
    api_request "PATCH" "/api/v1/groups/$group_id" "$updates_json"
}

# Delete group
api_delete_group() {
    local group_id="$1"
    api_request "DELETE" "/api/v1/groups/$group_id"
}

# Get group members
api_get_group_members() {
    local group_id="$1"
    api_request "GET" "/api/v1/groups/$group_id/members"
}

# Update member role
api_update_member_role() {
    local group_id="$1"
    local member_id="$2"
    local role="$3"  # OWNER, ADMIN, MEMBER, VIEWER

    local payload=$(cat <<EOF
{
    "role": "$role"
}
EOF
)
    api_request "PATCH" "/api/v1/groups/$group_id/members/$member_id" "$payload"
}

# Remove member from group
api_remove_group_member() {
    local group_id="$1"
    local member_id="$2"
    api_request "DELETE" "/api/v1/groups/$group_id/members/$member_id"
}

# Leave group
api_leave_group() {
    local group_id="$1"
    api_request "POST" "/api/v1/groups/$group_id/leave"
}

# Transfer ownership
api_transfer_ownership() {
    local group_id="$1"
    local new_owner_id="$2"

    local payload=$(cat <<EOF
{
    "new_owner_id": "$new_owner_id"
}
EOF
)
    api_request "POST" "/api/v1/groups/$group_id/transfer" "$payload"
}

# =============================================================================
# Group Invite Endpoints
# =============================================================================

# Create invite code
api_create_group_invite() {
    local group_id="$1"
    local max_uses="${2:-1}"
    local expires_hours="${3:-24}"

    local payload=$(cat <<EOF
{
    "max_uses": $max_uses,
    "expires_in_hours": $expires_hours
}
EOF
)
    api_request "POST" "/api/v1/groups/$group_id/invites" "$payload"
}

# Get group invites
api_get_group_invites() {
    local group_id="$1"
    api_request "GET" "/api/v1/groups/$group_id/invites"
}

# Revoke invite
api_revoke_invite() {
    local invite_id="$1"
    api_request "DELETE" "/api/v1/invites/$invite_id"
}

# Validate invite code
api_validate_invite() {
    local code="$1"
    api_request "GET" "/api/v1/invites/validate?code=$code"
}

# Join group with invite code
api_join_group() {
    local code="$1"
    local device_id="${2:-}"

    local payload=$(cat <<EOF
{
    "code": "$code",
    "device_id": "$device_id"
}
EOF
)
    api_request "POST" "/api/v1/invites/join" "$payload"
}

# =============================================================================
# Device Settings Endpoints
# =============================================================================

# Get device settings
api_get_device_settings() {
    local device_id="$1"
    api_request "GET" "/api/v1/devices/$device_id/settings"
}

# Update device settings
api_update_device_settings() {
    local device_id="$1"
    local settings_json="$2"
    api_request "PATCH" "/api/v1/devices/$device_id/settings" "$settings_json"
}

# Lock device setting (admin only)
api_lock_device_setting() {
    local device_id="$1"
    local setting_key="$2"
    local reason="${3:-Admin locked}"
    local access_token="$4"

    local payload=$(cat <<EOF
{
    "setting_key": "$setting_key",
    "reason": "$reason"
}
EOF
)
    api_request "POST" "/api/v1/devices/$device_id/settings/lock" "$payload" "Authorization: Bearer $access_token"
}

# Unlock device setting (admin only)
api_unlock_device_setting() {
    local device_id="$1"
    local setting_key="$2"
    local access_token="$3"

    local payload=$(cat <<EOF
{
    "setting_key": "$setting_key"
}
EOF
)
    api_request "POST" "/api/v1/devices/$device_id/settings/unlock" "$payload" "Authorization: Bearer $access_token"
}

# Get unlock requests
api_get_unlock_requests() {
    local device_id="$1"
    local access_token="$2"
    api_request "GET" "/api/v1/devices/$device_id/unlock-requests" "" "Authorization: Bearer $access_token"
}

# Create unlock request
api_create_unlock_request() {
    local device_id="$1"
    local setting_key="$2"
    local reason="$3"
    local access_token="$4"

    local payload=$(cat <<EOF
{
    "setting_key": "$setting_key",
    "reason": "$reason"
}
EOF
)
    api_request "POST" "/api/v1/devices/$device_id/unlock-requests" "$payload" "Authorization: Bearer $access_token"
}

# Approve unlock request (admin only)
api_approve_unlock_request() {
    local request_id="$1"
    local access_token="$2"
    api_request "POST" "/api/v1/unlock-requests/$request_id/approve" "" "Authorization: Bearer $access_token"
}

# Deny unlock request (admin only)
api_deny_unlock_request() {
    local request_id="$1"
    local reason="${2:-}"
    local access_token="$3"

    local payload=$(cat <<EOF
{
    "reason": "$reason"
}
EOF
)
    api_request "POST" "/api/v1/unlock-requests/$request_id/deny" "$payload" "Authorization: Bearer $access_token"
}

# Get settings history
api_get_settings_history() {
    local device_id="$1"
    local access_token="$2"
    local limit="${3:-50}"
    api_request "GET" "/api/v1/devices/$device_id/settings/history?limit=$limit" "" "Authorization: Bearer $access_token"
}

# =============================================================================
# Settings Templates Endpoints
# =============================================================================

# Get settings templates
api_get_settings_templates() {
    local access_token="$1"
    api_request "GET" "/api/v1/settings/templates" "" "Authorization: Bearer $access_token"
}

# Create settings template
api_create_settings_template() {
    local name="$1"
    local settings_json="$2"
    local access_token="$3"

    local payload=$(cat <<EOF
{
    "name": "$name",
    "settings": $settings_json
}
EOF
)
    api_request "POST" "/api/v1/settings/templates" "$payload" "Authorization: Bearer $access_token"
}

# Apply template to device
api_apply_settings_template() {
    local device_id="$1"
    local template_id="$2"
    local access_token="$3"

    local payload=$(cat <<EOF
{
    "template_id": "$template_id"
}
EOF
)
    api_request "POST" "/api/v1/devices/$device_id/settings/apply-template" "$payload" "Authorization: Bearer $access_token"
}

# Bulk apply settings to multiple devices
api_bulk_apply_settings() {
    local device_ids_json="$1"
    local settings_json="$2"
    local access_token="$3"

    local payload=$(cat <<EOF
{
    "device_ids": $device_ids_json,
    "settings": $settings_json
}
EOF
)
    api_request "POST" "/api/v1/devices/bulk-settings" "$payload" "Authorization: Bearer $access_token"
}

# =============================================================================
# Weather Endpoints
# =============================================================================

# Get current weather for location
api_get_weather() {
    local latitude="$1"
    local longitude="$2"
    api_request "GET" "/api/v1/weather?lat=$latitude&lon=$longitude"
}

# Get weather forecast
api_get_weather_forecast() {
    local latitude="$1"
    local longitude="$2"
    local days="${3:-5}"
    api_request "GET" "/api/v1/weather/forecast?lat=$latitude&lon=$longitude&days=$days"
}

# =============================================================================
# Webhook Endpoints
# =============================================================================

# Create webhook
api_create_webhook() {
    local device_id="$1"
    local url="$2"
    local events_json="$3"  # e.g., ["geofence_enter", "geofence_exit"]
    local secret="${4:-}"

    local payload=$(cat <<EOF
{
    "device_id": "$device_id",
    "url": "$url",
    "events": $events_json$(if [[ -n "$secret" ]]; then echo ", \"secret\": \"$secret\""; fi)
}
EOF
)
    api_request "POST" "/api/v1/webhooks" "$payload"
}

# Get webhooks
api_get_webhooks() {
    local device_id="$1"
    api_request "GET" "/api/v1/webhooks?device_id=$device_id"
}

# Update webhook
api_update_webhook() {
    local webhook_id="$1"
    local updates_json="$2"
    api_request "PATCH" "/api/v1/webhooks/$webhook_id" "$updates_json"
}

# Delete webhook
api_delete_webhook() {
    local webhook_id="$1"
    api_request "DELETE" "/api/v1/webhooks/$webhook_id"
}

# Test webhook
api_test_webhook() {
    local webhook_id="$1"
    api_request "POST" "/api/v1/webhooks/$webhook_id/test"
}

# =============================================================================
# Enterprise Enrollment Endpoints
# =============================================================================

# Initiate enrollment
api_initiate_enrollment() {
    local token="$1"
    api_request "POST" "/api/v1/enrollment/initiate" "{\"token\": \"$token\"}"
}

# Complete enrollment
api_complete_enrollment() {
    local session_id="$1"
    local device_info_json="$2"

    local payload=$(cat <<EOF
{
    "session_id": "$session_id",
    "device_info": $device_info_json
}
EOF
)
    api_request "POST" "/api/v1/enrollment/complete" "$payload"
}

# Get enrollment status
api_get_enrollment_status() {
    local session_id="$1"
    api_request "GET" "/api/v1/enrollment/status?session_id=$session_id"
}

echo "API helpers loaded"
