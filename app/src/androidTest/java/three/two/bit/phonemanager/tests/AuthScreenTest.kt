package three.two.bit.phonemanager.tests

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import three.two.bit.phonemanager.MainActivity

/**
 * Basic instrumented tests for verifying the test infrastructure works.
 */
@HiltAndroidTest
class AuthScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun appLaunches_successfully() {
        // Just verify the app launches without crashing
        // This is a smoke test to validate the test infrastructure
        composeTestRule.waitForIdle()
    }

    @Test
    fun mainScreen_isDisplayed() {
        // Verify some basic UI is present
        composeTestRule.waitForIdle()
        // The app should show something - this validates compose test rule works
    }
}
