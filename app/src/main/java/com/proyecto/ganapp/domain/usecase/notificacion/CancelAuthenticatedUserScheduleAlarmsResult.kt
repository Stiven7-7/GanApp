package com.proyecto.ganapp.domain.usecase.notificacion

sealed interface CancelAuthenticatedUserScheduleAlarmsResult {
    data object Cancelled : CancelAuthenticatedUserScheduleAlarmsResult
    data object NoAuthenticatedUser : CancelAuthenticatedUserScheduleAlarmsResult
    data class Failure(val cause: Throwable? = null) : CancelAuthenticatedUserScheduleAlarmsResult
}
