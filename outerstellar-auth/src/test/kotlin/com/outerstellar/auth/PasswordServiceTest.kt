package com.outerstellar.auth

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class PasswordServiceTest {

    private val passwordService = PasswordService(strength = 4)

    @Test
    fun `hash should return different hash each time`() {
        val password = "password123"
        val hash1 = passwordService.hash(password)
        val hash2 = passwordService.hash(password)

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `verify should return true for correct password`() {
        val password = "password123"
        val hash = passwordService.hash(password)

        assertTrue(passwordService.verify(password, hash))
    }

    @Test
    fun `verify should return false for wrong password`() {
        val password = "password123"
        val hash = passwordService.hash(password)

        assertFalse(passwordService.verify("wrongpassword", hash))
    }

    @Test
    fun `needsRehash should return true for weaker hash`() {
        val weakService = PasswordService(strength = 4)
        val strongService = PasswordService(strength = 12)

        val weakHash = weakService.hash("password123")

        assertTrue(strongService.needsRehash(weakHash))
    }

    @Test
    fun `needsRehash should return false for same strength hash`() {
        val hash = passwordService.hash("password123")

        assertFalse(passwordService.needsRehash(hash))
    }
}
