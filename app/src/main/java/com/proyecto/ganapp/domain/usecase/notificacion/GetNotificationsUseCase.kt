package com.proyecto.ganapp.domain.usecase.notificacion

import com.proyecto.ganapp.domain.model.Notificacion
import com.proyecto.ganapp.domain.repository.NotificacionRepository
import kotlinx.coroutines.flow.Flow

class GetNotificationsUseCase(
    private val repository: NotificacionRepository
) {
    operator fun invoke(userId: Long): Flow<List<Notificacion>> {
        return repository.getAllNotifications(userId)
    }
}
