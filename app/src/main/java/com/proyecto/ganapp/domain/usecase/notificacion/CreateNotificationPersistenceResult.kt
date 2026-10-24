package com.proyecto.ganapp.domain.usecase.notificacion

sealed interface CreateNotificationPersistenceResult {
    data class Persisted(
        val notificationId: Long,
    ) : CreateNotificationPersistenceResult

    data class ValidationFailure(
        val errors: Set<CreateNotificationValidationError>,
    ) : CreateNotificationPersistenceResult

    data class PersistenceFailure(
        val cause: Throwable? = null,
    ) : CreateNotificationPersistenceResult
}
