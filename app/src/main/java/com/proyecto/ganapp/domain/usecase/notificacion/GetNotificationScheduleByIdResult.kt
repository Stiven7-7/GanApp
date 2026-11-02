package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.ProgramacionNotificacionDetalle

sealed interface GetNotificationScheduleByIdResult {
    data class Success(val detail: ProgramacionNotificacionDetalle) : GetNotificationScheduleByIdResult
    data object InvalidScheduleId : GetNotificationScheduleByIdResult
    data object NoAuthenticatedUser : GetNotificationScheduleByIdResult
    data object ScheduleNotFound : GetNotificationScheduleByIdResult
    data object TemplateNotFound : GetNotificationScheduleByIdResult
    data object AnimalNotFound : GetNotificationScheduleByIdResult
    data object OwnershipMismatch : GetNotificationScheduleByIdResult
    data class Failure(val cause: Throwable? = null) : GetNotificationScheduleByIdResult
}
