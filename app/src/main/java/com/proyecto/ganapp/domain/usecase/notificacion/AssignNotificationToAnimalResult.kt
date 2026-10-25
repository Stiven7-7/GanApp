package com.proyecto.ganapp.domain.usecase.notificacion

sealed interface AssignNotificationToAnimalResult {

    data object Success : AssignNotificationToAnimalResult

    data object InvalidInput : AssignNotificationToAnimalResult

    data object AnimalNotFound : AssignNotificationToAnimalResult

    data object NotificationNotFound : AssignNotificationToAnimalResult

    data object OwnershipMismatch : AssignNotificationToAnimalResult

    data class Failure(
        val cause: Throwable? = null,
    ) : AssignNotificationToAnimalResult
}
