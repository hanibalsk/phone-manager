package three.two.bit.phonemanager.util

import three.two.bit.phonemanager.BuildConfig

/**
 * Story E11.9: Shared utility for invite code extraction
 *
 * Review Follow-up: Extract extractInviteCode() to shared utility to eliminate duplication
 * Used by JoinGroupViewModel and QRScannerScreen
 */
object InviteCodeUtils {

    /**
     * Deep link scheme for the app (e.g., "phonemanager")
     */
    val DEEP_LINK_SCHEME: String
        get() = BuildConfig.DEEP_LINK_SCHEME

    /**
     * Extract invite code from various content formats.
     *
     * Supports:
     * - Custom scheme: phonemanager://join/{code}
     * - Plain 8-character alphanumeric code
     * - HTTPS URL format: https://example.com/join/{code}
     *
     * @param content The raw content from QR scan, deep link, or user input
     * @return The extracted 8-character invite code (uppercase), or null if not found
     */
    fun extractInviteCode(content: String): String? {
        val trimmed = content.trim()

        // Check for custom scheme deep link format: phonemanager://join/{code}
        val deepLinkRegex = Regex("""$DEEP_LINK_SCHEME://join/([A-Za-z0-9]{8})""", RegexOption.IGNORE_CASE)
        deepLinkRegex.find(trimmed)?.let { match ->
            return match.groupValues[1].uppercase()
        }

        // Check for plain 8-character alphanumeric code
        if (trimmed.length == 8 && trimmed.all { it.isLetterOrDigit() }) {
            return trimmed.uppercase()
        }

        // Check for HTTPS URL format: https://example.com/join/{code}
        val urlRegex = Regex("""https?://[^/]+/join/([A-Za-z0-9]{8})""", RegexOption.IGNORE_CASE)
        urlRegex.find(trimmed)?.let { match ->
            return match.groupValues[1].uppercase()
        }

        return null
    }

    /**
     * Validate an invite code format.
     *
     * @param code The code to validate
     * @return True if the code is exactly 8 alphanumeric characters
     */
    fun isValidCodeFormat(code: String): Boolean {
        return code.length == 8 && code.all { it.isLetterOrDigit() }
    }

    /**
     * Normalize an invite code (uppercase, alphanumeric only, max 8 chars).
     *
     * @param code The raw code input
     * @return Normalized code
     */
    fun normalizeCode(code: String): String {
        return code
            .uppercase()
            .filter { it.isLetterOrDigit() }
            .take(8)
    }

    /**
     * Generate a deep link URL for an invite code.
     *
     * @param code The invite code
     * @return The deep link URL (e.g., "phonemanager://join/ABC12XYZ")
     */
    fun generateDeepLink(code: String): String {
        return "$DEEP_LINK_SCHEME://join/${code.uppercase()}"
    }
}
