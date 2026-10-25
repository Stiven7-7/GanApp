package com.proyecto.ganapp.domain.usecase.notificacion

sealed interface CancelAuthenticatedUserNotificationAlarmsResult {

    data class Cancelled(
        val userId: Long,
        val total: Int,
        val cancelled: Int,
        val skippedInvalid: Int,
        val failed: Int,
    ) : CancelAuthenticatedUserNotificationAlarmsResult

    data object NoAuthenticatedUser :
        CancelAuthenticatedUserNotificationAlarmsResult

    data class Failure(
        val cause: Throwable? = null,
    ) : CancelAuthenticatedUserNotificationAlarmsResult
}
