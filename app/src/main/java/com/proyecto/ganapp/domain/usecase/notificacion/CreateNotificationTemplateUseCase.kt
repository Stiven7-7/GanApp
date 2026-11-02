package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.PlantillaNotificacion
import com.proyecto.ganapp.domain.repository.NotificationTemplateRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first

class CreateNotificationTemplateUseCase(
    private val repository: NotificationTemplateRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(request: CreateNotificationTemplateRequest): CreateNotificationTemplateResult {
        return try {
            val userId = sessionRepository.observeAuthenticatedUserId().first()
            if (userId == null || userId <= 0L) {
                return CreateNotificationTemplateResult.NoAuthenticatedUser
            }
            val validation = NotificationTemplateValidator.validate(request)
            if (validation is TemplateValidationResult.Invalid) {
                return CreateNotificationTemplateResult.InvalidInput(validation.error)
            }
            val title = (validation as TemplateValidationResult.Valid).titulo
            val id = repository.insertTemplate(
                PlantillaNotificacion(titulo = title, tipo = request.tipo, idUsuario = userId),
            )
            if (id > 0L) {
                CreateNotificationTemplateResult.Success(id)
            } else {
                CreateNotificationTemplateResult.Failure(IllegalStateException("Invalid inserted template ID: $id"))
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            CreateNotificationTemplateResult.Failure(exception)
        }
    }
}
