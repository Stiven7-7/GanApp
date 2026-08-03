package com.proyecto.ganapp.domain.usecase.usuario

/**
 * Errores de validación de registro. Inmutables; pueden acumularse en un mismo resultado.
 */
enum class RegisterValidationError {
    EMPTY_NAME,
    EMPTY_LAST_NAME,
    EMPTY_EMAIL,
    INVALID_EMAIL_FORMAT,
    EMAIL_CONTAINS_WHITESPACE,
    EMPTY_PASSWORD,
    PASSWORD_TOO_SHORT,
    PASSWORD_TOO_LONG,
    EMPTY_CONFIRMATION,
    PASSWORD_MISMATCH,
}
