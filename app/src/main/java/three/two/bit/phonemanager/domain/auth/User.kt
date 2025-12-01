package three.two.bit.phonemanager.domain.auth

import kotlinx.datetime.Instant

/**
 * Story E9.11: Domain Model for Authenticated User
 *
 * Represents the currently authenticated user in the application.
 * Mapped from network UserInfo model.
 */
data class User(
    val userId: String,
    val email: String,
    val displayName: String,
    val createdAt: Instant
)
