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

    /** Obtener todas las notificaciones del usuario */
    override fun getAllNotifications(userId: Long): Flow<List<Notificacion>> {
        return dao.getAll().map { list ->
            list.filter { it.idUsuario == userId }.map { it.toDomain() }
        }
    }

    /** Insertar una notificación */
    override suspend fun insertNotification(notificacion: Notificacion) {
        dao.insert(notificacion.toEntity())
    }

    /** Actualizar una notificación */
    override suspend fun updateNotification(notificacion: Notificacion) {
        dao.update(notificacion.toEntity())
    }

    /** Eliminar una notificación */
    override suspend fun deleteNotification(id: Long) {
        dao.deleteById(id)
    }

    /** Obtener una notificación por ID */
    override suspend fun getNotificationById(id: Long): Notificacion? {
        return dao.getById(id)?.toDomain()
    }
}
