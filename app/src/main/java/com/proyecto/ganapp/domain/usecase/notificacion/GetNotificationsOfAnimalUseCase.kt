package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.repository.NotificacionRepository
import com.proyecto.ganapp.domain.repository.NotificacionxAnimalRepository
import com.proyecto.ganapp.domain.model.Notificacion
import javax.inject.Inject

class GetNotificationsOfAnimalUseCase @Inject constructor(
    private val linkRepo: NotificacionxAnimalRepository,
    private val notiRepo: NotificacionRepository
) {

    suspend operator fun invoke(animalId: Long): List<Notificacion> {
        val links = linkRepo.getNotificationsOfAnimal(animalId)

        return links.mapNotNull { link ->
            notiRepo.getNotificationById(link.idNotificacion)
        }
    }
}
