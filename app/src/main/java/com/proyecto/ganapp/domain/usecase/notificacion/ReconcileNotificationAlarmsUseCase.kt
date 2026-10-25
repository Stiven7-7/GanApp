package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.Notificacion
import com.proyecto.ganapp.domain.model.NotificationCancelResult
import com.proyecto.ganapp.domain.model.NotificationScheduleResult
import com.proyecto.ganapp.domain.model.RecurrenciaNotificacion
import com.proyecto.ganapp.domain.repository.NotificacionRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import com.proyecto.ganapp.domain.service.NotificationRecurrenceEngine
import java.time.ZoneId
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first

class ReconcileNotificationAlarmsUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val notificationRepository: NotificacionRepository,
    private val scheduleNotificationUseCase: ScheduleNotificationUseCase,
) {
    suspend operator fun invoke(
        now: Long,
        zoneId: ZoneId,
    ): ReconcileNotificationAlarmsResult {
        val userId = try {
            sessionRepository.observeAuthenticatedUserId().first()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            return ReconcileNotificationAlarmsResult.Failure(exception)
        }
        if (userId == null || userId <= 0L) {
            return ReconcileNotificationAlarmsResult.NoAuthenticatedUser
        }

        val notifications = try {
            notificationRepository.getAllNotifications(userId).first()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            return ReconcileNotificationAlarmsResult.Failure(exception)
        }

        var scheduled = 0
        var completed = 0
        var skippedInvalid = 0
        var failed = 0
        for (notification in notifications) {
            val outcome = try {
                reconcileOne(notification, now, zoneId)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                ItemOutcome.FAILED
            }
            when (outcome) {
                ItemOutcome.SCHEDULED -> scheduled += 1
                ItemOutcome.COMPLETED -> completed += 1
                ItemOutcome.SKIPPED -> skippedInvalid += 1
                ItemOutcome.FAILED -> failed += 1
            }
        }
        return ReconcileNotificationAlarmsResult.Reconciled(
            userId = userId,
            total = notifications.size,
            scheduled = scheduled,
            completed = completed,
            skippedInvalid = skippedInvalid,
            failed = failed,
        )
    }

    private fun reconcileOne(
        notification: Notificacion,
        now: Long,
        zoneId: ZoneId,
    ): ItemOutcome {
        if (notification.idNotificacion <= 0L) {
            return ItemOutcome.SKIPPED
        }
        val recurrence = RecurrenciaNotificacion.fromPersistentString(notification.seRepite)
            ?: return ItemOutcome.SKIPPED
        val nextOccurrence = NotificationRecurrenceEngine.nextOccurrence(
            recurrence = recurrence,
            fechaInicio = notification.fechaInicio,
            fechaFin = notification.fechaFin,
            hora = notification.hora,
            afterMillis = now,
            zoneId = zoneId,
        )
        if (nextOccurrence == null) {
            return when (scheduleNotificationUseCase.cancel(notification.idNotificacion)) {
                is NotificationCancelResult.Cancelled -> ItemOutcome.COMPLETED
                is NotificationCancelResult.Failure -> ItemOutcome.FAILED
                NotificationCancelResult.InvalidNotificationId -> ItemOutcome.FAILED
            }
        }
        val scheduled = scheduleNotificationUseCase.scheduleNext(
            notificationId = notification.idNotificacion,
            nextOccurrenceAtMillis = nextOccurrence,
            title = TITLE,
            message = "Recordatorio pendiente: ${notification.nombre.trim()}",
            now = now,
        )
        return when (scheduled) {
            is NotificationScheduleResult.Scheduled -> ItemOutcome.SCHEDULED
            is NotificationScheduleResult.Failure,
            NotificationScheduleResult.InvalidNotificationId,
            NotificationScheduleResult.InvalidTriggerTime,
            is NotificationScheduleResult.NoNextOccurrence,
            -> ItemOutcome.FAILED
        }
    }

    private enum class ItemOutcome {
        SCHEDULED,
        COMPLETED,
        SKIPPED,
        FAILED,
    }

    private companion object {
        const val TITLE = "Recordatorio GanApp"
    }
}
