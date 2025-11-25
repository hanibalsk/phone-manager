package three.two.bit.phonemanager.domain.model

import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceTest {
    @Test
    fun `Device can be created with all fields`() {
        // Given
        val deviceId = "device-001"
        val displayName = "Martin's Phone"
        val location =
            DeviceLocation(
                latitude = 48.1486,
                longitude = 17.1077,
                timestamp = Instant.parse("2025-11-25T18:30:00Z"),
            )
        val lastSeen = Instant.parse("2025-11-25T18:30:00Z")

        // When
        val device =
            Device(
                deviceId = deviceId,
                displayName = displayName,
                lastLocation = location,
                lastSeenAt = lastSeen,
            )

        // Then
        assertEquals(deviceId, device.deviceId)
        assertEquals(displayName, device.displayName)
        assertNotNull(device.lastLocation)
        assertEquals(location, device.lastLocation)
        assertNotNull(device.lastSeenAt)
        assertEquals(lastSeen, device.lastSeenAt)
    }

    @Test
    fun `Device can be created with null lastLocation and lastSeenAt`() {
        // Given
        val deviceId = "device-002"
        val displayName = "Test Device"

        // When
        val device =
            Device(
                deviceId = deviceId,
                displayName = displayName,
                lastLocation = null,
                lastSeenAt = null,
            )

        // Then
        assertEquals(deviceId, device.deviceId)
        assertEquals(displayName, device.displayName)
        assertNull(device.lastLocation)
        assertNull(device.lastSeenAt)
    }

    @Test
    fun `Device defaults lastLocation and lastSeenAt to null`() {
        // Given
        val deviceId = "device-003"
        val displayName = "Another Device"

        // When
        val device =
            Device(
                deviceId = deviceId,
                displayName = displayName,
            )

        // Then
        assertEquals(deviceId, device.deviceId)
        assertEquals(displayName, device.displayName)
        assertNull(device.lastLocation)
        assertNull(device.lastSeenAt)
    }
}

class DeviceLocationTest {
    @Test
    fun `DeviceLocation can be created with valid coordinates and timestamp`() {
        // Given
        val latitude = 48.1486
        val longitude = 17.1077
        val timestamp = Instant.parse("2025-11-25T18:30:00Z")

        // When
        val location =
            DeviceLocation(
                latitude = latitude,
                longitude = longitude,
                timestamp = timestamp,
            )

        // Then
        assertEquals(latitude, location.latitude, 0.0001)
        assertEquals(longitude, location.longitude, 0.0001)
        assertEquals(timestamp, location.timestamp)
    }

    @Test
    fun `DeviceLocation handles negative coordinates`() {
        // Given
        val latitude = -33.8688 // Sydney, Australia
        val longitude = 151.2093
        val timestamp = Instant.parse("2025-11-25T18:30:00Z")

        // When
        val location =
            DeviceLocation(
                latitude = latitude,
                longitude = longitude,
                timestamp = timestamp,
            )

        // Then
        assertEquals(latitude, location.latitude, 0.0001)
        assertEquals(longitude, location.longitude, 0.0001)
    }

    @Test
    fun `DeviceLocation handles edge case coordinates`() {
        // Given - North Pole
        val latitude = 90.0
        val longitude = 0.0
        val timestamp = Instant.parse("2025-11-25T18:30:00Z")

        // When
        val location =
            DeviceLocation(
                latitude = latitude,
                longitude = longitude,
                timestamp = timestamp,
            )

        // Then
        assertEquals(latitude, location.latitude, 0.0001)
        assertEquals(longitude, location.longitude, 0.0001)
    }
}
