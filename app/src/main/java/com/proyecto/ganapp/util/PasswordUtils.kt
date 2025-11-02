package com.proyecto.ganapp.util

import at.favre.lib.crypto.bcrypt.BCrypt

object PasswordUtils {

    // Genera hash (cost = 12)
    fun hash(password: String, cost: Int = 12): String {
        return BCrypt.withDefaults().hashToString(cost, password.toCharArray())
    }

    // Verifica password plain vs hash almacenado
    fun verify(password: String, hashed: String): Boolean {
        val result = BCrypt.verifyer().verify(password.toCharArray(), hashed)
        return result.verified
    }
}
