package three.two.bit.phonemanager.mocks

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.rules.ExternalResource
import java.util.concurrent.TimeUnit

/**
 * JUnit Rule for managing MockWebServer lifecycle and providing convenient API mocking.
 *
 * Features:
 * - Automatic server start/stop lifecycle management
 * - Pre-configured mock responses for common API endpoints
 * - Custom response queue support
 * - Network failure simulation
 * - Request recording and verification
 *
 * Usage:
 * ```kotlin
 * @get:Rule
 * val mockServerRule = MockWebServerRule()
 *
 * @Test
 * fun testApiCall() {
 *     mockServerRule.enqueue(ApiResponses.loginSuccess())
 *     // Perform action that triggers API call
 *     val request = mockServerRule.takeRequest()
 *     assertThat(request.path).isEqualTo("/api/auth/login")
 * }
 * ```
 */
class MockWebServerRule(private val port: Int = 8080) : ExternalResource() {

    lateinit var server: MockWebServer
        private set

    val baseUrl: String
        get() = server.url("/").toString()

    override fun before() {
        server = MockWebServer()
        server.start(port)
    }

    override fun after() {
        server.shutdown()
    }

    /**
     * Enqueue a mock response to be returned for the next request.
     */
    fun enqueue(response: MockResponse) {
        server.enqueue(response)
    }

    /**
     * Enqueue multiple mock responses in order.
     */
    fun enqueueAll(vararg responses: MockResponse) {
        responses.forEach { server.enqueue(it) }
    }

    /**
     * Get the next recorded request, waiting up to the specified timeout.
     */
    fun takeRequest(timeout: Long = 5, unit: TimeUnit = TimeUnit.SECONDS): RecordedRequest? =
        server.takeRequest(timeout, unit)

    /**
     * Get the total number of requests received.
     */
    val requestCount: Int
        get() = server.requestCount

    /**
     * Set a custom dispatcher for handling requests.
     */
    fun setDispatcher(dispatcher: Dispatcher) {
        server.dispatcher = dispatcher
    }

    /**
     * Create a dispatcher that routes requests to different responses based on path.
     */
    fun setPathBasedDispatcher(routes: Map<String, MockResponse>) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path ?: return MockResponse().setResponseCode(404)
                return routes.entries.find { path.startsWith(it.key) }?.value
                    ?: MockResponse().setResponseCode(404).setBody("""{"error": "Not found"}""")
            }
        }
    }

    /**
     * Simulate network delay for all responses.
     */
    fun simulateNetworkDelay(delayMs: Long) {
        val originalDispatcher = server.dispatcher
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                Thread.sleep(delayMs)
                return originalDispatcher.dispatch(request)
            }
        }
    }

    /**
     * Simulate network failure (connection reset).
     */
    fun simulateNetworkFailure() {
        server.enqueue(MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START))
    }

    /**
     * Simulate slow network (throttled response).
     */
    fun simulateSlowNetwork(bytesPerPeriod: Long = 1024, periodMs: Long = 100) {
        server.enqueue(
            MockResponse()
                .setBody("{}")
                .throttleBody(bytesPerPeriod, periodMs, TimeUnit.MILLISECONDS),
        )
    }
}
