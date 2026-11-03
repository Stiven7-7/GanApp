package com.proyecto.ganapp.domain.usecase.animal

sealed interface DeleteAnimalWithNotificationSchedulesResult {
    data object Success : DeleteAnimalWithNotificationSchedulesResult
    data object NoAuthenticatedUser : DeleteAnimalWithNotificationSchedulesResult
    data object InvalidAnimalId : DeleteAnimalWithNotificationSchedulesResult
    data object AnimalNotFound : DeleteAnimalWithNotificationSchedulesResult
    data object OwnershipMismatch : DeleteAnimalWithNotificationSchedulesResult
    data class AlarmCancellationFailed(
        val scheduleId: Long,
        val cause: Throwable? = null,
    ) : DeleteAnimalWithNotificationSchedulesResult
    data class PersistenceFailure(val cause: Throwable) : DeleteAnimalWithNotificationSchedulesResult
    data class Failure(val cause: Throwable) : DeleteAnimalWithNotificationSchedulesResult
}
