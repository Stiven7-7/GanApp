package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.PlantillaNotificacion

sealed interface GetNotificationTemplateByIdResult {
    data class Success(val template: PlantillaNotificacion) : GetNotificationTemplateByIdResult
    data object InvalidTemplateId : GetNotificationTemplateByIdResult
    data object NoAuthenticatedUser : GetNotificationTemplateByIdResult
    data object TemplateNotFound : GetNotificationTemplateByIdResult
    data object OwnershipMismatch : GetNotificationTemplateByIdResult
    data class Failure(val cause: Throwable? = null) : GetNotificationTemplateByIdResult
}
