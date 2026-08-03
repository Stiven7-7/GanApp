package com.proyecto.ganapp.ui.features.auth

import com.proyecto.ganapp.domain.model.AuthenticatedUser
import com.proyecto.ganapp.domain.usecase.usuario.LoginValidationError

/**
 * Estado temporal de login en AuthViewModel (Subfase 1.2 / Bloque 3).
 * No es el UiState definitivo.
 */
sealed interface AuthLoginState {
    data object Idle : AuthLoginState
    data object Loading : AuthLoginState
    data class Success(val authenticatedUser: AuthenticatedUser) : AuthLoginState
    data class ValidationError(val errors: Set<LoginValidationError>) : AuthLoginState
    data object InvalidCredentials : AuthLoginState
    data object UnexpectedError : AuthLoginState
}
