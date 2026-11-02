package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.ProgramacionNotificacionDetalle
import com.proyecto.ganapp.domain.repository.AnimalRepository
import com.proyecto.ganapp.domain.repository.NotificationScheduleRepository
import com.proyecto.ganapp.domain.repository.NotificationTemplateRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first

class GetNotificationScheduleByIdUseCase(
    private val repository: NotificationScheduleRepository,
    private val templateRepository: NotificationTemplateRepository,
    private val animalRepository: AnimalRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(scheduleId: Long): GetNotificationScheduleByIdResult {
        if (scheduleId <= 0L) return GetNotificationScheduleByIdResult.InvalidScheduleId
        return try {
            val userId = sessionRepository.observeAuthenticatedUserId().first()
            if (userId == null || userId <= 0L) return GetNotificationScheduleByIdResult.NoAuthenticatedUser
            val schedule = repository.getScheduleById(scheduleId)
                ?: return GetNotificationScheduleByIdResult.ScheduleNotFound
            val template = templateRepository.getTemplateById(schedule.idPlantilla)
                ?: return GetNotificationScheduleByIdResult.TemplateNotFound
            if (template.idUsuario != userId) return GetNotificationScheduleByIdResult.OwnershipMismatch
            val animalId = schedule.idAnimal
            if (animalId != null) {
                val animal = animalRepository.getAnimalById(animalId)
                    ?: return GetNotificationScheduleByIdResult.AnimalNotFound
                if (animal.idUsuario != userId) return GetNotificationScheduleByIdResult.OwnershipMismatch
            }
            GetNotificationScheduleByIdResult.Success(ProgramacionNotificacionDetalle(schedule, template))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            GetNotificationScheduleByIdResult.Failure(exception)
        }
    }
}
