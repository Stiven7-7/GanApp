package com.proyecto.ganapp.domain.repository

import com.proyecto.ganapp.domain.model.Notificacion
import kotlinx.coroutines.flow.Flow

interface NotificacionRepository {
    fun getAllNotifications(userId: Long): Flow<List<Notificacion>>
    suspend fun insertNotification(notificacion: Notificacion)
    suspend fun updateNotification(notificacion: Notificacion)
    suspend fun deleteNotification(id: Long)
    suspend fun getNotificationById(id: Long): Notificacion?
}

