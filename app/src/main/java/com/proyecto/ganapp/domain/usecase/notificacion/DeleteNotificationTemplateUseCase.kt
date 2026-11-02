package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.repository.NotificationTemplateRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first

class DeleteNotificationTemplateUseCase(
    private val repository: NotificationTemplateRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(templateId: Long): DeleteNotificationTemplateResult {
        if (templateId <= 0L) return DeleteNotificationTemplateResult.InvalidTemplateId
        return try {
            val userId = sessionRepository.observeAuthenticatedUserId().first()
            if (userId == null || userId <= 0L) {
                return DeleteNotificationTemplateResult.NoAuthenticatedUser
            }
            val template = repository.getTemplateById(templateId)
                ?: return DeleteNotificationTemplateResult.TemplateNotFound
            if (template.idUsuario != userId) {
                return DeleteNotificationTemplateResult.OwnershipMismatch
            }
            if (repository.hasSchedules(templateId)) {
                return DeleteNotificationTemplateResult.InUse
            }
            repository.deleteTemplate(templateId)
            DeleteNotificationTemplateResult.Success
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            DeleteNotificationTemplateResult.Failure(exception)
        }
    }
}
