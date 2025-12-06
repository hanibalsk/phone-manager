package three.two.bit.phonemanager.mocks

import okhttp3.mockwebserver.MockResponse

/**
 * Predefined mock API responses for E2E testing.
 *
 * Provides realistic response payloads for all API endpoints used by the app.
 * Responses match the actual API contract and include appropriate headers.
 */
object ApiResponses {

    private const val CONTENT_TYPE_JSON = "application/json"

    // =============================================================================
    // Authentication Responses
    // =============================================================================

    fun loginSuccess(
        userId: String = "test-user-123",
        email: String = "test@example.com",
        token: String = "test-jwt-token-abc123"
    ): MockResponse = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", CONTENT_TYPE_JSON)
        .setBody("""
            {
                "user": {
                    "id": "$userId",
                    "email": "$email",
                    "name": "Test User",
                    "role": "user"
                },
                "token": "$token",
                "expiresAt": "2025-12-31T23:59:59Z"
            }
        """.trimIndent())

    fun loginFailure(message: String = "Invalid credentials"): MockResponse = MockResponse()
        .setResponseCode(401)
        .addHeader("Content-Type", CONTENT_TYPE_JSON)
        .setBody("""{"error": "$message"}""")

    fun registerSuccess(
        userId: String = "new-user-456",
        email: String = "newuser@example.com"
    ): MockResponse = MockResponse()
        .setResponseCode(201)
        .addHeader("Content-Type", CONTENT_TYPE_JSON)
        .setBody("""
            {
                "user": {
                    "id": "$userId",
                    "email": "$email",
                    "name": "New User"
                },
                "message": "Registration successful"
            }
        """.trimIndent())

    fun registerFailure(message: String = "Email already exists"): MockResponse = MockResponse()
        .setResponseCode(409)
        .addHeader("Content-Type", CONTENT_TYPE_JSON)
        .setBody("""{"error": "$message"}""")

    // =============================================================================
    // Device Registration Responses
    // =============================================================================

    fun deviceRegisterSuccess(
        deviceId: String = "device-abc123",
        deviceName: String = "Test Device"
    ): MockResponse = MockResponse()
        .setResponseCode(201)
        .addHeader("Content-Type", CONTENT_TYPE_JSON)
        .setBody("""
            {
                "device": {
                    "id": "$deviceId",
                    "name": "$deviceName",
                    "platform": "android",
                    "isPrimary": true,
                    "createdAt": "2025-01-01T00:00:00Z"
                }
            }
        """.trimIndent())

    fun deviceListSuccess(devices: List<DeviceData> = listOf(
        DeviceData("device-1", "Phone", true),
        DeviceData("device-2", "Tablet", false)
    )): MockResponse = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", CONTENT_TYPE_JSON)
        .setBody("""
            {
                "devices": [
                    ${devices.joinToString(",") { device ->
                        """{"id": "${device.id}", "name": "${device.name}", "isPrimary": ${device.isPrimary}}"""
                    }}
                ]
            }
        """.trimIndent())

    // =============================================================================
    // Group Management Responses
    // =============================================================================

    fun groupListSuccess(groups: List<GroupData> = listOf(
        GroupData("group-1", "Family", 3),
        GroupData("group-2", "Work", 5)
    )): MockResponse = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", CONTENT_TYPE_JSON)
        .setBody("""
            {
                "groups": [
                    ${groups.joinToString(",") { group ->
                        """{"id": "${group.id}", "name": "${group.name}", "memberCount": ${group.memberCount}}"""
                    }}
                ]
            }
        """.trimIndent())

    fun groupCreateSuccess(
        groupId: String = "new-group-789",
        groupName: String = "New Group",
        inviteCode: String = "ABC123"
    ): MockResponse = MockResponse()
        .setResponseCode(201)
        .addHeader("Content-Type", CONTENT_TYPE_JSON)
        .setBody("""
            {
                "group": {
                    "id": "$groupId",
                    "name": "$groupName",
                    "inviteCode": "$inviteCode",
                    "memberCount": 1,
                    "createdAt": "2025-01-01T00:00:00Z"
                }
            }
        """.trimIndent())

    fun groupJoinSuccess(
        groupId: String = "joined-group-123",
        groupName: String = "Joined Group"
    ): MockResponse = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", CONTENT_TYPE_JSON)
        .setBody("""
            {
                "group": {
                    "id": "$groupId",
                    "name": "$groupName",
                    "memberCount": 2
                },
                "message": "Successfully joined group"
            }
        """.trimIndent())

    fun groupMembersSuccess(members: List<MemberData> = listOf(
        MemberData("user-1", "Alice", 37.7749, -122.4194),
        MemberData("user-2", "Bob", 37.8044, -122.2712)
    )): MockResponse = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", CONTENT_TYPE_JSON)
        .setBody("""
            {
                "members": [
                    ${members.joinToString(",") { member ->
                        """{
                            "id": "${member.id}",
                            "name": "${member.name}",
                            "location": {
                                "latitude": ${member.lat},
                                "longitude": ${member.lon},
                                "timestamp": "2025-01-01T12:00:00Z"
                            }
                        }"""
                    }}
                ]
            }
        """.trimIndent())

    // =============================================================================
    // Location Tracking Responses
    // =============================================================================

    fun locationUpdateSuccess(): MockResponse = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", CONTENT_TYPE_JSON)
        .setBody("""{"status": "ok", "message": "Location updated"}""")

    fun locationBatchSuccess(count: Int = 10): MockResponse = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", CONTENT_TYPE_JSON)
        .setBody("""{"status": "ok", "processed": $count}""")

    fun locationHistorySuccess(
        deviceId: String = "device-123",
        locations: List<LocationData> = listOf(
            LocationData(37.7749, -122.4194, "2025-01-01T10:00:00Z"),
            LocationData(37.7750, -122.4195, "2025-01-01T10:05:00Z"),
            LocationData(37.7755, -122.4190, "2025-01-01T10:10:00Z")
        )
    ): MockResponse = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", CONTENT_TYPE_JSON)
        .setBody("""
            {
                "deviceId": "$deviceId",
                "locations": [
                    ${locations.joinToString(",") { loc ->
                        """{"latitude": ${loc.lat}, "longitude": ${loc.lon}, "timestamp": "${loc.timestamp}"}"""
                    }}
                ]
            }
        """.trimIndent())

    // =============================================================================
    // Geofence Responses
    // =============================================================================

    fun geofenceListSuccess(geofences: List<GeofenceData> = listOf(
        GeofenceData("geo-1", "Home", 37.7749, -122.4194, 100),
        GeofenceData("geo-2", "Work", 37.8044, -122.2712, 200)
    )): MockResponse = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", CONTENT_TYPE_JSON)
        .setBody("""
            {
                "geofences": [
                    ${geofences.joinToString(",") { geo ->
                        """{
                            "id": "${geo.id}",
                            "name": "${geo.name}",
                            "center": {"latitude": ${geo.lat}, "longitude": ${geo.lon}},
                            "radius": ${geo.radius},
                            "active": true
                        }"""
                    }}
                ]
            }
        """.trimIndent())

    fun geofenceCreateSuccess(
        geofenceId: String = "new-geo-456",
        name: String = "New Geofence"
    ): MockResponse = MockResponse()
        .setResponseCode(201)
        .addHeader("Content-Type", CONTENT_TYPE_JSON)
        .setBody("""
            {
                "geofence": {
                    "id": "$geofenceId",
                    "name": "$name",
                    "active": true
                }
            }
        """.trimIndent())

    fun geofenceEventSuccess(
        eventType: String = "ENTER",
        geofenceId: String = "geo-123"
    ): MockResponse = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", CONTENT_TYPE_JSON)
        .setBody("""
            {
                "event": {
                    "type": "$eventType",
                    "geofenceId": "$geofenceId",
                    "timestamp": "2025-01-01T12:00:00Z"
                }
            }
        """.trimIndent())

    // =============================================================================
    // Trip Responses
    // =============================================================================

    fun tripListSuccess(trips: List<TripData> = listOf(
        TripData("trip-1", "2025-01-01T08:00:00Z", "2025-01-01T08:30:00Z", 5000.0, "driving"),
        TripData("trip-2", "2025-01-01T12:00:00Z", "2025-01-01T12:15:00Z", 1500.0, "walking")
    )): MockResponse = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", CONTENT_TYPE_JSON)
        .setBody("""
            {
                "trips": [
                    ${trips.joinToString(",") { trip ->
                        """{
                            "id": "${trip.id}",
                            "startTime": "${trip.startTime}",
                            "endTime": "${trip.endTime}",
                            "distance": ${trip.distance},
                            "mode": "${trip.mode}"
                        }"""
                    }}
                ]
            }
        """.trimIndent())

    // =============================================================================
    // Proximity Alert Responses
    // =============================================================================

    fun proximityAlertListSuccess(alerts: List<ProximityAlertData> = listOf(
        ProximityAlertData("alert-1", "user-2", 500, true),
        ProximityAlertData("alert-2", "user-3", 1000, false)
    )): MockResponse = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", CONTENT_TYPE_JSON)
        .setBody("""
            {
                "alerts": [
                    ${alerts.joinToString(",") { alert ->
                        """{
                            "id": "${alert.id}",
                            "targetUserId": "${alert.targetUserId}",
                            "radius": ${alert.radius},
                            "active": ${alert.active}
                        }"""
                    }}
                ]
            }
        """.trimIndent())

    fun proximityEventSuccess(
        eventType: String = "ENTER",
        alertId: String = "alert-123"
    ): MockResponse = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", CONTENT_TYPE_JSON)
        .setBody("""
            {
                "event": {
                    "type": "$eventType",
                    "alertId": "$alertId",
                    "timestamp": "2025-01-01T12:00:00Z"
                }
            }
        """.trimIndent())

    // =============================================================================
    // Enterprise Enrollment Responses
    // =============================================================================

    fun enrollmentSuccess(
        enrollmentId: String = "enroll-123",
        policyName: String = "Company Policy"
    ): MockResponse = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", CONTENT_TYPE_JSON)
        .setBody("""
            {
                "enrollment": {
                    "id": "$enrollmentId",
                    "policyName": "$policyName",
                    "status": "active",
                    "enrolledAt": "2025-01-01T00:00:00Z"
                }
            }
        """.trimIndent())

    // =============================================================================
    // Error Responses
    // =============================================================================

    fun serverError(message: String = "Internal server error"): MockResponse = MockResponse()
        .setResponseCode(500)
        .addHeader("Content-Type", CONTENT_TYPE_JSON)
        .setBody("""{"error": "$message"}""")

    fun notFound(message: String = "Resource not found"): MockResponse = MockResponse()
        .setResponseCode(404)
        .addHeader("Content-Type", CONTENT_TYPE_JSON)
        .setBody("""{"error": "$message"}""")

    fun unauthorized(message: String = "Unauthorized"): MockResponse = MockResponse()
        .setResponseCode(401)
        .addHeader("Content-Type", CONTENT_TYPE_JSON)
        .setBody("""{"error": "$message"}""")

    fun rateLimited(retryAfter: Int = 60): MockResponse = MockResponse()
        .setResponseCode(429)
        .addHeader("Content-Type", CONTENT_TYPE_JSON)
        .addHeader("Retry-After", retryAfter.toString())
        .setBody("""{"error": "Rate limit exceeded", "retryAfter": $retryAfter}""")

    // =============================================================================
    // Health Check
    // =============================================================================

    fun healthCheckSuccess(): MockResponse = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", CONTENT_TYPE_JSON)
        .setBody("""{"status": "healthy", "version": "1.0.0"}""")

    // =============================================================================
    // Data Classes for Response Building
    // =============================================================================

    data class DeviceData(val id: String, val name: String, val isPrimary: Boolean)
    data class GroupData(val id: String, val name: String, val memberCount: Int)
    data class MemberData(val id: String, val name: String, val lat: Double, val lon: Double)
    data class LocationData(val lat: Double, val lon: Double, val timestamp: String)
    data class GeofenceData(val id: String, val name: String, val lat: Double, val lon: Double, val radius: Int)
    data class TripData(val id: String, val startTime: String, val endTime: String, val distance: Double, val mode: String)
    data class ProximityAlertData(val id: String, val targetUserId: String, val radius: Int, val active: Boolean)
}
