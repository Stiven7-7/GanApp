package com.proyecto.ganapp.domain.usecase.notificacion

sealed interface UpdateNotificationTemplateResult {
    data object Success : UpdateNotificationTemplateResult
    data class InvalidInput(val error: TemplateValidationError) : UpdateNotificationTemplateResult
    data object NoAuthenticatedUser : UpdateNotificationTemplateResult
    data object TemplateNotFound : UpdateNotificationTemplateResult
    data object OwnershipMismatch : UpdateNotificationTemplateResult
    data class Failure(val cause: Throwable? = null) : UpdateNotificationTemplateResult
}
