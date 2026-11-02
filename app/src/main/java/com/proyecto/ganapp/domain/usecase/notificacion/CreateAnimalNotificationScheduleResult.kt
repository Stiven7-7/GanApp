package com.proyecto.ganapp.domain.usecase.notificacion

sealed interface CreateAnimalNotificationScheduleResult {
    data class Success(val scheduleId: Long) : CreateAnimalNotificationScheduleResult
    data object NoAuthenticatedUser : CreateAnimalNotificationScheduleResult
    data class InvalidInput(val error: ScheduleValidationError) : CreateAnimalNotificationScheduleResult
    data object TemplateNotFound : CreateAnimalNotificationScheduleResult
    data object AnimalNotFound : CreateAnimalNotificationScheduleResult
    data object OwnershipMismatch : CreateAnimalNotificationScheduleResult
    data class SchedulingFailure(val scheduleId: Long, val cause: Throwable? = null) : CreateAnimalNotificationScheduleResult
    data class Failure(val cause: Throwable? = null) : CreateAnimalNotificationScheduleResult
}
