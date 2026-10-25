package com.proyecto.ganapp.data.repository.impl


import com.proyecto.ganapp.data.local.dao.NotificacionxAnimalDao
import com.proyecto.ganapp.data.local.entity.NotificacionxAnimalEntity
import com.proyecto.ganapp.domain.repository.NotificacionxAnimalRepository
import javax.inject.Inject

class NotificacionxAnimalRepositoryImpl @Inject constructor(
    private val dao: NotificacionxAnimalDao
) : NotificacionxAnimalRepository {

    override suspend fun assignNotificationToAnimal(idAnimal: Long, idNotificacion: Long) {
        dao.insert(NotificacionxAnimalEntity(idAnimal, idNotificacion))
    }

    override suspend fun removeAssignment(idAnimal: Long, idNotificacion: Long) {
        dao.delete(idAnimal, idNotificacion)
    }

    override suspend fun getNotificationIdsOfAnimal(idAnimal: Long): List<Long> =
        dao.getNotificacionesByAnimal(idAnimal).map { entity -> entity.idNotificacion }

    override suspend fun getAnimalIdsWithNotification(idNotificacion: Long): List<Long> =
        dao.getAnimalesByNotificacion(idNotificacion).map { entity -> entity.idAnimal }
}
