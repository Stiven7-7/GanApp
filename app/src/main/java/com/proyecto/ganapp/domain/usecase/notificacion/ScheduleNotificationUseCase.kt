package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.NotificationAlarmRequest
import com.proyecto.ganapp.domain.model.NotificationCancelResult
import com.proyecto.ganapp.domain.model.NotificationScheduleResult
import com.proyecto.ganapp.domain.service.NotificationAlarmScheduler

class ScheduleNotificationUseCase(
    private val scheduler: NotificationAlarmScheduler,
) {
    fun scheduleNext(
        notificationId: Long,
        nextOccurrenceAtMillis: Long?,
        title: String,
        message: String,
        now: Long,
    ): NotificationScheduleResult {
        if (notificationId <= 0L) {
            return NotificationScheduleResult.InvalidNotificationId
        }
        if (nextOccurrenceAtMillis == null) {
            return when (val cancelResult = scheduler.cancel(notificationId)) {
                is NotificationCancelResult.Cancelled ->
                    NotificationScheduleResult.NoNextOccurrence(notificationId)
                is NotificationCancelResult.Failure ->
                    NotificationScheduleResult.Failure(
                        notificationId = cancelResult.notificationId,
                        cause = cancelResult.cause,
                    )
                NotificationCancelResult.InvalidNotificationId ->
                    NotificationScheduleResult.InvalidNotificationId
            }
        }
        if (nextOccurrenceAtMillis <= now) {
            return NotificationScheduleResult.InvalidTriggerTime
        }
        return scheduler.scheduleNext(
            NotificationAlarmRequest(
                notificationId = notificationId,
                triggerAtMillis = nextOccurrenceAtMillis,
                title = title,
                message = message,
            ),
        )
    }

    fun cancel(notificationId: Long): NotificationCancelResult {
        if (notificationId <= 0L) {
            return NotificationCancelResult.InvalidNotificationId
        }
        return scheduler.cancel(notificationId)
    }
}
