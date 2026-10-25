package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.Notificacion
import com.proyecto.ganapp.domain.repository.AnimalRepository
import com.proyecto.ganapp.domain.repository.NotificacionRepository
import com.proyecto.ganapp.domain.repository.NotificacionxAnimalRepository
import javax.inject.Inject

class GetNotificationsOfAnimalUseCase @Inject constructor(
    private val animalRepository: AnimalRepository,
    private val linkRepository: NotificacionxAnimalRepository,
    private val notificationRepository: NotificacionRepository,
) {

    suspend operator fun invoke(animalId: Long): List<Notificacion> {
        val animal = animalRepository.getAnimalById(animalId) ?: return emptyList()
        return linkRepository.getNotificationsOfAnimal(animalId).mapNotNull { link ->
            val notification = notificationRepository.getNotificationById(link.idNotificacion)
                ?: return@mapNotNull null
            if (notification.idUsuario == animal.idUsuario) notification else null
        }
    }
}
