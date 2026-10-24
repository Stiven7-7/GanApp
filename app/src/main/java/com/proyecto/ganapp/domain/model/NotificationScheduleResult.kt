package com.proyecto.ganapp.domain.model

sealed interface NotificationScheduleResult {
    data class Scheduled(
        val notificationId: Long,
        val triggerAtMillis: Long,
    ) : NotificationScheduleResult

    data class NoNextOccurrence(
        val notificationId: Long,
    ) : NotificationScheduleResult

    data object InvalidNotificationId : NotificationScheduleResult

    data object InvalidTriggerTime : NotificationScheduleResult

    data class Failure(
        val notificationId: Long,
        val cause: Throwable,
    ) : NotificationScheduleResult
}
