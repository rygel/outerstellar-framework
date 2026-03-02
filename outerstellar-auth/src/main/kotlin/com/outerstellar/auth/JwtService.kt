package com.outerstellar.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.Date
import java.util.UUID

class JwtService(private val config: AuthConfig) {
    private val secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(config.jwtSecret))
    private val parser = Jwts.parser().verifyWith(secretKey).build()

    fun generateAccessToken(userId: Long, email: String, username: String, roles: Set<String>): String {
        val now = Instant.now()
        val expiration = now.plus(config.accessTokenExpirationMinutes, ChronoUnit.MINUTES)

        return Jwts.builder()
            .subject(userId.toString())
            .issuer(config.jwtIssuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .claim("email", email)
            .claim("username", username)
            .claim("roles", roles.toList())
            .id(UUID.randomUUID().toString())
            .signWith(secretKey)
            .compact()
    }

    fun generateRefreshToken(userId: Long): String {
        val now = Instant.now()
        val expiration = now.plus(config.refreshTokenExpirationDays, ChronoUnit.DAYS)

        return Jwts.builder()
            .subject(userId.toString())
            .issuer(config.jwtIssuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .claim("type", "refresh")
            .id(UUID.randomUUID().toString())
            .signWith(secretKey)
            .compact()
    }

    fun validateToken(token: String): TokenValidationResult {
        return try {
            val claims = parser.parseSignedClaims(token).payload
            TokenValidationResult.Valid(
                userId = claims.subject.toLong(),
                expiresAt = claims.expiration.toInstant(),
                claims = claims
            )
        } catch (e: ExpiredJwtException) {
            TokenValidationResult.Expired
        } catch (e: SignatureException) {
            TokenValidationResult.Invalid
        } catch (e: MalformedJwtException) {
            TokenValidationResult.Invalid
        } catch (e: Exception) {
            TokenValidationResult.Invalid
        }
    }

    fun extractUserId(token: String): Long? {
        return try {
            parser.parseSignedClaims(token).payload.subject.toLong()
        } catch (e: Exception) {
            null
        }
    }

    fun extractClaims(token: String): Claims? {
        return try {
            parser.parseSignedClaims(token).payload
        } catch (e: Exception) {
            null
        }
    }

    fun getTokenExpiration(token: String): Instant? {
        return try {
            parser.parseSignedClaims(token).payload.expiration.toInstant()
        } catch (e: Exception) {
            null
        }
    }
}

sealed class TokenValidationResult {
    data class Valid(
        val userId: Long,
        val expiresAt: Instant,
        val claims: Claims
    ) : TokenValidationResult()

    data object Expired : TokenValidationResult()
    data object Invalid : TokenValidationResult()
}
