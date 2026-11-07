package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.repository.NotificationTemplateRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class UpdateNotificationTemplateUseCase @Inject constructor(
    private val repository: NotificationTemplateRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(request: UpdateNotificationTemplateRequest): UpdateNotificationTemplateResult {
        val validation = NotificationTemplateValidator.validate(request)
        if (validation is TemplateValidationResult.Invalid) {
            return UpdateNotificationTemplateResult.InvalidInput(validation.error)
        }
        return try {
            val userId = sessionRepository.observeAuthenticatedUserId().first()
            if (userId == null || userId <= 0L) {
                return UpdateNotificationTemplateResult.NoAuthenticatedUser
            }
            val original = repository.getTemplateById(request.templateId)
                ?: return UpdateNotificationTemplateResult.TemplateNotFound
            if (original.idUsuario != userId) {
                return UpdateNotificationTemplateResult.OwnershipMismatch
            }
            val updated = original.copy(
                titulo = (validation as TemplateValidationResult.Valid).titulo,
                tipo = request.tipo,
            )
            if (repository.updateTemplate(updated)) {
                UpdateNotificationTemplateResult.Success
            } else {
                UpdateNotificationTemplateResult.Failure(IllegalStateException("Template update was not applied"))
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            UpdateNotificationTemplateResult.Failure(exception)
        }
    }
}
