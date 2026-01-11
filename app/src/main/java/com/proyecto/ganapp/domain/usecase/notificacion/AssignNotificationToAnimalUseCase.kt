package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.repository.NotificacionRepository
import com.proyecto.ganapp.domain.repository.NotificacionxAnimalRepository
import javax.inject.Inject

class AssignNotificationToAnimalUseCase @Inject constructor(
    private val notificationRepo: NotificacionRepository,
    private val linkRepo: NotificacionxAnimalRepository
) {

    suspend operator fun invoke(idAnimal: Long, idNotificacion: Long) {
        // verificar que la notificación exista
        val noti = notificationRepo.getNotificationById(idNotificacion)
            ?: throw IllegalArgumentException("Notificación no encontrada")

        // asignar
        linkRepo.assignNotificationToAnimal(idAnimal, idNotificacion)
    }
}
