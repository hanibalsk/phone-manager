package three.two.bit.phonemanager.service

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import three.two.bit.phonemanager.R
import three.two.bit.phonemanager.movement.TransportationMode
import three.two.bit.phonemanager.util.NotificationFormatter
import kotlin.time.Clock

/**
 * Test coverage for notification formatting functions.
 * Tests NotificationFormatter utility object functions.
 */
class NotificationFormattingTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = mockk(relaxed = true)
    }

    // getModeEmoji tests
    @Test
    fun `getModeEmoji returns walking emoji for WALKING mode`() {
        val emoji = NotificationFormatter.getModeEmoji(TransportationMode.WALKING)
        assertEquals("🚶", emoji)
    }

    @Test
    fun `getModeEmoji returns running emoji for RUNNING mode`() {
        val emoji = NotificationFormatter.getModeEmoji(TransportationMode.RUNNING)
        assertEquals("🏃", emoji)
    }

    @Test
    fun `getModeEmoji returns cycling emoji for CYCLING mode`() {
        val emoji = NotificationFormatter.getModeEmoji(TransportationMode.CYCLING)
        assertEquals("🚲", emoji)
    }

    @Test
    fun `getModeEmoji returns vehicle emoji for IN_VEHICLE mode`() {
        val emoji = NotificationFormatter.getModeEmoji(TransportationMode.IN_VEHICLE)
        assertEquals("🚗", emoji)
    }

    @Test
    fun `getModeEmoji returns stationary emoji for STATIONARY mode`() {
        val emoji = NotificationFormatter.getModeEmoji(TransportationMode.STATIONARY)
        assertEquals("📍", emoji)
    }

    @Test
    fun `getModeEmoji returns question emoji for UNKNOWN mode`() {
        val emoji = NotificationFormatter.getModeEmoji(TransportationMode.UNKNOWN)
        assertEquals("❓", emoji)
    }

    // formatTripDuration tests
    @Test
    fun `formatTripDuration returns less than 1 min for duration under 60 seconds`() {
        val now = Clock.System.now().epochSeconds
        val startTime = now - 30 // 30 seconds ago

        every { context.getString(R.string.notification_duration_less_than_minute) } returns "<1 min"

        val result = NotificationFormatter.formatTripDuration(context, startTime)
        assertEquals("<1 min", result)
    }

    @Test
    fun `formatTripDuration returns minutes for duration under 1 hour`() {
        val now = Clock.System.now().epochSeconds
        val startTime = now - 1500 // 25 minutes ago

        every { context.getString(R.string.notification_duration_minutes, 25L) } returns "25 min"

        val result = NotificationFormatter.formatTripDuration(context, startTime)
        assertEquals("25 min", result)
    }

    @Test
    fun `formatTripDuration returns minutes for exactly 60 seconds`() {
        val now = Clock.System.now().epochSeconds
        val startTime = now - 60 // exactly 1 minute ago

        every { context.getString(R.string.notification_duration_minutes, 1L) } returns "1 min"

        val result = NotificationFormatter.formatTripDuration(context, startTime)
        assertEquals("1 min", result)
    }

    @Test
    fun `formatTripDuration returns hours and minutes for duration over 1 hour`() {
        val now = Clock.System.now().epochSeconds
        val startTime = now - 5400 // 1 hour 30 minutes ago

        every { context.getString(R.string.notification_duration_hours_minutes, 1L, 30L) } returns "1h 30m"

        val result = NotificationFormatter.formatTripDuration(context, startTime)
        assertEquals("1h 30m", result)
    }

    @Test
    fun `formatTripDuration returns hours and minutes for multiple hours`() {
        val now = Clock.System.now().epochSeconds
        val startTime = now - 10800 // 3 hours ago

        every { context.getString(R.string.notification_duration_hours_minutes, 3L, 0L) } returns "3h 0m"

        val result = NotificationFormatter.formatTripDuration(context, startTime)
        assertEquals("3h 0m", result)
    }

    @Test
    fun `formatTripDuration handles zero seconds duration`() {
        val now = Clock.System.now().epochSeconds

        every { context.getString(R.string.notification_duration_less_than_minute) } returns "<1 min"

        val result = NotificationFormatter.formatTripDuration(context, now)
        assertEquals("<1 min", result)
    }

    // formatTripDistance tests
    @Test
    fun `formatTripDistance returns meters for distance under 1000 meters`() {
        every { context.getString(R.string.notification_distance_m, 500.0) } returns "500 m"

        val result = NotificationFormatter.formatTripDistance(context, 500.0)
        assertEquals("500 m", result)
    }

    @Test
    fun `formatTripDistance returns meters for zero distance`() {
        every { context.getString(R.string.notification_distance_m, 0.0) } returns "0 m"

        val result = NotificationFormatter.formatTripDistance(context, 0.0)
        assertEquals("0 m", result)
    }

    @Test
    fun `formatTripDistance returns meters for 999 meters`() {
        every { context.getString(R.string.notification_distance_m, 999.0) } returns "999 m"

        val result = NotificationFormatter.formatTripDistance(context, 999.0)
        assertEquals("999 m", result)
    }

    @Test
    fun `formatTripDistance returns kilometers for distance 1000 meters or more`() {
        every { context.getString(R.string.notification_distance_km, 1.0) } returns "1.0 km"

        val result = NotificationFormatter.formatTripDistance(context, 1000.0)
        assertEquals("1.0 km", result)
    }

    @Test
    fun `formatTripDistance returns kilometers with decimal for 8200 meters`() {
        every { context.getString(R.string.notification_distance_km, 8.2) } returns "8.2 km"

        val result = NotificationFormatter.formatTripDistance(context, 8200.0)
        assertEquals("8.2 km", result)
    }

    @Test
    fun `formatTripDistance handles large distances correctly`() {
        every { context.getString(R.string.notification_distance_km, 42.5) } returns "42.5 km"

        val result = NotificationFormatter.formatTripDistance(context, 42500.0)
        assertEquals("42.5 km", result)
    }

    // getLastUpdateText tests
    @Test
    fun `getLastUpdateText returns Just now for update less than 1 minute ago`() {
        val now = Clock.System.now().toEpochMilliseconds()
        val lastUpdate = now - 30000 // 30 seconds ago

        every { context.getString(R.string.notification_last_update_just_now) } returns "Last update: Just now"

        val result = NotificationFormatter.getLastUpdateText(context, lastUpdate)
        assertEquals("Last update: Just now", result)
    }

    @Test
    fun `getLastUpdateText returns Just now for update 0 milliseconds ago`() {
        val now = Clock.System.now().toEpochMilliseconds()

        every { context.getString(R.string.notification_last_update_just_now) } returns "Last update: Just now"

        val result = NotificationFormatter.getLastUpdateText(context, now)
        assertEquals("Last update: Just now", result)
    }

    @Test
    fun `getLastUpdateText returns minutes for update between 1 and 60 minutes ago`() {
        val now = Clock.System.now().toEpochMilliseconds()
        val lastUpdate = now - 900000 // 15 minutes ago

        every { context.getString(R.string.notification_last_update_minutes, 15) } returns "Last update: 15 min ago"

        val result = NotificationFormatter.getLastUpdateText(context, lastUpdate)
        assertEquals("Last update: 15 min ago", result)
    }

    @Test
    fun `getLastUpdateText returns 1 minute for exactly 60 seconds ago`() {
        val now = Clock.System.now().toEpochMilliseconds()
        val lastUpdate = now - 60000 // 1 minute ago

        every { context.getString(R.string.notification_last_update_minutes, 1) } returns "Last update: 1 min ago"

        val result = NotificationFormatter.getLastUpdateText(context, lastUpdate)
        assertEquals("Last update: 1 min ago", result)
    }

    @Test
    fun `getLastUpdateText returns hours for update over 60 minutes ago`() {
        val now = Clock.System.now().toEpochMilliseconds()
        val lastUpdate = now - 7200000 // 2 hours ago

        every { context.getString(R.string.notification_last_update_hours, 2) } returns "Last update: 2h ago"

        val result = NotificationFormatter.getLastUpdateText(context, lastUpdate)
        assertEquals("Last update: 2h ago", result)
    }

    @Test
    fun `getLastUpdateText returns hours for multiple hours ago`() {
        val now = Clock.System.now().toEpochMilliseconds()
        val lastUpdate = now - 14400000 // 4 hours ago

        every { context.getString(R.string.notification_last_update_hours, 4) } returns "Last update: 4h ago"

        val result = NotificationFormatter.getLastUpdateText(context, lastUpdate)
        assertEquals("Last update: 4h ago", result)
    }

    @Test
    fun `getLastUpdateText returns Never for null timestamp`() {
        every { context.getString(R.string.notification_last_update_never) } returns "Last update: Never"

        val result = NotificationFormatter.getLastUpdateText(context, null)
        assertEquals("Last update: Never", result)
    }

    // Edge case tests
    @Test
    fun `formatTripDuration handles exactly 1 hour`() {
        val now = Clock.System.now().epochSeconds
        val startTime = now - 3600 // exactly 1 hour

        every { context.getString(R.string.notification_duration_hours_minutes, 1L, 0L) } returns "1h 0m"

        val result = NotificationFormatter.formatTripDuration(context, startTime)
        assertEquals("1h 0m", result)
    }

    @Test
    fun `formatTripDistance handles exactly 1000 meters`() {
        every { context.getString(R.string.notification_distance_km, 1.0) } returns "1.0 km"

        val result = NotificationFormatter.formatTripDistance(context, 1000.0)
        assertEquals("1.0 km", result)
    }

    @Test
    fun `getLastUpdateText handles exactly 60 minutes`() {
        val now = Clock.System.now().toEpochMilliseconds()
        val lastUpdate = now - 3600000 // exactly 60 minutes

        every { context.getString(R.string.notification_last_update_hours, 1) } returns "Last update: 1h ago"

        val result = NotificationFormatter.getLastUpdateText(context, lastUpdate)
        assertEquals("Last update: 1h ago", result)
    }
}
