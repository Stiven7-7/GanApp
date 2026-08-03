package com.proyecto.ganapp.domain.model

/**
 * Usuario autenticado sin secretos. No incluye contraseña ni hash.
 */
data class AuthenticatedUser(
    val idUsuario: Long,
    val nombre: String,
    val apellido: String,
    val correo: String,
)
