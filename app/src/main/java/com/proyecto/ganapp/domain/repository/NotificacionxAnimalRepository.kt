package com.proyecto.ganapp.domain.repository

interface NotificacionxAnimalRepository {

    suspend fun assignNotificationToAnimal(
        idAnimal: Long,
        idNotificacion: Long,
    )

    suspend fun removeAssignment(
        idAnimal: Long,
        idNotificacion: Long,
    )

    suspend fun getNotificationIdsOfAnimal(
        idAnimal: Long,
    ): List<Long>

    suspend fun getAnimalIdsWithNotification(
        idNotificacion: Long,
    ): List<Long>
}
