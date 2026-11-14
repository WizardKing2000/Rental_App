package com.example.ra

import org.junit.Assert.*
import org.junit.Test

class AccountAccessTest {
    private val owner = AccountUser("owner-1", "Owner", "owner@example.com", AccountRole.OWNER)
    private val renter = AccountUser("renter-1", "Renter", "renter@example.com", AccountRole.RENTER)

    @Test fun onlyListingOwnerCanManageTheirCars() {
        assertTrue(AccountAccess.canManage(owner, owner.id))
        assertFalse(AccountAccess.canManage(owner, "owner-2"))
        assertFalse(AccountAccess.canManage(renter, renter.id))
        assertFalse(AccountAccess.canManage(null, owner.id))
        assertFalse(AccountAccess.canManage(owner, ""))
    }

    @Test fun bookingsAreVisibleOnlyToTheirParticipantsInTheirRoles() {
        assertTrue(AccountAccess.canViewBooking(owner, owner.id, renter.id))
        assertTrue(AccountAccess.canViewBooking(renter, owner.id, renter.id))
        assertFalse(AccountAccess.canViewBooking(owner.copy(id = "owner-2"), owner.id, renter.id))
        assertFalse(AccountAccess.canViewBooking(renter.copy(id = "renter-2"), owner.id, renter.id))
        assertFalse(AccountAccess.canViewBooking(null, owner.id, renter.id))
        assertFalse(AccountAccess.canViewBooking(owner, "", ""))
    }

    @Test fun onlyRentersCanBook() {
        assertTrue(AccountAccess.canRent(renter))
        assertFalse(AccountAccess.canRent(owner))
        assertFalse(AccountAccess.canRent(null))
    }

    @Test fun tenCharacterPasswordsAreAcceptedAndNineAreRejected() {
        val ten = "Pass12345!"
        assertEquals(10, ten.length)
        assertNull(AccountValidation.registration("Tester", "tester@example.com", ten, ten))
        val nine = ten.dropLast(1)
        assertEquals("Use a password with at least 10 characters.",
            AccountValidation.registration("Tester", "tester@example.com", nine, nine))
    }

    @Test fun registrationValidatesEmailPasswordAndConfirmation() {
        assertNull(AccountValidation.registration("Shanice Bryan", "shanice@example.com", "long-password-123", "long-password-123"))
        assertNotNull(AccountValidation.registration("S", "shanice@example.com", "long-password-123", "long-password-123"))
        assertNotNull(AccountValidation.registration("Shanice", "invalid", "long-password-123", "long-password-123"))
        assertNotNull(AccountValidation.registration("Shanice", "shanice@example.com", "short", "short"))
        assertNotNull(AccountValidation.registration("Shanice", "shanice@example.com", "long-password-123", "different-password"))
    }
}
