package com.proyecto.ganapp.ui.features.auth

import com.proyecto.ganapp.domain.usecase.usuario.LoginValidationError

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: LoginValidationError? = null,
    val passwordError: LoginValidationError? = null,
    val generalError: LoginGeneralError? = null,
    val isLoading: Boolean = false,
) {
    val canSubmit: Boolean
        get() = !isLoading
}

enum class LoginGeneralError {
    INVALID_CREDENTIALS,
    UNEXPECTED,
}
