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
     * - Plain XXX-XXX-XXX format (11 characters with dashes)
     * - HTTPS URL format: https://example.com/join/{code}
     *
     * @param content The raw content from QR scan, deep link, or user input
     * @return The extracted invite code in XXX-XXX-XXX format (uppercase), or null if not found
     */
    fun extractInviteCode(content: String): String? {
        val trimmed = content.trim()

        // Check for custom scheme deep link format: phonemanager://join/{code}
        // Supports XXX-XXX-XXX format (11 chars with dashes)
        val deepLinkRegex = Regex("""$DEEP_LINK_SCHEME://join/([A-Za-z0-9]{3}-[A-Za-z0-9]{3}-[A-Za-z0-9]{3})""", RegexOption.IGNORE_CASE)
        deepLinkRegex.find(trimmed)?.let { match ->
            return match.groupValues[1].uppercase()
        }

        // Check for plain XXX-XXX-XXX format (11 characters with dashes)
        val codeRegex = Regex("""^[A-Za-z0-9]{3}-[A-Za-z0-9]{3}-[A-Za-z0-9]{3}$""")
        if (codeRegex.matches(trimmed)) {
            return trimmed.uppercase()
        }

        // Check for HTTPS URL format: https://example.com/join/{code}
        val urlRegex = Regex("""https?://[^/]+/join/([A-Za-z0-9]{3}-[A-Za-z0-9]{3}-[A-Za-z0-9]{3})""", RegexOption.IGNORE_CASE)
        urlRegex.find(trimmed)?.let { match ->
            return match.groupValues[1].uppercase()
        }

        return null
    }

    /**
     * Validate an invite code format.
     *
     * @param code The code to validate
     * @return True if the code is valid (XXX-XXX-XXX format: 11 chars with dashes)
     */
    fun isValidCodeFormat(code: String): Boolean {
        // Format: XXX-XXX-XXX (11 chars with 2 dashes, 9 alphanumeric)
        val codeRegex = Regex("""^[A-Za-z0-9]{3}-[A-Za-z0-9]{3}-[A-Za-z0-9]{3}$""")
        return codeRegex.matches(code)
    }

    /**
     * Normalize an invite code (uppercase, alphanumeric and dashes only, max 11 chars for format like XXX-XXX-XXX).
     *
     * @param code The raw code input
     * @return Normalized code
     */
    fun normalizeCode(code: String): String {
        return code
            .uppercase()
            .filter { it.isLetterOrDigit() || it == '-' }
            .take(11) // Allow for format like XXX-XXX-XXX
    }

    /**
     * Generate a deep link URL for an invite code.
     *
     * @param code The invite code
     * @return The deep link URL (e.g., "phonemanager://join/ABC12XYZ")
     */
    fun generateDeepLink(code: String): String = "$DEEP_LINK_SCHEME://join/${code.uppercase()}"
}
