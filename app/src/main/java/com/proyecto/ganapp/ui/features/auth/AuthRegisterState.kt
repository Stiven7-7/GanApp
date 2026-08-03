package com.proyecto.ganapp.ui.features.auth

import com.proyecto.ganapp.domain.usecase.usuario.RegisterValidationError

/**
 * Estado temporal de registro en AuthViewModel (Subfase 1.2 / Bloque 3).
 * No es el UiState definitivo.
 */
sealed interface AuthRegisterState {
    data object Idle : AuthRegisterState
    data object Loading : AuthRegisterState
    data class Success(val userId: Long) : AuthRegisterState
    data class ValidationError(val errors: Set<RegisterValidationError>) : AuthRegisterState
    data object UnexpectedError : AuthRegisterState
}
