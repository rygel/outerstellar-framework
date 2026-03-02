package com.outerstellar.auth

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object Users : Table("users") {
    val id = long("id").autoIncrement()
    val email = varchar("email", 255).uniqueIndex()
    val username = varchar("username", 100).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val isActive = bool("is_active").default(true)
    val emailVerified = bool("email_verified").default(false)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val lastLoginAt = timestamp("last_login_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

object Roles : Table("roles") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 50).uniqueIndex()
    val description = varchar("description", 255).nullable()

    override val primaryKey = PrimaryKey(id)
}

object UserRoles : Table("user_roles") {
    val userId = long("user_id").references(Users.id, onDelete = org.jetbrains.exposed.sql.ReferenceOption.CASCADE)
    val roleId = long("role_id").references(Roles.id, onDelete = org.jetbrains.exposed.sql.ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(userId, roleId)
}

object RefreshTokens : Table("refresh_tokens") {
    val id = long("id").autoIncrement()
    val userId = long("user_id").references(Users.id, onDelete = org.jetbrains.exposed.sql.ReferenceOption.CASCADE)
    val tokenHash = varchar("token_hash", 255).uniqueIndex()
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at")
    val revoked = bool("revoked").default(false)

    override val primaryKey = PrimaryKey(id)
}
