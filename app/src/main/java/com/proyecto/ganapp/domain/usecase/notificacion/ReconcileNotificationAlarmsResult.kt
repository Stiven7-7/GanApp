package com.proyecto.ganapp.domain.usecase.notificacion

sealed interface ReconcileNotificationAlarmsResult {
    data class Reconciled(
        val userId: Long,
        val total: Int,
        val scheduled: Int,
        val completed: Int,
        val skippedInvalid: Int,
        val failed: Int,
    ) : ReconcileNotificationAlarmsResult

    data object NoAuthenticatedUser : ReconcileNotificationAlarmsResult

    data class Failure(
        val cause: Throwable? = null,
    ) : ReconcileNotificationAlarmsResult
}
