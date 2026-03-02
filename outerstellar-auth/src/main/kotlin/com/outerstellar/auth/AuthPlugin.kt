package com.outerstellar.auth

import com.outerstellar.plugin.ServerPlugin
import com.outerstellar.plugin.exposed.SchemaProvider
import org.jetbrains.exposed.sql.Database

class AuthPlugin(
    private val config: AuthConfig,
    private val database: Database? = null
) : ServerPlugin, SchemaProvider {

    override val name: String = "outerstellar-auth"
    override val version: String = "1.0.0"
    override val description: String = "Framework-agnostic authentication with JWT and user management"

    private lateinit var authService: AuthService

    val service: AuthService
        get() = authService

    override fun initialize() {
        authService = AuthService(config, database)
        authService.initialize()
    }

    override fun shutdown() {
        authService.cleanupExpiredTokens()
    }

    override fun getTables() = listOf(Users, Roles, UserRoles, RefreshTokens)

    override fun getDataClasses() = listOf(
        User::class.qualifiedName ?: "com.outerstellar.auth.User",
        Role::class.qualifiedName ?: "com.outerstellar.auth.Role",
        AuthTokens::class.qualifiedName ?: "com.outerstellar.auth.AuthTokens"
    )

    companion object {
        @JvmStatic
        @JvmName("create")
        fun create(config: AuthConfig): AuthPlugin {
            return AuthPlugin(config)
        }

        @JvmStatic
        @JvmName("createWithDatabase")
        fun create(config: AuthConfig, database: Database): AuthPlugin {
            return AuthPlugin(config, database)
        }
    }
}
