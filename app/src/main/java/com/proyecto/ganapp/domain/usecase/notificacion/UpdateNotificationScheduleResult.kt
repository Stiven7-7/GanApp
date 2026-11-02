package com.proyecto.ganapp.domain.usecase.notificacion

sealed interface UpdateNotificationScheduleResult {
    data object Success : UpdateNotificationScheduleResult
    data object NoAuthenticatedUser : UpdateNotificationScheduleResult
    data class InvalidInput(val error: ScheduleValidationError) : UpdateNotificationScheduleResult
    data object ScheduleNotFound : UpdateNotificationScheduleResult
    data object TemplateNotFound : UpdateNotificationScheduleResult
    data object AnimalNotFound : UpdateNotificationScheduleResult
    data object OwnershipMismatch : UpdateNotificationScheduleResult
    data class SchedulingFailure(val scheduleId: Long, val cause: Throwable? = null) : UpdateNotificationScheduleResult
    data class Failure(val cause: Throwable? = null) : UpdateNotificationScheduleResult
}
