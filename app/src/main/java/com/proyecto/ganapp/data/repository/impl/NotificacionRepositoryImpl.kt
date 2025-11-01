package com.proyecto.ganapp.data.repository.impl

import com.proyecto.ganapp.data.local.dao.NotificacionDao
import com.proyecto.ganapp.data.mapper.toDomain
import com.proyecto.ganapp.data.mapper.toEntity
import com.proyecto.ganapp.domain.model.Notificacion
import com.proyecto.ganapp.domain.repository.NotificacionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NotificacionRepositoryImpl @Inject constructor(
    private val dao: NotificacionDao
) : NotificacionRepository {

    override fun getAllNotifications(userId: Long): Flow<List<Notificacion>> {
        return dao.getAll().map { list -> list.filter { it.idUsuario == userId }.map { it.toDomain() } }
    }

    override suspend fun insertNotification(notificacion: Notificacion) {
        dao.insert(notificacion.toEntity())
    }

    override suspend fun deleteNotification(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun getNotificationById(id: Long): Notificacion? {
        return dao.getById(id)?.toDomain()
    }
}
