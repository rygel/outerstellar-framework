package com.outerstellar.auth

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.Base64

class JwtServiceTest {

    private val config = AuthConfig(
        jwtSecret = Base64.getEncoder().encodeToString(
            "this-is-a-very-long-secret-key-for-testing-purposes-only-32bytes".toByteArray()
        ),
        jwtIssuer = "test-issuer",
        accessTokenExpirationMinutes = 15,
        refreshTokenExpirationDays = 7
    )

    private val jwtService = JwtService(config)

    @Test
    fun `generateAccessToken should create valid token`() {
        val token = jwtService.generateAccessToken(
            userId = 1L,
            email = "test@example.com",
            username = "testuser",
            roles = setOf("USER", "ADMIN")
        )

        assertNotNull(token)
        assertTrue(token.isNotEmpty())
    }

    @Test
    fun `generateRefreshToken should create valid token`() {
        val token = jwtService.generateRefreshToken(userId = 1L)

        assertNotNull(token)
        assertTrue(token.isNotEmpty())
    }

    @Test
    fun `validateToken should return valid result for correct token`() {
        val token = jwtService.generateAccessToken(
            userId = 1L,
            email = "test@example.com",
            username = "testuser",
            roles = setOf("USER")
        )

        val result = jwtService.validateToken(token)

        assertTrue(result is TokenValidationResult.Valid)
        val validResult = result as TokenValidationResult.Valid
        assertEquals(1L, validResult.userId)
        assertNotNull(validResult.expiresAt)
        assertNotNull(validResult.claims)
    }

    @Test
    fun `validateToken should return invalid for malformed token`() {
        val result = jwtService.validateToken("invalid.token.here")

        assertTrue(result is TokenValidationResult.Invalid)
    }

    @Test
    fun `validateToken should return invalid for wrong signature`() {
        val wrongConfig = AuthConfig(
            jwtSecret = Base64.getEncoder().encodeToString(
                "different-secret-key-for-testing-purposes-32-bytes".toByteArray()
            ),
            jwtIssuer = "test-issuer"
        )
        val wrongJwtService = JwtService(wrongConfig)

        val token = jwtService.generateAccessToken(
            userId = 1L,
            email = "test@example.com",
            username = "testuser",
            roles = setOf("USER")
        )

        val result = wrongJwtService.validateToken(token)

        assertTrue(result is TokenValidationResult.Invalid)
    }

    @Test
    fun `extractUserId should return correct id`() {
        val token = jwtService.generateAccessToken(
            userId = 42L,
            email = "test@example.com",
            username = "testuser",
            roles = setOf("USER")
        )

        val userId = jwtService.extractUserId(token)

        assertEquals(42L, userId)
    }

    @Test
    fun `extractUserId should return null for invalid token`() {
        val userId = jwtService.extractUserId("invalid-token")

        assertNull(userId)
    }

    @Test
    fun `extractClaims should return claims for valid token`() {
        val token = jwtService.generateAccessToken(
            userId = 1L,
            email = "test@example.com",
            username = "testuser",
            roles = setOf("USER", "ADMIN")
        )

        val claims = jwtService.extractClaims(token)

        assertNotNull(claims)
        assertEquals("1", claims!!.subject)
        assertEquals("test@example.com", claims["email"])
        assertEquals("testuser", claims["username"])
        assertEquals(config.jwtIssuer, claims.issuer)
    }

    @Test
    fun `getTokenExpiration should return expiration date`() {
        val token = jwtService.generateAccessToken(
            userId = 1L,
            email = "test@example.com",
            username = "testuser",
            roles = setOf("USER")
        )

        val expiration = jwtService.getTokenExpiration(token)

        assertNotNull(expiration)
        assertTrue(expiration!!.isAfter(java.time.Instant.now()))
    }
}
