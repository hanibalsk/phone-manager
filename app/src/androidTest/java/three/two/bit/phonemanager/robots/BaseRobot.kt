package three.two.bit.phonemanager.robots

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput

/**
 * Base class for Robot pattern implementation in Compose UI tests.
 *
 * The Robot pattern separates test logic from UI interaction details,
 * making tests more readable and maintainable.
 *
 * Usage:
 * ```kotlin
 * class LoginTest : BaseComposeTest() {
 *     @Test
 *     fun testSuccessfulLogin() {
 *         AuthScreenRobot(composeTestRule)
 *             .enterEmail("test@example.com")
 *             .enterPassword("password123")
 *             .clickSignIn()
 *             .verifyHomeScreenDisplayed()
 *     }
 * }
 * ```
 */
abstract class BaseRobot(
    protected val semanticsProvider: SemanticsNodeInteractionsProvider
) {
    // =============================================================================
    // Common Assertions
    // =============================================================================

    /**
     * Assert that a node with the given text is displayed.
     */
    protected fun assertTextDisplayed(text: String) {
        semanticsProvider.onNodeWithText(text).assertIsDisplayed()
    }

    /**
     * Assert that a node with the given test tag is displayed.
     */
    protected fun assertTagDisplayed(tag: String) {
        semanticsProvider.onNodeWithTag(tag).assertIsDisplayed()
    }

    /**
     * Assert that a node with the given content description is displayed.
     */
    protected fun assertContentDescriptionDisplayed(description: String) {
        semanticsProvider.onNodeWithContentDescription(description).assertIsDisplayed()
    }

    /**
     * Assert that a node with the given text has the expected text content.
     */
    protected fun assertTextEquals(tag: String, expectedText: String) {
        semanticsProvider.onNodeWithTag(tag).assertTextEquals(expectedText)
    }

    /**
     * Assert that a node is enabled.
     */
    protected fun assertEnabled(tag: String) {
        semanticsProvider.onNodeWithTag(tag).assertIsEnabled()
    }

    /**
     * Assert that a node is disabled.
     */
    protected fun assertDisabled(tag: String) {
        semanticsProvider.onNodeWithTag(tag).assertIsNotEnabled()
    }

    // =============================================================================
    // Common Actions
    // =============================================================================

    /**
     * Click on a node with the given text.
     */
    protected fun clickOnText(text: String) {
        semanticsProvider.onNodeWithText(text).performClick()
    }

    /**
     * Click on a node with the given test tag.
     */
    protected fun clickOnTag(tag: String) {
        semanticsProvider.onNodeWithTag(tag).performClick()
    }

    /**
     * Click on a node with the given content description.
     */
    protected fun clickOnContentDescription(description: String) {
        semanticsProvider.onNodeWithContentDescription(description).performClick()
    }

    /**
     * Enter text into a text field identified by test tag.
     */
    protected fun enterTextByTag(tag: String, text: String) {
        semanticsProvider.onNodeWithTag(tag).apply {
            performTextClearance()
            performTextInput(text)
        }
    }

    /**
     * Enter text into a text field identified by text/hint.
     */
    protected fun enterTextByHint(hint: String, text: String) {
        semanticsProvider.onNodeWithText(hint).apply {
            performTextClearance()
            performTextInput(text)
        }
    }

    /**
     * Scroll to a node with the given test tag.
     */
    protected fun scrollToTag(tag: String) {
        semanticsProvider.onNodeWithTag(tag).performScrollTo()
    }

    /**
     * Scroll to a node with the given text.
     */
    protected fun scrollToText(text: String) {
        semanticsProvider.onNodeWithText(text).performScrollTo()
    }

    // =============================================================================
    // Waiting Utilities
    // =============================================================================

    /**
     * Wait for a node with text to exist.
     */
    protected fun waitForText(text: String, timeoutMs: Long = 5000) {
        semanticsProvider.waitUntil(timeoutMs) {
            try {
                semanticsProvider.onNode(hasText(text)).fetchSemanticsNode()
                true
            } catch (e: AssertionError) {
                false
            }
        }
    }

    /**
     * Wait for a node with test tag to exist.
     */
    protected fun waitForTag(tag: String, timeoutMs: Long = 5000) {
        semanticsProvider.waitUntil(timeoutMs) {
            try {
                semanticsProvider.onNode(hasTestTag(tag)).fetchSemanticsNode()
                true
            } catch (e: AssertionError) {
                false
            }
        }
    }

    /**
     * Wait for a node with content description to exist.
     */
    protected fun waitForContentDescription(description: String, timeoutMs: Long = 5000) {
        semanticsProvider.waitUntil(timeoutMs) {
            try {
                semanticsProvider.onNode(hasContentDescription(description)).fetchSemanticsNode()
                true
            } catch (e: AssertionError) {
                false
            }
        }
    }
}
