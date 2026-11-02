package com.proyecto.ganapp.domain.usecase.notificacion

sealed interface DeleteNotificationScheduleResult {
    data object Success : DeleteNotificationScheduleResult
    data object NoAuthenticatedUser : DeleteNotificationScheduleResult
    data object InvalidScheduleId : DeleteNotificationScheduleResult
    data object ScheduleNotFound : DeleteNotificationScheduleResult
    data object TemplateNotFound : DeleteNotificationScheduleResult
    data object AnimalNotFound : DeleteNotificationScheduleResult
    data object OwnershipMismatch : DeleteNotificationScheduleResult
    data class AlarmCancellationFailed(val cause: Throwable? = null) : DeleteNotificationScheduleResult
    data class PersistenceFailure(val cause: Throwable? = null) : DeleteNotificationScheduleResult
    data class Failure(val cause: Throwable? = null) : DeleteNotificationScheduleResult
}
