package com.proyecto.ganapp.ui.features.auth

import com.proyecto.ganapp.domain.usecase.usuario.RegisterValidationError

data class RegisterUiState(
    val name: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val passwordConfirmation: String = "",
    val nameError: RegisterValidationError? = null,
    val lastNameError: RegisterValidationError? = null,
    val emailError: RegisterValidationError? = null,
    val passwordError: RegisterValidationError? = null,
    val confirmationError: RegisterValidationError? = null,
    val generalError: RegisterGeneralError? = null,
    val isLoading: Boolean = false,
) {
    val canSubmit: Boolean
        get() = !isLoading
}

enum class RegisterGeneralError {
    DUPLICATE_EMAIL,
    UNEXPECTED,
}
