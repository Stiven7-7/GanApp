package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.ProgramacionNotificacionDetalle

sealed interface GetAnimalNotificationSchedulesResult {
    data class Success(val schedules: List<ProgramacionNotificacionDetalle>) : GetAnimalNotificationSchedulesResult
    data object InvalidAnimalId : GetAnimalNotificationSchedulesResult
    data object NoAuthenticatedUser : GetAnimalNotificationSchedulesResult
    data object AnimalNotFound : GetAnimalNotificationSchedulesResult
    data object OwnershipMismatch : GetAnimalNotificationSchedulesResult
    data class Failure(val cause: Throwable? = null) : GetAnimalNotificationSchedulesResult
}
