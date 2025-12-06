package three.two.bit.phonemanager.base

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit Rule for replacing the Main dispatcher with a test dispatcher.
 *
 * This rule ensures that coroutines launched on [Dispatchers.Main] use the test dispatcher,
 * allowing for controlled execution and proper testing of suspend functions.
 *
 * Usage:
 * ```kotlin
 * @get:Rule
 * val dispatcherRule = TestDispatcherRule()
 *
 * @Test
 * fun testCoroutine() = runTest {
 *     // Your test code here
 *     dispatcherRule.dispatcher.scheduler.advanceUntilIdle()
 * }
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
