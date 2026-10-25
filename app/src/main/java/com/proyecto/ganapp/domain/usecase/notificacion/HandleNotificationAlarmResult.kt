package com.proyecto.ganapp.domain.usecase.notificacion

sealed interface HandleNotificationAlarmResult {
    data class Rescheduled(
        val notificationId: Long,
        val nextOccurrenceAtMillis: Long,
    ) : HandleNotificationAlarmResult

    data class Completed(
        val notificationId: Long,
    ) : HandleNotificationAlarmResult

    data class NotificationNotFound(
        val notificationId: Long,
    ) : HandleNotificationAlarmResult

    data object InvalidNotificationId : HandleNotificationAlarmResult

    data class InvalidRecurrence(
        val notificationId: Long,
    ) : HandleNotificationAlarmResult

    data class Failure(
        val notificationId: Long,
        val cause: Throwable? = null,
    ) : HandleNotificationAlarmResult
}
