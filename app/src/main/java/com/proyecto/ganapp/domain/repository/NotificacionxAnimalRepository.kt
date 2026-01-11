package com.proyecto.ganapp.domain.repository

import com.proyecto.ganapp.data.local.entity.NotificacionxAnimalEntity

interface NotificacionxAnimalRepository {
    suspend fun assignNotificationToAnimal(idAnimal: Long, idNotificacion: Long)
    suspend fun removeAssignment(idAnimal: Long, idNotificacion: Long)
    suspend fun getNotificationsOfAnimal(idAnimal: Long): List<NotificacionxAnimalEntity>
    suspend fun getAnimalsWithNotification(idNotificacion: Long): List<NotificacionxAnimalEntity>
}
