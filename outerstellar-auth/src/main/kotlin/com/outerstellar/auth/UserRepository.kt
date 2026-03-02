package com.outerstellar.auth

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

class UserRepository(private val database: Database? = null) {

    private fun <T> transaction(block: Transaction.() -> T): T {
        return if (database != null) {
            org.jetbrains.exposed.sql.transactions.transaction(db = database, statement = block)
        } else {
            org.jetbrains.exposed.sql.transactions.transaction(statement = block)
        }
    }

    fun createSchema() = transaction {
        SchemaUtils.create(Users, Roles, UserRoles, RefreshTokens)
        
        val defaultRoles = listOf(
            "USER" to "Default user role",
            "ADMIN" to "Administrator with full access",
            "MODERATOR" to "Content moderator"
        )
        
        defaultRoles.forEach { (name, description) ->
            if (Roles.select { Roles.name eq name }.empty()) {
                Roles.insert {
                    it[Roles.name] = name
                    it[Roles.description] = description
                }
            }
        }
    }

    fun findById(id: Long): User? = transaction {
        Users.select { Users.id eq id }
            .map { it.toUser() }
            .singleOrNull()
            ?.let { user ->
                user.copy(roles = getUserRoles(id))
            }
    }

    fun findByEmail(email: String): User? = transaction {
        Users.select { Users.email eq email }
            .map { it.toUser() }
            .singleOrNull()
            ?.let { user ->
                user.copy(roles = getUserRoles(user.id))
            }
    }

    fun findByUsername(username: String): User? = transaction {
        Users.select { Users.username eq username }
            .map { it.toUser() }
            .singleOrNull()
            ?.let { user ->
                user.copy(roles = getUserRoles(user.id))
            }
    }

    fun emailExists(email: String): Boolean = transaction {
        !Users.select { Users.email eq email }.empty()
    }

    fun usernameExists(username: String): Boolean = transaction {
        !Users.select { Users.username eq username }.empty()
    }

    fun create(
        email: String,
        username: String,
        passwordHash: String,
        roles: Set<String> = setOf("USER")
    ): User = transaction {
        val now = Instant.now()
        val userId = Users.insert {
            it[Users.email] = email
            it[Users.username] = username
            it[Users.passwordHash] = passwordHash
            it[createdAt] = now
            it[updatedAt] = now
        } get Users.id

        roles.forEach { roleName ->
            val roleId = Roles.select { Roles.name eq roleName }
                .map { it[Roles.id] }
                .singleOrNull()
            
            if (roleId != null) {
                UserRoles.insert {
                    it[UserRoles.userId] = userId
                    it[UserRoles.roleId] = roleId
                }
            }
        }

        User(
            id = userId,
            email = email,
            username = username,
            passwordHash = passwordHash,
            createdAt = now,
            updatedAt = now,
            roles = roles
        )
    }

    fun updateLastLogin(userId: Long) = transaction {
        Users.update({ Users.id eq userId }) {
            it[lastLoginAt] = Instant.now()
        }
    }

    fun update(userId: Long, updates: UserUpdate): User? = transaction {
        val current = findById(userId) ?: return@transaction null
        
        Users.update({ Users.id eq userId }) {
            updates.email?.let { email -> it[Users.email] = email }
            updates.username?.let { username -> it[Users.username] = username }
            updates.passwordHash?.let { hash -> it[passwordHash] = hash }
            updates.isActive?.let { active -> it[Users.isActive] = active }
            updates.emailVerified?.let { verified -> it[Users.emailVerified] = verified }
            it[updatedAt] = Instant.now()
        }

        findById(userId)
    }

    fun delete(userId: Long): Boolean = transaction {
        Users.deleteWhere { Users.id eq userId } > 0
    }

    fun addRole(userId: Long, roleName: String): Boolean = transaction {
        val roleId = Roles.select { Roles.name eq roleName }
            .map { it[Roles.id] }
            .singleOrNull() ?: return@transaction false

        val exists = UserRoles.select {
            (UserRoles.userId eq userId) and (UserRoles.roleId eq roleId)
        }.count() > 0

        if (!exists) {
            UserRoles.insert {
                it[UserRoles.userId] = userId
                it[UserRoles.roleId] = roleId
            }
        }
        true
    }

    fun removeRole(userId: Long, roleName: String): Boolean = transaction {
        val roleId = Roles.select { Roles.name eq roleName }
            .map { it[Roles.id] }
            .singleOrNull() ?: return@transaction false

        UserRoles.deleteWhere {
            (UserRoles.userId eq userId) and (UserRoles.roleId eq roleId)
        } > 0
    }

    fun getUserRoles(userId: Long): Set<String> = transaction {
        (UserRoles innerJoin Roles)
            .select { UserRoles.userId eq userId }
            .map { it[Roles.name] }
            .toSet()
    }

    fun createRefreshToken(userId: Long, tokenHash: String, expiresAt: Instant) = transaction {
        RefreshTokens.insert {
            it[RefreshTokens.userId] = userId
            it[RefreshTokens.tokenHash] = tokenHash
            it[RefreshTokens.expiresAt] = expiresAt
            it[createdAt] = Instant.now()
        }
    }

    fun findRefreshToken(tokenHash: String): RefreshTokenRecord? = transaction {
        RefreshTokens.select { RefreshTokens.tokenHash eq tokenHash }
            .map { 
                RefreshTokenRecord(
                    id = it[RefreshTokens.id],
                    userId = it[RefreshTokens.userId],
                    tokenHash = it[RefreshTokens.tokenHash],
                    expiresAt = it[RefreshTokens.expiresAt],
                    revoked = it[RefreshTokens.revoked]
                )
            }
            .singleOrNull()
    }

    fun revokeRefreshToken(tokenHash: String): Boolean = transaction {
        RefreshTokens.update({ RefreshTokens.tokenHash eq tokenHash }) {
            it[revoked] = true
        } > 0
    }

    fun revokeAllUserTokens(userId: Long): Int = transaction {
        RefreshTokens.update({ RefreshTokens.userId eq userId }) {
            it[revoked] = true
        }
    }

    fun deleteExpiredRefreshTokens(): Int = transaction {
        val now = java.sql.Timestamp.from(Instant.now())
        RefreshTokens.deleteWhere { SqlExpressionBuilder.run { 
            (expiresAt lessEq now.toInstant()) or (revoked eq true)
        } }
    }

    fun findAll(limit: Int = 100, offset: Int = 0): List<User> = transaction {
        Users.selectAll()
            .limit(limit, offset.toLong())
            .map { it.toUser().copy(roles = getUserRoles(it[Users.id])) }
    }

    fun count(): Long = transaction {
        Users.selectAll().count()
    }

    private fun ResultRow.toUser() = User(
        id = this[Users.id],
        email = this[Users.email],
        username = this[Users.username],
        passwordHash = this[Users.passwordHash],
        isActive = this[Users.isActive],
        emailVerified = this[Users.emailVerified],
        createdAt = this[Users.createdAt],
        updatedAt = this[Users.updatedAt],
        lastLoginAt = this[Users.lastLoginAt]
    )
}

data class UserUpdate(
    val email: String? = null,
    val username: String? = null,
    val passwordHash: String? = null,
    val isActive: Boolean? = null,
    val emailVerified: Boolean? = null
)

data class RefreshTokenRecord(
    val id: Long,
    val userId: Long,
    val tokenHash: String,
    val expiresAt: Instant,
    val revoked: Boolean
)
