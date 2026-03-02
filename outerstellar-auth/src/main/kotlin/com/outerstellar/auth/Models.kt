package com.outerstellar.auth

import java.time.Instant

data class User(
    val id: Long,
    val email: String,
    val username: String,
    val passwordHash: String,
    val isActive: Boolean = true,
    val emailVerified: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastLoginAt: Instant? = null,
    val roles: Set<String> = emptySet()
)

data class Role(
    val id: Long,
    val name: String,
    val description: String? = null
)

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAt: Instant,
    val refreshTokenExpiresAt: Instant,
    val tokenType: String = "Bearer"
)

data class AuthConfig(
    val jwtSecret: String,
    val jwtIssuer: String = "outerstellar",
    val accessTokenExpirationMinutes: Long = 15,
    val refreshTokenExpirationDays: Long = 7,
    val bcryptStrength: Int = 12
)

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Failure(val error: AuthError) : AuthResult<Nothing>()
}

sealed class AuthError {
    data object InvalidCredentials : AuthError()
    data object UserNotFound : AuthError()
    data object UserAlreadyExists : AuthError()
    data object EmailAlreadyTaken : AuthError()
    data object UsernameAlreadyTaken : AuthError()
    data object InvalidToken : AuthError()
    data object TokenExpired : AuthError()
    data object TokenRevoked : AuthError()
    data object UserInactive : AuthError()
    data object EmailNotVerified : AuthError()
    data object InsufficientPermissions : AuthError()
    data class ValidationError(val message: String) : AuthError()
    data class DatabaseError(val message: String) : AuthError()
    data class UnknownError(val throwable: Throwable) : AuthError()
}
