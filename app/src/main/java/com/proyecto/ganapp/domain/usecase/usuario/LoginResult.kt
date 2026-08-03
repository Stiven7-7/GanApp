package com.proyecto.ganapp.domain.usecase.usuario

import com.proyecto.ganapp.domain.model.AuthenticatedUser

/**
 * Resultado tipado del login. No incluye contraseñas, hashes ni excepciones crudas.
 */
sealed class LoginResult {
    data class Success(val authenticatedUser: AuthenticatedUser) : LoginResult()
    data class ValidationError(val errors: Set<LoginValidationError>) : LoginResult()
    data object InvalidCredentials : LoginResult()
    data object UnexpectedError : LoginResult()
}
