package com.outerstellar.auth

import org.jetbrains.exposed.sql.Database
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64

class AuthService(
    private val config: AuthConfig,
    private val database: Database? = null
) {
    private val userRepository = UserRepository(database)
    private val passwordService = PasswordService(config.bcryptStrength)
    private val jwtService = JwtService(config)

    fun initialize() {
        userRepository.createSchema()
    }

    fun register(
        email: String,
        username: String,
        password: String,
        roles: Set<String> = setOf("USER")
    ): AuthResult<User> {
        val validationError = validateRegistration(email, username, password)
        if (validationError != null) {
            return AuthResult.Failure(validationError)
        }

        return try {
            if (userRepository.emailExists(email)) {
                return AuthResult.Failure(AuthError.EmailAlreadyTaken)
            }
            if (userRepository.usernameExists(username)) {
                return AuthResult.Failure(AuthError.UsernameAlreadyTaken)
            }

            val passwordHash = passwordService.hash(password)
            val user = userRepository.create(email, username, passwordHash, roles)
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Failure(AuthError.DatabaseError(e.message ?: "Failed to create user"))
        }
    }

    fun login(emailOrUsername: String, password: String): AuthResult<AuthTokens> {
        return try {
            val user = userRepository.findByEmail(emailOrUsername)
                ?: userRepository.findByUsername(emailOrUsername)
                ?: return AuthResult.Failure(AuthError.InvalidCredentials)

            if (!user.isActive) {
                return AuthResult.Failure(AuthError.UserInactive)
            }

            if (!passwordService.verify(password, user.passwordHash)) {
                return AuthResult.Failure(AuthError.InvalidCredentials)
            }

            if (passwordService.needsRehash(user.passwordHash)) {
                val newHash = passwordService.hash(password)
                userRepository.update(user.id, UserUpdate(passwordHash = newHash))
            }

            userRepository.updateLastLogin(user.id)

            val tokens = generateTokens(user)

            val tokenHash = hashToken(tokens.refreshToken)
            val refreshTokenExpiration = Instant.now().plus(config.refreshTokenExpirationDays, ChronoUnit.DAYS)
            userRepository.createRefreshToken(user.id, tokenHash, refreshTokenExpiration)

            AuthResult.Success(tokens)
        } catch (e: Exception) {
            AuthResult.Failure(AuthError.UnknownError(e))
        }
    }

    fun refresh(refreshToken: String): AuthResult<AuthTokens> {
        val tokenHash = hashToken(refreshToken)
        
        val storedToken = userRepository.findRefreshToken(tokenHash)
        when {
            storedToken == null -> return AuthResult.Failure(AuthError.InvalidToken)
            storedToken.revoked -> return AuthResult.Failure(AuthError.TokenRevoked)
            storedToken.expiresAt.isBefore(Instant.now()) -> return AuthResult.Failure(AuthError.TokenExpired)
        }

        val user = userRepository.findById(storedToken.userId)
            ?: return AuthResult.Failure(AuthError.UserNotFound)

        if (!user.isActive) {
            return AuthResult.Failure(AuthError.UserInactive)
        }

        userRepository.revokeRefreshToken(tokenHash)

        val tokens = generateTokens(user)

        val newTokenHash = hashToken(tokens.refreshToken)
        val newExpiration = Instant.now().plus(config.refreshTokenExpirationDays, ChronoUnit.DAYS)
        userRepository.createRefreshToken(user.id, newTokenHash, newExpiration)

        return AuthResult.Success(tokens)
    }

    fun logout(refreshToken: String): AuthResult<Boolean> {
        val tokenHash = hashToken(refreshToken)
        val revoked = userRepository.revokeRefreshToken(tokenHash)
        return if (revoked) {
            AuthResult.Success(true)
        } else {
            AuthResult.Success(false)
        }
    }

    fun logoutAll(userId: Long): AuthResult<Int> {
        val count = userRepository.revokeAllUserTokens(userId)
        return AuthResult.Success(count)
    }

    fun validateAccessToken(token: String): AuthResult<User> {
        val validation = jwtService.validateToken(token)

        return when (validation) {
            is TokenValidationResult.Valid -> {
                val user = userRepository.findById(validation.userId)
                    ?: return AuthResult.Failure(AuthError.UserNotFound)

                if (!user.isActive) {
                    return AuthResult.Failure(AuthError.UserInactive)
                }

                AuthResult.Success(user)
            }
            TokenValidationResult.Expired -> AuthResult.Failure(AuthError.TokenExpired)
            TokenValidationResult.Invalid -> AuthResult.Failure(AuthError.InvalidToken)
        }
    }

    fun getUser(userId: Long): AuthResult<User> {
        val user = userRepository.findById(userId)
            ?: return AuthResult.Failure(AuthError.UserNotFound)
        return AuthResult.Success(user)
    }

    fun getUserByEmail(email: String): AuthResult<User> {
        val user = userRepository.findByEmail(email)
            ?: return AuthResult.Failure(AuthError.UserNotFound)
        return AuthResult.Success(user)
    }

    fun getUserByUsername(username: String): AuthResult<User> {
        val user = userRepository.findByUsername(username)
            ?: return AuthResult.Failure(AuthError.UserNotFound)
        return AuthResult.Success(user)
    }

    fun updateUser(userId: Long, updates: UserUpdate): AuthResult<User> {
        updates.email?.let { email ->
            if (!isValidEmail(email)) {
                return AuthResult.Failure(AuthError.ValidationError("Invalid email format"))
            }
            val existingUser = userRepository.findByEmail(email)
            if (existingUser != null && existingUser.id != userId) {
                return AuthResult.Failure(AuthError.EmailAlreadyTaken)
            }
        }

        updates.username?.let { username ->
            if (!isValidUsername(username)) {
                return AuthResult.Failure(AuthError.ValidationError("Invalid username format"))
            }
            val existingUser = userRepository.findByUsername(username)
            if (existingUser != null && existingUser.id != userId) {
                return AuthResult.Failure(AuthError.UsernameAlreadyTaken)
            }
        }

        val updatedUser = userRepository.update(userId, updates)
            ?: return AuthResult.Failure(AuthError.UserNotFound)

        return AuthResult.Success(updatedUser)
    }

    fun changePassword(userId: Long, currentPassword: String, newPassword: String): AuthResult<Boolean> {
        val user = userRepository.findById(userId)
            ?: return AuthResult.Failure(AuthError.UserNotFound)

        if (!passwordService.verify(currentPassword, user.passwordHash)) {
            return AuthResult.Failure(AuthError.InvalidCredentials)
        }

        if (!isValidPassword(newPassword)) {
            return AuthResult.Failure(AuthError.ValidationError("Password does not meet requirements"))
        }

        val newHash = passwordService.hash(newPassword)
        userRepository.update(userId, UserUpdate(passwordHash = newHash))

        return AuthResult.Success(true)
    }

    fun deleteUser(userId: Long): AuthResult<Boolean> {
        val deleted = userRepository.delete(userId)
        return if (deleted) {
            AuthResult.Success(true)
        } else {
            AuthResult.Failure(AuthError.UserNotFound)
        }
    }

    fun addRole(userId: Long, roleName: String): AuthResult<Boolean> {
        val success = userRepository.addRole(userId, roleName)
        return if (success) {
            AuthResult.Success(true)
        } else {
            AuthResult.Failure(AuthError.ValidationError("Role '$roleName' not found"))
        }
    }

    fun removeRole(userId: Long, roleName: String): AuthResult<Boolean> {
        val success = userRepository.removeRole(userId, roleName)
        return AuthResult.Success(success)
    }

    fun hasRole(userId: Long, roleName: String): Boolean {
        return userRepository.getUserRoles(userId).contains(roleName)
    }

    fun hasAnyRole(userId: Long, roles: Set<String>): Boolean {
        val userRoles = userRepository.getUserRoles(userId)
        return roles.any { it in userRoles }
    }

    fun hasAllRoles(userId: Long, roles: Set<String>): Boolean {
        val userRoles = userRepository.getUserRoles(userId)
        return roles.all { it in userRoles }
    }

    fun listUsers(limit: Int = 100, offset: Int = 0): AuthResult<List<User>> {
        val users = userRepository.findAll(limit, offset)
        return AuthResult.Success(users)
    }

    fun countUsers(): AuthResult<Long> {
        return AuthResult.Success(userRepository.count())
    }

    fun cleanupExpiredTokens(): Int {
        return userRepository.deleteExpiredRefreshTokens()
    }

    private fun generateTokens(user: User): AuthTokens {
        val accessToken = jwtService.generateAccessToken(
            userId = user.id,
            email = user.email,
            username = user.username,
            roles = user.roles
        )

        val refreshToken = jwtService.generateRefreshToken(user.id)

        val accessTokenExpiration = Instant.now().plus(config.accessTokenExpirationMinutes, ChronoUnit.MINUTES)
        val refreshTokenExpiration = Instant.now().plus(config.refreshTokenExpirationDays, ChronoUnit.DAYS)

        return AuthTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessTokenExpiresAt = accessTokenExpiration,
            refreshTokenExpiresAt = refreshTokenExpiration
        )
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(token.toByteArray())
        return Base64.getEncoder().encodeToString(hash)
    }

    private fun validateRegistration(email: String, username: String, password: String): AuthError? {
        if (!isValidEmail(email)) {
            return AuthError.ValidationError("Invalid email format")
        }
        if (!isValidUsername(username)) {
            return AuthError.ValidationError("Username must be 3-100 characters and contain only letters, numbers, and underscores")
        }
        if (!isValidPassword(password)) {
            return AuthError.ValidationError("Password must be at least 8 characters")
        }
        return null
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return email.matches(emailRegex)
    }

    private fun isValidUsername(username: String): Boolean {
        val usernameRegex = Regex("^[a-zA-Z0-9_]{3,100}$")
        return username.matches(usernameRegex)
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 8
    }
}
