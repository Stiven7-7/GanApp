package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.NotificationCancelResult
import com.proyecto.ganapp.domain.model.NotificationScheduleResult
import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import com.proyecto.ganapp.domain.repository.NotificacionRepository
import com.proyecto.ganapp.domain.service.NotificationRecurrenceEngine
import java.time.ZoneId
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class HandleNotificationAlarmUseCase @Inject constructor(
    private val repository: NotificacionRepository,
    private val scheduleNotificationUseCase: ScheduleNotificationUseCase,
) {
    suspend operator fun invoke(
        notificationId: Long,
        firedAtMillis: Long,
        zoneId: ZoneId,
    ): HandleNotificationAlarmResult {
        if (notificationId <= 0L) {
            return HandleNotificationAlarmResult.InvalidNotificationId
        }
        return try {
            reschedule(notificationId, firedAtMillis, zoneId)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            HandleNotificationAlarmResult.Failure(
                notificationId = notificationId,
                cause = exception,
            )
        }
    }

    private suspend fun reschedule(
        notificationId: Long,
        firedAtMillis: Long,
        zoneId: ZoneId,
    ): HandleNotificationAlarmResult {
        val notification = repository.getNotificationById(notificationId)
            ?: return mapCancel(
                notificationId = notificationId,
                cancelResult = scheduleNotificationUseCase.cancel(notificationId),
                whenCancelled = HandleNotificationAlarmResult.NotificationNotFound(notificationId),
            )

        if (notification.idNotificacion != notificationId) {
            return HandleNotificationAlarmResult.Failure(
                notificationId = notificationId,
                cause = null,
            )
        }

        val recurrence = RecurrenciaNotificacion.fromPersistentString(notification.seRepite)
            ?: return HandleNotificationAlarmResult.InvalidRecurrence(notificationId)

        val nextOccurrence = NotificationRecurrenceEngine.nextOccurrence(
            recurrence = recurrence,
            fechaInicio = notification.fechaInicio,
            fechaFin = notification.fechaFin,
            hora = notification.hora,
            afterMillis = firedAtMillis,
            zoneId = zoneId,
        )

        if (nextOccurrence == null) {
            return mapCancel(
                notificationId = notificationId,
                cancelResult = scheduleNotificationUseCase.cancel(notificationId),
                whenCancelled = HandleNotificationAlarmResult.Completed(notificationId),
            )
        }

        val scheduled = scheduleNotificationUseCase.scheduleNext(
            notificationId = notification.idNotificacion,
            nextOccurrenceAtMillis = nextOccurrence,
            title = TITLE,
            message = "Recordatorio pendiente: ${notification.nombre.trim()}",
            now = firedAtMillis,
        )
        return when (scheduled) {
            is NotificationScheduleResult.Scheduled ->
                HandleNotificationAlarmResult.Rescheduled(
                    notificationId = notificationId,
                    nextOccurrenceAtMillis = nextOccurrence,
                )
            NotificationScheduleResult.InvalidNotificationId,
            NotificationScheduleResult.InvalidTriggerTime,
            is NotificationScheduleResult.NoNextOccurrence,
            -> HandleNotificationAlarmResult.Failure(
                notificationId = notificationId,
                cause = null,
            )
            is NotificationScheduleResult.Failure ->
                HandleNotificationAlarmResult.Failure(
                    notificationId = notificationId,
                    cause = scheduled.cause,
                )
        }
    }

    private fun mapCancel(
        notificationId: Long,
        cancelResult: NotificationCancelResult,
        whenCancelled: HandleNotificationAlarmResult,
    ): HandleNotificationAlarmResult {
        return when (cancelResult) {
            is NotificationCancelResult.Cancelled -> whenCancelled
            is NotificationCancelResult.Failure ->
                HandleNotificationAlarmResult.Failure(
                    notificationId = notificationId,
                    cause = cancelResult.cause,
                )
            NotificationCancelResult.InvalidNotificationId ->
                HandleNotificationAlarmResult.Failure(
                    notificationId = notificationId,
                    cause = null,
                )
        }
    }

    private companion object {
        const val TITLE = "Recordatorio GanApp"
    }
}
