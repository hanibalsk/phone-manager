package three.two.bit.phonemanager.network.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Instant

class DeviceDtoMapperTest {
    @Test
    fun `toDomain maps DeviceDto with all fields to Device`() {
        // Given
        val deviceDto =
            DeviceDto(
                deviceId = "device-001",
                displayName = "Martin's Phone",
                lastLocation =
                LocationDto(
                    latitude = 48.1486,
                    longitude = 17.1077,
                    timestamp = "2025-11-25T18:30:00Z",
                ),
                lastSeenAt = "2025-11-25T18:30:00Z",
            )

        // When
        val device = deviceDto.toDomain()

        // Then
        assertEquals("device-001", device.deviceId)
        assertEquals("Martin's Phone", device.displayName)
        assertNotNull(device.lastLocation)
        assertEquals(48.1486, device.lastLocation!!.latitude, 0.0001)
        assertEquals(17.1077, device.lastLocation!!.longitude, 0.0001)
        assertEquals(Instant.parse("2025-11-25T18:30:00Z"), device.lastLocation!!.timestamp)
        assertNotNull(device.lastSeenAt)
        assertEquals(Instant.parse("2025-11-25T18:30:00Z"), device.lastSeenAt)
    }

    @Test
    fun `toDomain maps DeviceDto with null lastLocation and lastSeenAt`() {
        // Given
        val deviceDto =
            DeviceDto(
                deviceId = "device-002",
                displayName = "Test Device",
                lastLocation = null,
                lastSeenAt = null,
            )

        // When
        val device = deviceDto.toDomain()

        // Then
        assertEquals("device-002", device.deviceId)
        assertEquals("Test Device", device.displayName)
        assertNull(device.lastLocation)
        assertNull(device.lastSeenAt)
    }

    @Test
    fun `toDomain maps DeviceDto with lastLocation but no lastSeenAt`() {
        // Given
        val deviceDto =
            DeviceDto(
                deviceId = "device-003",
                displayName = "Another Device",
                lastLocation =
                LocationDto(
                    latitude = -33.8688,
                    longitude = 151.2093,
                    timestamp = "2025-11-25T08:00:00Z",
                ),
                lastSeenAt = null,
            )

        // When
        val device = deviceDto.toDomain()

        // Then
        assertEquals("device-003", device.deviceId)
        assertEquals("Another Device", device.displayName)
        assertNotNull(device.lastLocation)
        assertEquals(-33.8688, device.lastLocation!!.latitude, 0.0001)
        assertEquals(151.2093, device.lastLocation!!.longitude, 0.0001)
        assertNull(device.lastSeenAt)
    }

    @Test
    fun `toDomain handles timestamp with milliseconds`() {
        // Given
        val deviceDto =
            DeviceDto(
                deviceId = "device-004",
                displayName = "Device with Milliseconds",
                lastLocation =
                LocationDto(
                    latitude = 40.7128,
                    longitude = -74.0060,
                    timestamp = "2025-11-25T18:30:45.123Z",
                ),
                lastSeenAt = "2025-11-25T18:30:45.123Z",
            )

        // When
        val device = deviceDto.toDomain()

        // Then
        assertNotNull(device.lastLocation)
        assertEquals(Instant.parse("2025-11-25T18:30:45.123Z"), device.lastLocation!!.timestamp)
        assertEquals(Instant.parse("2025-11-25T18:30:45.123Z"), device.lastSeenAt)
    }

    @Test
    fun `toDomain handles negative coordinates`() {
        // Given
        val deviceDto =
            DeviceDto(
                deviceId = "device-005",
                displayName = "Southern Hemisphere Device",
                lastLocation =
                LocationDto(
                    latitude = -33.8688,
                    longitude = -151.2093,
                    timestamp = "2025-11-25T08:00:00Z",
                ),
                lastSeenAt = null,
            )

        // When
        val device = deviceDto.toDomain()

        // Then
        assertNotNull(device.lastLocation)
        assertEquals(-33.8688, device.lastLocation!!.latitude, 0.0001)
        assertEquals(-151.2093, device.lastLocation!!.longitude, 0.0001)
    }
}

class DevicesResponseTest {
    @Test
    fun `DevicesResponse can contain multiple devices`() {
        // Given
        val devicesResponse =
            DevicesResponse(
                devices =
                listOf(
                    DeviceDto(
                        deviceId = "device-001",
                        displayName = "Device 1",
                        lastLocation = null,
                        lastSeenAt = null,
                    ),
                    DeviceDto(
                        deviceId = "device-002",
                        displayName = "Device 2",
                        lastLocation = null,
                        lastSeenAt = null,
                    ),
                    DeviceDto(
                        deviceId = "device-003",
                        displayName = "Device 3",
                        lastLocation = null,
                        lastSeenAt = null,
                    ),
                ),
            )

        // Then
        assertEquals(3, devicesResponse.devices.size)
        assertEquals("device-001", devicesResponse.devices[0].deviceId)
        assertEquals("device-002", devicesResponse.devices[1].deviceId)
        assertEquals("device-003", devicesResponse.devices[2].deviceId)
    }

    @Test
    fun `DevicesResponse can be empty`() {
        // Given
        val devicesResponse = DevicesResponse(devices = emptyList())

        // Then
        assertEquals(0, devicesResponse.devices.size)
    }
}
