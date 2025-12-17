package three.two.bit.phonemanager.domain.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Story E11.9 Task 14: Unit tests for GroupInvite model
 *
 * Tests:
 * - Invite validity checks
 * - Expiry calculations
 * - Deep link generation
 * - Multi-use behavior
 */
class GroupInviteTest {

    // Helper to create a test invite
    private fun createInvite(
        expiresAt: Instant = Clock.System.now() + 7.days,
        status: InviteStatus = InviteStatus.ACTIVE,
        maxUses: Int = 1,
        usesRemaining: Int = 1,
    ) = GroupInvite(
        id = "invite-1",
        groupId = "group-1",
        groupName = "Test Group",
        code = "ABCD1234",
        createdBy = "user-1",
        createdAt = Clock.System.now(),
        expiresAt = expiresAt,
        maxUses = maxUses,
        usesRemaining = usesRemaining,
        status = status,
    )

    // isExpired Tests

    @Test
    fun `isExpired returns false for future expiry`() {
        val invite = createInvite(expiresAt = Clock.System.now() + 7.days)
        assertFalse(invite.isExpired())
    }

    @Test
    fun `isExpired returns true for past expiry`() {
        val invite = createInvite(expiresAt = Clock.System.now() - 1.days)
        assertTrue(invite.isExpired())
    }

    // isValid Tests

    @Test
    fun `isValid returns true for active non-expired invite with uses remaining`() {
        val invite = createInvite()
        assertTrue(invite.isValid())
    }

    @Test
    fun `isValid returns false for expired invite`() {
        val invite = createInvite(expiresAt = Clock.System.now() - 1.days)
        assertFalse(invite.isValid())
    }

    @Test
    fun `isValid returns false for revoked invite`() {
        val invite = createInvite(status = InviteStatus.REVOKED)
        assertFalse(invite.isValid())
    }

    @Test
    fun `isValid returns false for invite with no uses remaining`() {
        val invite = createInvite(maxUses = 1, usesRemaining = 0)
        assertFalse(invite.isValid())
    }

    @Test
    fun `isValid returns true for unlimited use invite`() {
        val invite = createInvite(maxUses = -1, usesRemaining = 0)
        assertTrue(invite.isValid())
    }

    // isMultiUse Tests

    @Test
    fun `isMultiUse returns false for single use invite`() {
        val invite = createInvite(maxUses = 1)
        assertFalse(invite.isMultiUse())
    }

    @Test
    fun `isMultiUse returns true for multi use invite`() {
        val invite = createInvite(maxUses = 5)
        assertTrue(invite.isMultiUse())
    }

    @Test
    fun `isMultiUse returns true for unlimited invite`() {
        val invite = createInvite(maxUses = -1)
        assertTrue(invite.isMultiUse())
    }

    // getDeepLink Tests

    @Test
    fun `getDeepLink generates correct format`() {
        val invite = createInvite()
        assertEquals("phonemanager://join/ABCD1234", invite.getDeepLink())
    }

    // getShareUrl Tests

    @Test
    fun `getShareUrl generates correct HTTPS format`() {
        val invite = createInvite()
        assertEquals("https://phonemanager.app/join/ABCD1234", invite.getShareUrl())
    }

    // getTimeRemainingMillis Tests

    @Test
    fun `getTimeRemainingMillis returns positive for future expiry`() {
        val invite = createInvite(expiresAt = Clock.System.now() + 1.days)
        assertTrue(invite.getTimeRemainingMillis() > 0)
    }

    @Test
    fun `getTimeRemainingMillis returns zero for past expiry`() {
        val invite = createInvite(expiresAt = Clock.System.now() - 1.days)
        assertEquals(0L, invite.getTimeRemainingMillis())
    }

    // getExpiryUrgency Tests

    @Test
    fun `getExpiryUrgency returns EXPIRED for past expiry`() {
        val invite = createInvite(expiresAt = Clock.System.now() - 1.days)
        assertEquals(ExpiryUrgency.EXPIRED, invite.getExpiryUrgency())
    }

    @Test
    fun `getExpiryUrgency returns CRITICAL for less than 1 hour`() {
        val invite = createInvite(expiresAt = Clock.System.now() + 30.minutes)
        assertEquals(ExpiryUrgency.CRITICAL, invite.getExpiryUrgency())
    }

    @Test
    fun `getExpiryUrgency returns WARNING for less than 24 hours`() {
        val invite = createInvite(expiresAt = Clock.System.now() + 12.hours)
        assertEquals(ExpiryUrgency.WARNING, invite.getExpiryUrgency())
    }

    @Test
    fun `getExpiryUrgency returns NORMAL for more than 24 hours`() {
        val invite = createInvite(expiresAt = Clock.System.now() + 2.days)
        assertEquals(ExpiryUrgency.NORMAL, invite.getExpiryUrgency())
    }

    // InviteStatus Tests

    @Test
    fun `InviteStatus ACTIVE is correct`() {
        assertEquals("ACTIVE", InviteStatus.ACTIVE.name)
    }

    @Test
    fun `InviteStatus EXPIRED is correct`() {
        assertEquals("EXPIRED", InviteStatus.EXPIRED.name)
    }

    @Test
    fun `InviteStatus REVOKED is correct`() {
        assertEquals("REVOKED", InviteStatus.REVOKED.name)
    }

    @Test
    fun `InviteStatus USED is correct`() {
        assertEquals("USED", InviteStatus.USED.name)
    }
}
