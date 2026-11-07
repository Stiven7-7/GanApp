package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.ProgramacionNotificacionDetalle
import com.proyecto.ganapp.domain.repository.AnimalRepository
import com.proyecto.ganapp.domain.repository.NotificationScheduleRepository
import com.proyecto.ganapp.domain.repository.NotificationTemplateRepository
import com.proyecto.ganapp.domain.repository.SessionRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first

class GetAnimalNotificationSchedulesUseCase @Inject constructor(
    private val repository: NotificationScheduleRepository,
    private val templateRepository: NotificationTemplateRepository,
    private val animalRepository: AnimalRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(animalId: Long): GetAnimalNotificationSchedulesResult {
        if (animalId <= 0L) return GetAnimalNotificationSchedulesResult.InvalidAnimalId
        return try {
            val userId = sessionRepository.observeAuthenticatedUserId().first()
            if (userId == null || userId <= 0L) return GetAnimalNotificationSchedulesResult.NoAuthenticatedUser
            val animal = animalRepository.getAnimalById(animalId)
                ?: return GetAnimalNotificationSchedulesResult.AnimalNotFound
            if (animal.idUsuario != userId) return GetAnimalNotificationSchedulesResult.OwnershipMismatch
            val details = mutableListOf<ProgramacionNotificacionDetalle>()
            for (schedule in repository.getSchedulesByAnimal(animalId)) {
                // Omite filas inconsistentes; no repara persistencia ni expone datos ajenos.
                if (schedule.idAnimal != animalId) continue
                val template = templateRepository.getTemplateById(schedule.idPlantilla) ?: continue
                if (template.idUsuario != userId) continue
                details += ProgramacionNotificacionDetalle(schedule, template)
            }
            GetAnimalNotificationSchedulesResult.Success(details)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            GetAnimalNotificationSchedulesResult.Failure(exception)
        }
    }
}
