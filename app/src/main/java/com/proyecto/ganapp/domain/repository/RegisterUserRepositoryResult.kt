package com.proyecto.ganapp.domain.repository

/**
 * Resultado tipado de persistencia de registro.
 * No incluye Usuario, contraseña, hash ni excepciones.
 */
sealed interface RegisterUserRepositoryResult {
    data class Success(val userId: Long) : RegisterUserRepositoryResult
    data object DuplicateEmail : RegisterUserRepositoryResult
}
