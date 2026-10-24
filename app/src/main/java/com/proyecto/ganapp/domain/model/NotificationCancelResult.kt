package com.proyecto.ganapp.domain.model

sealed interface NotificationCancelResult {
    data class Cancelled(
        val notificationId: Long,
    ) : NotificationCancelResult

    data object InvalidNotificationId : NotificationCancelResult

    data class Failure(
        val notificationId: Long,
        val cause: Throwable,
    ) : NotificationCancelResult
}
