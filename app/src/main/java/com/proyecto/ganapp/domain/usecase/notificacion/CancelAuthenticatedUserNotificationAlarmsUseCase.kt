package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.NotificationCancelResult
import com.proyecto.ganapp.domain.repository.NotificacionRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first

class CancelAuthenticatedUserNotificationAlarmsUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val notificationRepository: NotificacionRepository,
    private val scheduleNotificationUseCase: ScheduleNotificationUseCase,
) {
    suspend operator fun invoke(): CancelAuthenticatedUserNotificationAlarmsResult {
        val userId = try {
            sessionRepository.observeAuthenticatedUserId().first()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            return CancelAuthenticatedUserNotificationAlarmsResult.Failure(exception)
        }
        if (userId == null || userId <= 0L) {
            return CancelAuthenticatedUserNotificationAlarmsResult.NoAuthenticatedUser
        }

        val notifications = try {
            notificationRepository.getAllNotifications(userId).first()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            return CancelAuthenticatedUserNotificationAlarmsResult.Failure(exception)
        }

        var cancelled = 0
        var skippedInvalid = 0
        var failed = 0
        for (notification in notifications) {
            if (notification.idNotificacion <= 0L) {
                skippedInvalid += 1
                continue
            }
            val cancelResult = try {
                scheduleNotificationUseCase.cancel(notification.idNotificacion)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                failed += 1
                continue
            }
            when (cancelResult) {
                is NotificationCancelResult.Cancelled -> cancelled += 1
                is NotificationCancelResult.Failure -> failed += 1
                NotificationCancelResult.InvalidNotificationId -> failed += 1
            }
        }
        return CancelAuthenticatedUserNotificationAlarmsResult.Cancelled(
            userId = userId,
            total = notifications.size,
            cancelled = cancelled,
            skippedInvalid = skippedInvalid,
            failed = failed,
        )
    }
}
