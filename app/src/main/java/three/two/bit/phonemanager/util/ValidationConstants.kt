package three.two.bit.phonemanager.util

/**
 * Centralized validation constants for the app.
 *
 * Extracted to avoid magic numbers scattered across ViewModels and screens.
 */
object ValidationConstants {
    // Group name validation
    const val MIN_GROUP_NAME_LENGTH = 3
    const val MAX_GROUP_NAME_LENGTH = 50

    // Display name validation
    const val MIN_DISPLAY_NAME_LENGTH = 2
    const val MAX_DISPLAY_NAME_LENGTH = 50

    // Password validation
    const val MIN_PASSWORD_LENGTH = 8
    const val STRONG_PASSWORD_LENGTH = 12

    // Group ID validation
    const val MIN_GROUP_ID_LENGTH = 2
    const val MAX_GROUP_ID_LENGTH = 50

    // Device link retry
    const val MAX_DEVICE_LINK_RETRIES = 3
}
