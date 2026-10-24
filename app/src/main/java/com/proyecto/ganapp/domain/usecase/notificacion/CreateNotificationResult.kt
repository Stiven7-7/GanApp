package com.proyecto.ganapp.domain.usecase.notificacion

sealed interface CreateNotificationResult {
    data class Success(
        val notificationId: Long,
        val firstOccurrenceAtMillis: Long,
    ) : CreateNotificationResult

    data class ValidationFailure(
        val errors: Set<CreateNotificationValidationError>,
    ) : CreateNotificationResult

    data class PersistenceFailure(
        val cause: Throwable? = null,
    ) : CreateNotificationResult

    data class SchedulingFailure(
        val notificationId: Long,
        val cause: Throwable? = null,
    ) : CreateNotificationResult
}
