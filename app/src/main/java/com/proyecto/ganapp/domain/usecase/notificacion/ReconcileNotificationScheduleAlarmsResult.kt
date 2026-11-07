package com.proyecto.ganapp.domain.usecase.notificacion

sealed interface ReconcileNotificationScheduleAlarmsResult {
    data class Reconciled(
        val scheduled: Int,
        val failed: Int,
    ) : ReconcileNotificationScheduleAlarmsResult

    data object NoAuthenticatedUser : ReconcileNotificationScheduleAlarmsResult

    data class Failure(
        val cause: Throwable,
    ) : ReconcileNotificationScheduleAlarmsResult
}
