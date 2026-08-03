package com.proyecto.ganapp.domain.usecase.usuario

/**
 * Resultado tipado del registro. No incluye contraseñas ni excepciones crudas.
 */
sealed class RegisterUserResult {
    data class Success(val userId: Long) : RegisterUserResult()
    data class ValidationError(val errors: Set<RegisterValidationError>) : RegisterUserResult()
    data object UnexpectedError : RegisterUserResult()
}
