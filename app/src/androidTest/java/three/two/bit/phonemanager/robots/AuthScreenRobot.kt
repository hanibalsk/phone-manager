package three.two.bit.phonemanager.robots

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider

/**
 * Robot for interacting with authentication screens (Login, Register, Forgot Password).
 *
 * Usage:
 * ```kotlin
 * AuthScreenRobot(composeTestRule)
 *     .verifyLoginScreenDisplayed()
 *     .enterEmail("test@example.com")
 *     .enterPassword("password123")
 *     .clickSignIn()
 * ```
 */
class AuthScreenRobot(
    semanticsProvider: SemanticsNodeInteractionsProvider
) : BaseRobot(semanticsProvider) {

    // Test tags - should match those defined in the Compose UI
    companion object {
        const val TAG_EMAIL_INPUT = "email_input"
        const val TAG_PASSWORD_INPUT = "password_input"
        const val TAG_CONFIRM_PASSWORD_INPUT = "confirm_password_input"
        const val TAG_NAME_INPUT = "name_input"
        const val TAG_SIGN_IN_BUTTON = "sign_in_button"
        const val TAG_REGISTER_BUTTON = "register_button"
        const val TAG_FORGOT_PASSWORD_LINK = "forgot_password_link"
        const val TAG_CREATE_ACCOUNT_LINK = "create_account_link"
        const val TAG_BACK_TO_LOGIN_LINK = "back_to_login_link"
        const val TAG_RESET_PASSWORD_BUTTON = "reset_password_button"
        const val TAG_ERROR_MESSAGE = "error_message"
        const val TAG_SUCCESS_MESSAGE = "success_message"
        const val TAG_LOADING_INDICATOR = "loading_indicator"
    }

    // =============================================================================
    // Login Screen Actions
    // =============================================================================

    /**
     * Enter email address in the email field.
     */
    fun enterEmail(email: String): AuthScreenRobot {
        enterTextByTag(TAG_EMAIL_INPUT, email)
        return this
    }

    /**
     * Enter password in the password field.
     */
    fun enterPassword(password: String): AuthScreenRobot {
        enterTextByTag(TAG_PASSWORD_INPUT, password)
        return this
    }

    /**
     * Click the Sign In button.
     */
    fun clickSignIn(): AuthScreenRobot {
        clickOnTag(TAG_SIGN_IN_BUTTON)
        return this
    }

    /**
     * Click the Forgot Password link.
     */
    fun clickForgotPassword(): AuthScreenRobot {
        clickOnTag(TAG_FORGOT_PASSWORD_LINK)
        return this
    }

    /**
     * Click the Create Account link to navigate to registration.
     */
    fun clickCreateAccount(): AuthScreenRobot {
        clickOnTag(TAG_CREATE_ACCOUNT_LINK)
        return this
    }

    /**
     * Perform a complete login flow.
     */
    fun performLogin(email: String, password: String): AuthScreenRobot {
        return enterEmail(email)
            .enterPassword(password)
            .clickSignIn()
    }

    // =============================================================================
    // Registration Screen Actions
    // =============================================================================

    /**
     * Enter name in the name field (registration).
     */
    fun enterName(name: String): AuthScreenRobot {
        enterTextByTag(TAG_NAME_INPUT, name)
        return this
    }

    /**
     * Enter confirm password (registration).
     */
    fun enterConfirmPassword(password: String): AuthScreenRobot {
        enterTextByTag(TAG_CONFIRM_PASSWORD_INPUT, password)
        return this
    }

    /**
     * Click the Register button.
     */
    fun clickRegister(): AuthScreenRobot {
        clickOnTag(TAG_REGISTER_BUTTON)
        return this
    }

    /**
     * Click Back to Login link.
     */
    fun clickBackToLogin(): AuthScreenRobot {
        clickOnTag(TAG_BACK_TO_LOGIN_LINK)
        return this
    }

    /**
     * Perform a complete registration flow.
     */
    fun performRegistration(
        name: String,
        email: String,
        password: String
    ): AuthScreenRobot {
        return enterName(name)
            .enterEmail(email)
            .enterPassword(password)
            .enterConfirmPassword(password)
            .clickRegister()
    }

    // =============================================================================
    // Forgot Password Screen Actions
    // =============================================================================

    /**
     * Click the Reset Password button.
     */
    fun clickResetPassword(): AuthScreenRobot {
        clickOnTag(TAG_RESET_PASSWORD_BUTTON)
        return this
    }

    /**
     * Perform forgot password flow.
     */
    fun performForgotPassword(email: String): AuthScreenRobot {
        return enterEmail(email)
            .clickResetPassword()
    }

    // =============================================================================
    // Assertions
    // =============================================================================

    /**
     * Verify the login screen is displayed.
     */
    fun verifyLoginScreenDisplayed(): AuthScreenRobot {
        assertTagDisplayed(TAG_EMAIL_INPUT)
        assertTagDisplayed(TAG_PASSWORD_INPUT)
        assertTagDisplayed(TAG_SIGN_IN_BUTTON)
        return this
    }

    /**
     * Verify the registration screen is displayed.
     */
    fun verifyRegistrationScreenDisplayed(): AuthScreenRobot {
        assertTagDisplayed(TAG_NAME_INPUT)
        assertTagDisplayed(TAG_EMAIL_INPUT)
        assertTagDisplayed(TAG_PASSWORD_INPUT)
        assertTagDisplayed(TAG_CONFIRM_PASSWORD_INPUT)
        assertTagDisplayed(TAG_REGISTER_BUTTON)
        return this
    }

    /**
     * Verify the forgot password screen is displayed.
     */
    fun verifyForgotPasswordScreenDisplayed(): AuthScreenRobot {
        assertTagDisplayed(TAG_EMAIL_INPUT)
        assertTagDisplayed(TAG_RESET_PASSWORD_BUTTON)
        return this
    }

    /**
     * Verify an error message is displayed.
     */
    fun verifyErrorDisplayed(errorMessage: String? = null): AuthScreenRobot {
        assertTagDisplayed(TAG_ERROR_MESSAGE)
        if (errorMessage != null) {
            assertTextDisplayed(errorMessage)
        }
        return this
    }

    /**
     * Verify a success message is displayed.
     */
    fun verifySuccessDisplayed(successMessage: String? = null): AuthScreenRobot {
        assertTagDisplayed(TAG_SUCCESS_MESSAGE)
        if (successMessage != null) {
            assertTextDisplayed(successMessage)
        }
        return this
    }

    /**
     * Verify loading indicator is displayed.
     */
    fun verifyLoadingDisplayed(): AuthScreenRobot {
        assertTagDisplayed(TAG_LOADING_INDICATOR)
        return this
    }

    /**
     * Verify the Sign In button is enabled.
     */
    fun verifySignInEnabled(): AuthScreenRobot {
        assertEnabled(TAG_SIGN_IN_BUTTON)
        return this
    }

    /**
     * Verify the Sign In button is disabled.
     */
    fun verifySignInDisabled(): AuthScreenRobot {
        assertDisabled(TAG_SIGN_IN_BUTTON)
        return this
    }

    /**
     * Wait for login to complete and navigate to home.
     */
    fun waitForLoginSuccess(timeoutMs: Long = 10000): AuthScreenRobot {
        waitForTag(HomeScreenRobot.TAG_HOME_SCREEN, timeoutMs)
        return this
    }
}
