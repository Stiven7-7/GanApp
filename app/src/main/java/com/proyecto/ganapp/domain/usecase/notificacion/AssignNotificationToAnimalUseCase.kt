package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.repository.AnimalRepository
import com.proyecto.ganapp.domain.repository.NotificacionRepository
import com.proyecto.ganapp.domain.repository.NotificacionxAnimalRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class AssignNotificationToAnimalUseCase @Inject constructor(
    private val animalRepository: AnimalRepository,
    private val notificationRepository: NotificacionRepository,
    private val linkRepository: NotificacionxAnimalRepository,
) {

    suspend operator fun invoke(
        userId: Long,
        animalId: Long,
        notificationId: Long,
    ): AssignNotificationToAnimalResult {
        if (userId <= 0L || animalId <= 0L || notificationId <= 0L) {
            return AssignNotificationToAnimalResult.InvalidInput
        }
        return try {
            val animal = animalRepository.getAnimalById(animalId)
                ?: return AssignNotificationToAnimalResult.AnimalNotFound
            val notification = notificationRepository.getNotificationById(notificationId)
                ?: return AssignNotificationToAnimalResult.NotificationNotFound
            if (animal.idUsuario != userId || notification.idUsuario != userId) {
                return AssignNotificationToAnimalResult.OwnershipMismatch
            }
            linkRepository.assignNotificationToAnimal(animalId, notificationId)
            AssignNotificationToAnimalResult.Success
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            AssignNotificationToAnimalResult.Failure(exception)
        }
    }
}
