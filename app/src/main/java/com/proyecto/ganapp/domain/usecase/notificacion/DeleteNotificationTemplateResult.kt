package com.proyecto.ganapp.domain.usecase.notificacion

sealed interface DeleteNotificationTemplateResult {
    data object Success : DeleteNotificationTemplateResult
    data object InvalidTemplateId : DeleteNotificationTemplateResult
    data object NoAuthenticatedUser : DeleteNotificationTemplateResult
    data object TemplateNotFound : DeleteNotificationTemplateResult
    data object OwnershipMismatch : DeleteNotificationTemplateResult
    data object InUse : DeleteNotificationTemplateResult
    data class Failure(val cause: Throwable? = null) : DeleteNotificationTemplateResult
}
