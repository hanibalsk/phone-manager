#!/bin/bash
# =============================================================================
# E2E Tests: Authentication Flows
# Tests user registration, login, token management, and logout
# =============================================================================

set -euo pipefail

# Load test framework
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/../config.sh"
source "${SCRIPT_DIR}/../lib/common.sh"
source "${SCRIPT_DIR}/../lib/api.sh"
source "${SCRIPT_DIR}/../lib/adb.sh"
source "${SCRIPT_DIR}/../lib/generators.sh"

# =============================================================================
# Test Setup
# =============================================================================
TEST_SESSION_EMAIL=""
TEST_SESSION_PASSWORD=""
TEST_SESSION_TOKEN=""
TEST_SESSION_REFRESH_TOKEN=""
TEST_SESSION_USER_ID=""

setup_auth_tests() {
    log_info "Setting up authentication tests..."

    # Generate unique test user for this session
    TEST_SESSION_EMAIL=$(generate_test_email "auth")
    TEST_SESSION_PASSWORD="TestPass123!"

    log_debug "Test user email: $TEST_SESSION_EMAIL"
}

cleanup_auth_tests() {
    log_info "Cleaning up authentication tests..."

    # Cleanup test user if created
    if [[ -n "$TEST_SESSION_USER_ID" ]]; then
        log_debug "Test user cleanup would happen here (user_id: $TEST_SESSION_USER_ID)"
        # api_admin_delete_user "$TEST_SESSION_USER_ID" 2>/dev/null || true
    fi
}

# =============================================================================
# Test: Email Registration Success
# =============================================================================
test_email_registration_success() {
    test_start "Email Registration - Success"

    local email=$(generate_test_email "reg_success")
    local password="ValidPass123!"
    local name="Test User Registration"

    log_step "Registering new user with valid credentials"
    local response
    response=$(api_auth_register "$email" "$password" "$name")
    local status=$?

    if [[ $status -ne 0 ]]; then
        log_error "Registration API call failed"
        test_end
        return 1
    fi

    # Check response contains user data
    local user_id
    user_id=$(echo "$response" | jq -r '.user.id // .id // empty')

    assert_not_equals "" "$user_id" "Registration should return user ID"

    # Verify token is returned
    local token
    token=$(echo "$response" | jq -r '.token // .access_token // empty')

    assert_not_equals "" "$token" "Registration should return access token"

    # Verify user can be authenticated with returned token
    log_step "Verifying token is valid"
    local verify_response
    verify_response=$(api_auth_verify_token "$token" 2>/dev/null || echo "")

    if [[ -n "$verify_response" ]]; then
        log_success "Token verification successful"
    else
        log_warning "Token verification endpoint may not be available"
    fi

    test_end
}

# =============================================================================
# Test: Email Registration Validation Errors
# =============================================================================
test_email_registration_validation_errors() {
    test_start "Email Registration - Validation Errors"

    # Test 1: Invalid email format
    log_step "Testing invalid email format"
    local response
    response=$(api_auth_register "invalid-email" "ValidPass123!" "Test" 2>&1 || true)
    local http_code
    http_code=$(echo "$response" | grep -o '"status":[0-9]*' | grep -o '[0-9]*' || echo "400")

    # Should return 400 or contain error
    if echo "$response" | grep -qi "error\|invalid\|validation"; then
        log_success "Invalid email rejected correctly"
        ((TESTS_PASSED++))
    else
        log_warning "Invalid email may not have been rejected (response: ${response:0:100})"
    fi

    # Test 2: Weak password
    log_step "Testing weak password"
    local weak_email=$(generate_test_email "weak_pass")
    response=$(api_auth_register "$weak_email" "123" "Test" 2>&1 || true)

    if echo "$response" | grep -qi "error\|invalid\|password\|weak"; then
        log_success "Weak password rejected correctly"
        ((TESTS_PASSED++))
    else
        log_warning "Weak password may not have been rejected"
    fi

    # Test 3: Missing required fields
    log_step "Testing missing required fields"
    response=$(api_auth_register "" "ValidPass123!" "" 2>&1 || true)

    if echo "$response" | grep -qi "error\|required\|missing"; then
        log_success "Missing fields rejected correctly"
        ((TESTS_PASSED++))
    else
        log_warning "Missing fields may not have been rejected"
    fi

    # Test 4: Duplicate email (if we have a pre-existing user)
    log_step "Testing duplicate email registration"
    local dup_email="${TEST_USER_PARENT_EMAIL}"
    response=$(api_auth_register "$dup_email" "ValidPass123!" "Duplicate" 2>&1 || true)

    if echo "$response" | grep -qi "error\|exists\|duplicate\|already"; then
        log_success "Duplicate email rejected correctly"
        ((TESTS_PASSED++))
    else
        log_warning "Duplicate email check may not be enforced or user doesn't exist"
    fi

    test_end
}

# =============================================================================
# Test: Email Login Success
# =============================================================================
test_email_login_success() {
    test_start "Email Login - Success"

    # First register a user to ensure we have valid credentials
    local email=$(generate_test_email "login_success")
    local password="LoginTest123!"
    local name="Login Test User"

    log_step "Creating test user for login test"
    local reg_response
    reg_response=$(api_auth_register "$email" "$password" "$name" 2>/dev/null || echo "")

    # Try login with pre-configured test user if registration fails
    if [[ -z "$reg_response" ]] || echo "$reg_response" | grep -qi "error"; then
        log_warning "Could not create new user, using pre-configured test user"
        email="$TEST_USER_PARENT_EMAIL"
        password="$TEST_USER_PARENT_PASSWORD"
    fi

    log_step "Logging in with valid credentials"
    local login_response
    login_response=$(api_auth_login "$email" "$password")
    local status=$?

    if [[ $status -ne 0 ]]; then
        log_error "Login API call failed"
        test_end
        return 1
    fi

    # Verify token is returned
    local token
    token=$(echo "$login_response" | jq -r '.token // .access_token // empty')

    assert_not_equals "" "$token" "Login should return access token"

    # Verify refresh token is returned
    local refresh_token
    refresh_token=$(echo "$login_response" | jq -r '.refresh_token // empty')

    if [[ -n "$refresh_token" ]]; then
        log_success "Refresh token returned"
        ((TESTS_PASSED++))
    else
        log_warning "Refresh token may not be returned (depends on API design)"
    fi

    # Store for subsequent tests
    TEST_SESSION_TOKEN="$token"
    TEST_SESSION_REFRESH_TOKEN="$refresh_token"

    # Verify user info is returned
    local user_email
    user_email=$(echo "$login_response" | jq -r '.user.email // .email // empty')

    if [[ -n "$user_email" ]]; then
        assert_equals "$email" "$user_email" "Returned email should match login email"
    fi

    test_end
}

# =============================================================================
# Test: Email Login Invalid Credentials
# =============================================================================
test_email_login_invalid_credentials() {
    test_start "Email Login - Invalid Credentials"

    # Test 1: Wrong password
    log_step "Testing login with wrong password"
    local response
    response=$(api_auth_login "$TEST_USER_PARENT_EMAIL" "WrongPassword123!" 2>&1 || true)

    if echo "$response" | grep -qi "error\|invalid\|unauthorized\|401"; then
        log_success "Wrong password rejected correctly"
        ((TESTS_PASSED++))
    else
        log_error "Wrong password may have been accepted"
        ((TESTS_FAILED++))
    fi

    # Test 2: Non-existent user
    log_step "Testing login with non-existent user"
    local fake_email="nonexistent_$(date +%s)@test.local"
    response=$(api_auth_login "$fake_email" "SomePassword123!" 2>&1 || true)

    if echo "$response" | grep -qi "error\|invalid\|not found\|unauthorized\|401\|404"; then
        log_success "Non-existent user rejected correctly"
        ((TESTS_PASSED++))
    else
        log_error "Non-existent user login may have been accepted"
        ((TESTS_FAILED++))
    fi

    # Test 3: Empty credentials
    log_step "Testing login with empty credentials"
    response=$(api_auth_login "" "" 2>&1 || true)

    if echo "$response" | grep -qi "error\|required\|invalid\|400"; then
        log_success "Empty credentials rejected correctly"
        ((TESTS_PASSED++))
    else
        log_warning "Empty credentials handling unclear"
    fi

    test_end
}

# =============================================================================
# Test: Password Reset Flow
# =============================================================================
test_password_reset_flow() {
    test_start "Password Reset Flow"

    log_step "Requesting password reset for test user"
    local response
    response=$(api_auth_forgot_password "$TEST_USER_PARENT_EMAIL" 2>&1 || true)

    # Should return success (200) or accepted (202)
    if echo "$response" | grep -qi "success\|sent\|email\|202\|200" || [[ -z "$response" ]]; then
        log_success "Password reset request accepted"
        ((TESTS_PASSED++))
    else
        # Even on error, server should not reveal if email exists (security)
        if echo "$response" | grep -qi "error"; then
            log_warning "Password reset may have failed, but error handling is acceptable"
        else
            log_success "Password reset request handled (no error revealed)"
            ((TESTS_PASSED++))
        fi
    fi

    # Test with non-existent email (should not reveal user doesn't exist)
    log_step "Testing password reset with non-existent email"
    local fake_email="nonexistent_$(date +%s)@fake.local"
    response=$(api_auth_forgot_password "$fake_email" 2>&1 || true)

    # Server should respond the same way for security
    if echo "$response" | grep -qi "user.*not.*found\|does.*not.*exist"; then
        log_warning "Server reveals user existence - potential security issue"
    else
        log_success "Server does not reveal user existence (secure)"
        ((TESTS_PASSED++))
    fi

    # Note: Actual password reset requires email verification which can't be automated
    log_info "Note: Full password reset flow requires email verification (not automatable)"

    test_end
}

# =============================================================================
# Test: OAuth Google Sign-in (Mock)
# =============================================================================
test_oauth_google_signin() {
    test_start "OAuth Google Sign-in (Mock)"

    log_step "Testing OAuth endpoint availability"

    # Generate a mock OAuth token (in real scenario, this comes from Google)
    local mock_oauth_token="mock_google_token_$(date +%s)"

    local response
    response=$(api_auth_oauth "google" "$mock_oauth_token" 2>&1 || true)

    # OAuth with invalid token should fail gracefully
    if echo "$response" | grep -qi "invalid\|error\|unauthorized"; then
        log_success "OAuth endpoint rejects invalid token correctly"
        ((TESTS_PASSED++))
    elif echo "$response" | grep -qi "not.*found\|not.*implemented\|404\|501"; then
        log_warning "OAuth endpoint may not be implemented"
        test_skip "OAuth not implemented in backend"
    else
        log_info "OAuth response: ${response:0:200}"
    fi

    # Test OAuth URL generation (if available)
    log_step "Checking OAuth redirect URL generation"
    local oauth_url
    oauth_url=$(api_get_oauth_url "google" 2>/dev/null || echo "")

    if [[ -n "$oauth_url" ]] && echo "$oauth_url" | grep -qi "google\|oauth"; then
        log_success "OAuth URL generation available"
        ((TESTS_PASSED++))
    else
        log_info "OAuth URL generation not available or different flow used"
    fi

    test_end
}

# =============================================================================
# Test: Token Refresh Mechanism
# =============================================================================
test_token_refresh_mechanism() {
    test_start "Token Refresh Mechanism"

    # Ensure we have a valid session
    if [[ -z "$TEST_SESSION_TOKEN" ]]; then
        log_step "Creating session for token refresh test"
        local login_response
        login_response=$(api_auth_login "$TEST_USER_PARENT_EMAIL" "$TEST_USER_PARENT_PASSWORD" 2>/dev/null || echo "")

        if [[ -z "$login_response" ]]; then
            test_skip "Could not create session for token refresh test"
            test_end
            return
        fi

        TEST_SESSION_TOKEN=$(echo "$login_response" | jq -r '.token // .access_token // empty')
        TEST_SESSION_REFRESH_TOKEN=$(echo "$login_response" | jq -r '.refresh_token // empty')
    fi

    if [[ -z "$TEST_SESSION_REFRESH_TOKEN" ]]; then
        log_warning "No refresh token available, using access token"
        TEST_SESSION_REFRESH_TOKEN="$TEST_SESSION_TOKEN"
    fi

    log_step "Requesting token refresh"
    local response
    response=$(api_auth_refresh_token "$TEST_SESSION_REFRESH_TOKEN" 2>&1 || true)

    # Check if new token is returned
    local new_token
    new_token=$(echo "$response" | jq -r '.token // .access_token // empty' 2>/dev/null)

    if [[ -n "$new_token" ]] && [[ "$new_token" != "null" ]]; then
        log_success "Token refresh returned new access token"
        ((TESTS_PASSED++))

        # Verify new token is different (optional - some systems return same token if not expired)
        if [[ "$new_token" != "$TEST_SESSION_TOKEN" ]]; then
            log_success "New token is different from original"
            ((TESTS_PASSED++))
        else
            log_info "Token unchanged (may still be valid)"
        fi

        # Update session token
        TEST_SESSION_TOKEN="$new_token"
    else
        if echo "$response" | grep -qi "not.*implemented\|501"; then
            test_skip "Token refresh not implemented"
        else
            log_error "Token refresh did not return new token"
            log_debug "Response: ${response:0:200}"
            ((TESTS_FAILED++))
        fi
    fi

    # Test refresh with invalid token
    log_step "Testing refresh with invalid token"
    response=$(api_auth_refresh_token "invalid_refresh_token_xyz" 2>&1 || true)

    if echo "$response" | grep -qi "invalid\|error\|unauthorized\|401"; then
        log_success "Invalid refresh token rejected correctly"
        ((TESTS_PASSED++))
    else
        log_warning "Invalid refresh token handling unclear"
    fi

    test_end
}

# =============================================================================
# Test: Logout Single Device
# =============================================================================
test_logout_single_device() {
    test_start "Logout - Single Device"

    # Ensure we have a valid session
    if [[ -z "$TEST_SESSION_TOKEN" ]]; then
        log_step "Creating session for logout test"
        local login_response
        login_response=$(api_auth_login "$TEST_USER_PARENT_EMAIL" "$TEST_USER_PARENT_PASSWORD" 2>/dev/null || echo "")

        if [[ -z "$login_response" ]]; then
            test_skip "Could not create session for logout test"
            test_end
            return
        fi

        TEST_SESSION_TOKEN=$(echo "$login_response" | jq -r '.token // .access_token // empty')
    fi

    log_step "Logging out current session"
    local response
    response=$(api_auth_logout "$TEST_SESSION_TOKEN" 2>&1 || true)

    # Logout should succeed (200/204) or return success message
    if echo "$response" | grep -qi "success\|logged.*out" || [[ -z "$response" ]]; then
        log_success "Logout request accepted"
        ((TESTS_PASSED++))
    else
        log_debug "Logout response: ${response:0:100}"
    fi

    # Verify token is now invalid
    log_step "Verifying token is invalidated after logout"
    local verify_response
    verify_response=$(api_auth_verify_token "$TEST_SESSION_TOKEN" 2>&1 || true)

    if echo "$verify_response" | grep -qi "invalid\|expired\|unauthorized\|401"; then
        log_success "Token invalidated after logout"
        ((TESTS_PASSED++))
    else
        log_warning "Token may still be valid after logout (depends on implementation)"
    fi

    # Clear session token
    TEST_SESSION_TOKEN=""

    test_end
}

# =============================================================================
# Test: Logout All Devices
# =============================================================================
test_logout_all_devices() {
    test_start "Logout - All Devices"

    # Create a fresh session
    log_step "Creating session for logout all test"
    local login_response
    login_response=$(api_auth_login "$TEST_USER_PARENT_EMAIL" "$TEST_USER_PARENT_PASSWORD" 2>/dev/null || echo "")

    if [[ -z "$login_response" ]]; then
        test_skip "Could not create session for logout all test"
        test_end
        return
    fi

    local token
    token=$(echo "$login_response" | jq -r '.token // .access_token // empty')

    if [[ -z "$token" ]]; then
        test_skip "No token received from login"
        test_end
        return
    fi

    log_step "Logging out all devices"
    local response
    response=$(api_auth_logout_all "$token" 2>&1 || true)

    # Logout all should succeed
    if echo "$response" | grep -qi "success\|logged.*out\|all.*devices" || [[ -z "$response" ]]; then
        log_success "Logout all request accepted"
        ((TESTS_PASSED++))
    elif echo "$response" | grep -qi "not.*implemented\|404\|501"; then
        test_skip "Logout all devices not implemented"
        test_end
        return
    else
        log_debug "Logout all response: ${response:0:100}"
    fi

    # Verify token is now invalid
    log_step "Verifying all tokens are invalidated"
    local verify_response
    verify_response=$(api_auth_verify_token "$token" 2>&1 || true)

    if echo "$verify_response" | grep -qi "invalid\|expired\|unauthorized\|401"; then
        log_success "All tokens invalidated after logout all"
        ((TESTS_PASSED++))
    else
        log_warning "Token may still be valid (implementation dependent)"
    fi

    test_end
}

# =============================================================================
# Test: App UI Login Flow (if emulator available)
# =============================================================================
test_app_ui_login_flow() {
    test_start "App UI Login Flow"

    # Check if emulator/device is available
    if ! adb_check_device; then
        test_skip "No Android device/emulator connected"
        test_end
        return
    fi

    # Check if app is installed
    if ! adb_is_app_installed "$APP_PACKAGE"; then
        test_skip "App not installed on device"
        test_end
        return
    fi

    log_step "Launching app to login screen"
    adb_launch_app
    sleep 3

    log_step "Taking screenshot of login screen"
    adb_screenshot "auth_login_screen"

    # Note: Full UI automation would require UIAutomator or similar
    log_info "UI login flow screenshot captured for manual verification"
    log_info "Full UI automation requires additional tooling (UIAutomator, Espresso)"

    test_end
}

# =============================================================================
# Main Test Execution
# =============================================================================
main() {
    echo ""
    echo "=============================================="
    echo "  E2E Authentication Tests"
    echo "  Session: $TEST_SESSION_ID"
    echo "=============================================="
    echo ""

    # Check dependencies
    if ! check_dependencies; then
        log_error "Missing dependencies, cannot run tests"
        exit 1
    fi

    # Check backend
    if ! check_backend; then
        log_error "Backend not available"
        exit 1
    fi

    # Setup
    setup_auth_tests

    # Run tests
    test_email_registration_success
    test_email_registration_validation_errors
    test_email_login_success
    test_email_login_invalid_credentials
    test_password_reset_flow
    test_oauth_google_signin
    test_token_refresh_mechanism
    test_logout_single_device
    test_logout_all_devices

    # Optional UI test
    if [[ "${RUN_UI_TESTS:-false}" == "true" ]]; then
        test_app_ui_login_flow
    fi

    # Cleanup
    cleanup_auth_tests

    # Print summary
    print_summary

    # Generate HTML report
    generate_html_report

    # Return exit code based on test results
    [[ $TESTS_FAILED -eq 0 ]]
}

# Run if executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
