package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.repository.NotificationTemplateRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first

class GetNotificationTemplateByIdUseCase(
    private val repository: NotificationTemplateRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(templateId: Long): GetNotificationTemplateByIdResult {
        if (templateId <= 0L) return GetNotificationTemplateByIdResult.InvalidTemplateId
        return try {
            val userId = sessionRepository.observeAuthenticatedUserId().first()
            if (userId == null || userId <= 0L) {
                return GetNotificationTemplateByIdResult.NoAuthenticatedUser
            }
            val template = repository.getTemplateById(templateId)
                ?: return GetNotificationTemplateByIdResult.TemplateNotFound
            if (template.idUsuario != userId) {
                return GetNotificationTemplateByIdResult.OwnershipMismatch
            }
            GetNotificationTemplateByIdResult.Success(template)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            GetNotificationTemplateByIdResult.Failure(exception)
        }
    }
}
