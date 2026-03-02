package com.outerstellar.auth

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.Base64

class AuthServiceTest {
    
    private lateinit var database: Database
    private lateinit var authService: AuthService
    
    private val testConfig = AuthConfig(
        jwtSecret = Base64.getEncoder().encodeToString(
            "this-is-a-very-long-secret-key-for-testing-purposes-only-32bytes".toByteArray()
        ),
        jwtIssuer = "test-issuer",
        accessTokenExpirationMinutes = 15,
        refreshTokenExpirationDays = 7,
        bcryptStrength = 4
    )

    @BeforeEach
    fun setup() {
        database = Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
        authService = AuthService(testConfig, database)
        authService.initialize()
    }

    @AfterEach
    fun cleanup() {
        transaction(database) {
            SchemaUtils.drop(Users, Roles, UserRoles, RefreshTokens)
        }
    }

    @Test
    fun `register should create user with valid data`() {
        val result = authService.register(
            email = "test@example.com",
            username = "testuser",
            password = "password123"
        )

        assertTrue(result is AuthResult.Success)
        val user = (result as AuthResult.Success).data
        assertEquals("test@example.com", user.email)
        assertEquals("testuser", user.username)
        assertTrue(user.roles.contains("USER"))
    }

    @Test
    fun `register should fail with invalid email`() {
        val result = authService.register(
            email = "invalid-email",
            username = "testuser",
            password = "password123"
        )

        assertTrue(result is AuthResult.Failure)
        val error = (result as AuthResult.Failure).error
        assertTrue(error is AuthError.ValidationError)
    }

    @Test
    fun `register should fail with duplicate email`() {
        authService.register(
            email = "test@example.com",
            username = "testuser1",
            password = "password123"
        )

        val result = authService.register(
            email = "test@example.com",
            username = "testuser2",
            password = "password123"
        )

        assertTrue(result is AuthResult.Failure)
        val error = (result as AuthResult.Failure).error
        assertTrue(error is AuthError.EmailAlreadyTaken)
    }

    @Test
    fun `login should return tokens with valid credentials`() {
        authService.register(
            email = "test@example.com",
            username = "testuser",
            password = "password123"
        )

        val result = authService.login("test@example.com", "password123")

        assertTrue(result is AuthResult.Success)
        val tokens = (result as AuthResult.Success).data
        assertNotNull(tokens.accessToken)
        assertNotNull(tokens.refreshToken)
        assertEquals("Bearer", tokens.tokenType)
    }

    @Test
    fun `login should fail with invalid password`() {
        authService.register(
            email = "test@example.com",
            username = "testuser",
            password = "password123"
        )

        val result = authService.login("test@example.com", "wrongpassword")

        assertTrue(result is AuthResult.Failure)
        val error = (result as AuthResult.Failure).error
        assertTrue(error is AuthError.InvalidCredentials)
    }

    @Test
    fun `login should work with username`() {
        authService.register(
            email = "test@example.com",
            username = "testuser",
            password = "password123"
        )

        val result = authService.login("testuser", "password123")

        assertTrue(result is AuthResult.Success)
    }

    @Test
    fun `refresh should return new tokens`() {
        authService.register(
            email = "test@example.com",
            username = "testuser",
            password = "password123"
        )

        val loginResult = authService.login("test@example.com", "password123")
        val tokens = (loginResult as AuthResult.Success).data

        val refreshResult = authService.refresh(tokens.refreshToken)

        assertTrue(refreshResult is AuthResult.Success)
        val newTokens = (refreshResult as AuthResult.Success).data
        assertNotEquals(tokens.accessToken, newTokens.accessToken)
        assertNotEquals(tokens.refreshToken, newTokens.refreshToken)
    }

    @Test
    fun `refresh should fail with revoked token`() {
        authService.register(
            email = "test@example.com",
            username = "testuser",
            password = "password123"
        )

        val loginResult = authService.login("test@example.com", "password123")
        val tokens = (loginResult as AuthResult.Success).data

        authService.logout(tokens.refreshToken)

        val refreshResult = authService.refresh(tokens.refreshToken)

        assertTrue(refreshResult is AuthResult.Failure)
        val error = (refreshResult as AuthResult.Failure).error
        assertTrue(error is AuthError.TokenRevoked)
    }

    @Test
    fun `validateAccessToken should return user for valid token`() {
        authService.register(
            email = "test@example.com",
            username = "testuser",
            password = "password123"
        )

        val loginResult = authService.login("test@example.com", "password123")
        val tokens = (loginResult as AuthResult.Success).data

        val validationResult = authService.validateAccessToken(tokens.accessToken)

        assertTrue(validationResult is AuthResult.Success)
        val user = (validationResult as AuthResult.Success).data
        assertEquals("test@example.com", user.email)
    }

    @Test
    fun `validateAccessToken should fail for invalid token`() {
        val result = authService.validateAccessToken("invalid-token")

        assertTrue(result is AuthResult.Failure)
        val error = (result as AuthResult.Failure).error
        assertTrue(error is AuthError.InvalidToken)
    }

    @Test
    fun `hasRole should return true for user with role`() {
        val registerResult = authService.register(
            email = "admin@example.com",
            username = "admin",
            password = "password123",
            roles = setOf("USER", "ADMIN")
        )
        val user = (registerResult as AuthResult.Success).data

        assertTrue(authService.hasRole(user.id, "ADMIN"))
        assertTrue(authService.hasRole(user.id, "USER"))
        assertFalse(authService.hasRole(user.id, "MODERATOR"))
    }

    @Test
    fun `changePassword should update password`() {
        val registerResult = authService.register(
            email = "test@example.com",
            username = "testuser",
            password = "oldpassword"
        )
        val user = (registerResult as AuthResult.Success).data

        val changeResult = authService.changePassword(user.id, "oldpassword", "newpassword123")
        assertTrue(changeResult is AuthResult.Success)

        val newLoginResult = authService.login("test@example.com", "newpassword123")
        assertTrue(newLoginResult is AuthResult.Success)
    }

    @Test
    fun `changePassword should fail with wrong current password`() {
        val registerResult = authService.register(
            email = "test@example.com",
            username = "testuser",
            password = "password123"
        )
        val user = (registerResult as AuthResult.Success).data

        val changeResult = authService.changePassword(user.id, "wrongpassword", "newpassword123")

        assertTrue(changeResult is AuthResult.Failure)
    }
}
