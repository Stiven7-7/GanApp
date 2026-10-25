package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.repository.NotificacionRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class CreateNotificacionUseCase @Inject constructor(
    private val repository: NotificacionRepository
) {
    suspend operator fun invoke(
        request: CreateNotificationRequest,
        now: Long,
    ): CreateNotificationPersistenceResult {
        val errors = CreateNotificationValidator.validate(request, now)
        if (errors.isNotEmpty()) {
            return CreateNotificationPersistenceResult.ValidationFailure(errors)
        }

        return try {
            val notificationId = repository.insertNotification(
                CreateNotificationMapper.toNotificacion(request),
            )
            if (notificationId > 0L) {
                CreateNotificationPersistenceResult.Persisted(notificationId)
            } else {
                CreateNotificationPersistenceResult.PersistenceFailure()
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            CreateNotificationPersistenceResult.PersistenceFailure(exception)
        }
    }
}
