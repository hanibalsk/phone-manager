package three.two.bit.phonemanager.base

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import three.two.bit.phonemanager.MainActivity

/**
 * Base class for Compose UI tests with Hilt dependency injection and MockWebServer support.
 *
 * Provides:
 * - Hilt DI setup via HiltAndroidRule
 * - Compose testing via createAndroidComposeRule
 * - MockWebServer for API mocking
 * - Common test utilities and assertions
 *
 * Usage:
 * ```kotlin
 * @HiltAndroidTest
 * class MyFeatureTest : BaseComposeTest() {
 *     @Test
 *     fun testFeature() {
 *         // Use composeTestRule for UI assertions
 *         // Use mockWebServer for API mocking
 *     }
 * }
 * ```
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
abstract class BaseComposeTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    protected lateinit var mockWebServer: MockWebServer

    @Before
    open fun setUp() {
        hiltRule.inject()
        mockWebServer = MockWebServer()
        mockWebServer.start(8080)
    }

    @After
    open fun tearDown() {
        mockWebServer.shutdown()
    }

    /**
     * Wait for the app to be idle before performing assertions.
     */
    protected fun waitForIdle() {
        composeTestRule.waitForIdle()
    }

    /**
     * Wait for a specific condition with timeout.
     *
     * @param timeoutMillis Maximum time to wait
     * @param condition The condition to check
     */
    protected fun waitUntil(timeoutMillis: Long = 5000, condition: () -> Boolean) {
        composeTestRule.waitUntil(timeoutMillis) { condition() }
    }
}
