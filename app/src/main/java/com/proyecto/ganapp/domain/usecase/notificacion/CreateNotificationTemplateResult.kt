package com.proyecto.ganapp.domain.usecase.notificacion

sealed interface CreateNotificationTemplateResult {
    data class Success(val templateId: Long) : CreateNotificationTemplateResult
    data object NoAuthenticatedUser : CreateNotificationTemplateResult
    data class InvalidInput(val error: TemplateValidationError) : CreateNotificationTemplateResult
    data class Failure(val cause: Throwable? = null) : CreateNotificationTemplateResult
}
