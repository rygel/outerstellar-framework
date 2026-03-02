package com.outerstellar.auth

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class PasswordService(private val strength: Int = 12) {
    private val encoder = BCryptPasswordEncoder(strength)

    fun hash(password: String): String = encoder.encode(password)

    fun verify(password: String, hash: String): Boolean = encoder.matches(password, hash)

    fun needsRehash(hash: String): Boolean = encoder.upgradeEncoding(hash)
}
