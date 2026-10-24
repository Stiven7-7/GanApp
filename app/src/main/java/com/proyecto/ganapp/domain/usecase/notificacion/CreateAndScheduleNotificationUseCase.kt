package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.NotificationScheduleResult
import javax.inject.Inject

class CreateAndScheduleNotificationUseCase @Inject constructor(
    private val createNotificacionUseCase: CreateNotificacionUseCase,
    private val scheduleNotificationUseCase: ScheduleNotificationUseCase,
) {
    suspend operator fun invoke(
        request: CreateNotificationRequest,
        now: Long,
    ): CreateNotificationResult {
        val persisted = when (val persistence = createNotificacionUseCase(request, now)) {
            is CreateNotificationPersistenceResult.ValidationFailure ->
                return CreateNotificationResult.ValidationFailure(persistence.errors)
            is CreateNotificationPersistenceResult.PersistenceFailure ->
                return CreateNotificationResult.PersistenceFailure(persistence.cause)
            is CreateNotificationPersistenceResult.Persisted -> persistence.notificationId
        }

        val firstOccurrence = CreateNotificationValidator.firstOccurrence(request)
            ?: return CreateNotificationResult.SchedulingFailure(
                notificationId = persisted,
                cause = null,
            )

        val scheduled = scheduleNotificationUseCase.scheduleNext(
            notificationId = persisted,
            nextOccurrenceAtMillis = firstOccurrence,
            title = TITLE,
            message = "Recordatorio pendiente: ${request.nombre.trim()}",
            now = now,
        )
        return when (scheduled) {
            is NotificationScheduleResult.Scheduled ->
                CreateNotificationResult.Success(
                    notificationId = persisted,
                    firstOccurrenceAtMillis = firstOccurrence,
                )
            NotificationScheduleResult.InvalidNotificationId,
            NotificationScheduleResult.InvalidTriggerTime,
            is NotificationScheduleResult.NoNextOccurrence,
            -> CreateNotificationResult.SchedulingFailure(
                notificationId = persisted,
                cause = null,
            )
            is NotificationScheduleResult.Failure ->
                CreateNotificationResult.SchedulingFailure(
                    notificationId = persisted,
                    cause = scheduled.cause,
                )
        }
    }

    private companion object {
        const val TITLE = "Recordatorio GanApp"
    }
}
